package com.gatekeeper.mobile.vpn

import android.util.Log
import com.gatekeeper.mobile.data.db.entity.ConnectionLog
import com.gatekeeper.mobile.data.repository.TrafficRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Logs network connections from the VPN tunnel into Room.
 *
 * DEDUPLICATION: The same (packageName + remoteIp + wasBlocked) tuple is only
 * written to the DB once per DEDUP_WINDOW_MS milliseconds. This prevents the
 * Traffic Monitor from filling up with hundreds of identical rows for the same
 * ongoing connection (e.g. a streaming video to Cloudflare's 172.64.146.213).
 *
 * THREADING: log() is a non-blocking trySend() — safe to call from the packet-loop
 * thread at line-rate. A single consumer coroutine on Dispatchers.IO batches writes.
 */
@Singleton
class TrafficLogger @Inject constructor(
    private val trafficRepository: TrafficRepository,
    private val geoIpResolver: GeoIpResolver,
    private val exfiltrationDetector: ExfiltrationDetector
) {
    companion object {
        private const val TAG = "TrafficLogger"
        private const val BATCH_SIZE = 20
        // Only re-log the same connection once per 30 seconds
        private const val DEDUP_WINDOW_MS = 30_000L
    }

    private val channel = Channel<ConnectionLog>(capacity = Channel.UNLIMITED)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // key = "packageName|remoteIp|blocked" → last logged timestamp
    private val dedupCache = ConcurrentHashMap<String, Long>()

    init {
        startConsumer()
        startDedupCacheCleaner()
    }

    private fun startConsumer() {
        scope.launch {
            val batch = mutableListOf<ConnectionLog>()
            for (log in channel) {
                batch.add(log)
                // Drain extras that are already waiting
                while (true) {
                    val extra = channel.tryReceive().getOrNull() ?: break
                    batch.add(extra)
                    if (batch.size >= BATCH_SIZE) break
                }
                try {
                    trafficRepository.logConnections(batch.toList())
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to write traffic batch", e)
                }
                batch.clear()
            }
        }
    }

    /** Periodically clear the dedup cache so truly long connections eventually re-appear */
    private fun startDedupCacheCleaner() {
        scope.launch {
            while (true) {
                kotlinx.coroutines.delay(60_000L)
                val cutoff = System.currentTimeMillis() - DEDUP_WINDOW_MS * 2
                val toRemove = dedupCache.filter { it.value < cutoff }.keys
                toRemove.forEach { dedupCache.remove(it) }
            }
        }
    }

    /**
     * Log a connection event. Silently ignored if the same connection was already
     * logged within DEDUP_WINDOW_MS. Non-blocking — safe from the packet loop.
     */
    fun log(
        uid: Int? = null,
        packageName: String?,
        appName: String?,
        protocol: String,
        sourceIp: String,
        sourcePort: Int,
        destinationIp: String,
        destinationPort: Int,
        hostname: String? = null,
        country: String? = null,
        countryCode: String? = null,
        bytesSent: Long = 0,
        bytesReceived: Long = 0,
        wasBlocked: Boolean = false
    ) {
        val dedupKey = "$packageName|$destinationIp|$wasBlocked"
        val now = System.currentTimeMillis()
        val last = dedupCache[dedupKey] ?: 0L
        if (now - last < DEDUP_WINDOW_MS) return  // Duplicate — skip silently
        dedupCache[dedupKey] = now

        val (resolvedCountry, resolvedCode) = if (destinationIp.isNotBlank()) {
            geoIpResolver.resolve(destinationIp)
        } else {
            Pair(country, countryCode)
        }

        // F15: Cross-correlate traffic with sensor logs for exfiltration detection
        if (!wasBlocked && bytesSent > 0 && packageName != null) {
            exfiltrationDetector.analyzeTraffic(
                packageName = packageName,
                appName = appName ?: "Unknown App",
                remoteIp = destinationIp,
                bytesOut = bytesSent,
                countryCode = resolvedCode
            )
        }

        val attribution = NetworkAttributionMapper.resolveService(hostname, destinationPort)

        val entry = ConnectionLog(
            uid = uid,
            packageName = packageName,
            appName = appName ?: "Unknown App",
            protocol = protocol,
            sourceIp = sourceIp,
            sourcePort = sourcePort,
            destinationIp = destinationIp,
            destinationPort = destinationPort,
            hostname = hostname,
            serviceName = attribution.serviceName,
            attributionConfidence = if (packageName != null) com.gatekeeper.mobile.data.db.entity.ConfidenceLevel.HIGH else com.gatekeeper.mobile.data.db.entity.ConfidenceLevel.UNKNOWN,
            serviceConfidence = attribution.serviceConfidence,
            country = resolvedCountry,
            countryCode = resolvedCode,
            bytesSent = bytesSent,
            bytesReceived = bytesReceived,
            wasBlocked = wasBlocked
        )
        channel.trySend(entry)
    }

    /** No-op kept for API compatibility */
    fun flushLogs() {}

    fun cleanOldLogs(daysToKeep: Int = 7) {
        val cutoff = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        scope.launch {
            try { trafficRepository.clearOlderThan(cutoff) } catch (e: Exception) {}
        }
    }
}

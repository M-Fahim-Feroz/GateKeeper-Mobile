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
    private val trafficRepository: TrafficRepository
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
        packageName: String,
        appName: String,
        protocol: String,
        localIp: String,
        localPort: Int,
        remoteIp: String,
        remotePort: Int,
        remoteHostname: String? = null,
        country: String? = null,
        countryCode: String? = null,
        bytesIn: Long = 0,
        bytesOut: Long = 0,
        wasBlocked: Boolean = false
    ) {
        val dedupKey = "$packageName|$remoteIp|$wasBlocked"
        val now = System.currentTimeMillis()
        val last = dedupCache[dedupKey] ?: 0L
        if (now - last < DEDUP_WINDOW_MS) return  // Duplicate — skip silently
        dedupCache[dedupKey] = now

        val entry = ConnectionLog(
            packageName = packageName,
            appName = appName,
            protocol = protocol,
            localIp = localIp,
            localPort = localPort,
            remoteIp = remoteIp,
            remotePort = remotePort,
            remoteHostname = remoteHostname,
            country = country,
            countryCode = countryCode,
            bytesIn = bytesIn,
            bytesOut = bytesOut,
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

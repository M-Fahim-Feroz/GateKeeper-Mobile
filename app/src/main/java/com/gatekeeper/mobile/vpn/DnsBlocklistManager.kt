package com.gatekeeper.mobile.vpn

import android.util.Log
import com.gatekeeper.mobile.data.repository.DnsRepository
import com.gatekeeper.mobile.data.repository.SecurityAlertRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln

/**
 * Manages DNS blocklists — loads domains from Room DB and
 * provides them to DnsResolver for real-time filtering.
 *
 * LIVE SYNC: observeAndSync() must be called from the VpnService so that any domain
 * the user adds/removes while the VPN is running is instantly applied without restart.
 */
@Singleton
class DnsBlocklistManager @Inject constructor(
    private val dnsRepository: DnsRepository,
    private val threatFeedRepository: com.gatekeeper.mobile.data.repository.ThreatFeedRepository,
    private val securityAlertRepository: SecurityAlertRepository
) {
    // ConcurrentHashMap used as a Set for thread-safe reads from the packet loop
    private val blacklistedDomains = ConcurrentHashMap.newKeySet<String>()
    private val whitelistedDomains = ConcurrentHashMap.newKeySet<String>()
    private val scope = CoroutineScope(Dispatchers.IO)
    
    @Volatile
    var isSafeSearchEnabled: Boolean = true // Feature 4D

    // F14: DNS resolution cache — tracks recently resolved IPs to detect hardcoded-IP bypass
    val recentDnsResolutions = ConcurrentHashMap<String, Long>() // IP -> timestamp

    // F13: Per-domain query rate counter for DNS exfiltration detection
    private val dnsQueryRateCounter = ConcurrentHashMap<String, Int>() // baseDomain -> count/minute
    private val dnsQueryRateResetTime = java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis())
    private val exfiltAlertedDomains = ConcurrentHashMap.newKeySet<String>()

    companion object {
        private const val TAG = "DnsBlocklistManager"
    }

    /**
     * Suspending: loads blocklist from DB and awaits completion.
     * Call this from a coroutine before establishing the VPN tunnel so the
     * in-memory blocked-domain set is populated before any DNS queries arrive.
     */
    suspend fun awaitInitialLoad() {
        try {
            reloadAll()
        } catch (e: Exception) {
            Log.e(TAG, "awaitInitialLoad failed", e)
        }
    }

    /**
     * Fire-and-forget version (kept for compatibility).
     */
    fun loadFromDatabase() {
        scope.launch { reloadAll() }
    }

    /**
     * LIVE sync: collects Room Flow emissions so any user change is immediately
     * applied to the in-memory sets. Call this from a coroutine in VpnService.
     * This function suspends forever (until the VPN stops).
     */
    suspend fun observeAndSync() {
        // Watch only the *count* of each list, not the full list of entities.
        // This avoids materialising 175k+ DnsEntry objects on every DB change.
        // When the count changes we call the paged reloadAll() to safely rebuild in-memory sets.
        kotlinx.coroutines.coroutineScope {
            launch {
                dnsRepository.observeBlacklistCount().collect {
                    reloadAll()
                }
            }
            launch {
                dnsRepository.observeWhitelistCount().collect {
                    reloadAll()
                }
            }
        }
    }

    private suspend fun reloadAll() {
        try {
            val PAGE = 5_000

            // Load blacklist in pages to avoid OOM with large feeds
            blacklistedDomains.clear()
            var offset = 0
            while (true) {
                val page = dnsRepository.getBlacklistPage(PAGE, offset)
                if (page.isEmpty()) break
                blacklistedDomains.addAll(page)
                offset += page.size
                if (page.size < PAGE) break
            }
            // Merge threat feed domains
            val threatDomains = threatFeedRepository.getThreatDomains()
            blacklistedDomains.addAll(threatDomains)

            // Load whitelist in pages
            whitelistedDomains.clear()
            offset = 0
            while (true) {
                val page = dnsRepository.getWhitelistPage(PAGE, offset)
                if (page.isEmpty()) break
                whitelistedDomains.addAll(page)
                offset += page.size
                if (page.size < PAGE) break
            }

            Log.i(TAG, "Reload complete: ${blacklistedDomains.size} blocked, ${whitelistedDomains.size} allowed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load blocklists from DB", e)
        }
    }

    /**
     * Import a blocklist from a remote URL, processing it line-by-line to avoid OOM.
     * Domains are inserted in small batches as they are read — the full list is never
     * held in memory. A hard cap of MAX_DOMAINS_PER_FEED prevents runaway feeds from
     * consuming all available memory and crashing the app.
     */
    suspend fun importFromUrl(urlString: String, listType: String = "blacklist", sourceName: String = urlString): Int {
        return withContext(Dispatchers.IO) {
            val MAX_DOMAINS_PER_FEED = 300_000
            val BATCH_SIZE = 500

            try {
                Log.i(TAG, "Streaming blocklist from $urlString")
                val url = URL(urlString)
                val connection = url.openConnection()
                connection.connectTimeout = 15_000
                connection.readTimeout = 60_000

                // Delete old entries for this source before we start importing new ones
                dnsRepository.clearBySource(sourceName)

                val batch = mutableListOf<String>()
                var totalInserted = 0

                BufferedReader(InputStreamReader(connection.getInputStream())).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        if (totalInserted + batch.size >= MAX_DOMAINS_PER_FEED) {
                            Log.w(TAG, "Feed cap ($MAX_DOMAINS_PER_FEED) reached for $urlString — stopping early")
                            break
                        }

                        val trimmed = line!!.trim()
                        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) continue

                        // Handle "0.0.0.0 example.com" or "127.0.0.1 example.com" hosts-file format
                        val domain = if (trimmed.contains(" ")) {
                            val parts = trimmed.split(Regex("\\s+"))
                            if (parts.size >= 2 && (parts[0] == "0.0.0.0" || parts[0] == "127.0.0.1")) {
                                parts[1]
                            } else parts[0]
                        } else trimmed

                        if (domain != "localhost" && domain != "localhost.localdomain" &&
                            domain != "broadcasthost" && domain != "0.0.0.0" && domain != "127.0.0.1" &&
                            domain.contains(".")) {
                            batch.add(domain.lowercase())
                        }

                        // Flush the batch every BATCH_SIZE entries — never hold the full list
                        if (batch.size >= BATCH_SIZE) {
                            dnsRepository.addDomains(batch, listType, sourceName)
                            totalInserted += batch.size
                            batch.clear()
                        }
                    }
                }

                // Flush any remaining entries
                if (batch.isNotEmpty()) {
                    dnsRepository.addDomains(batch, listType, sourceName)
                    totalInserted += batch.size
                    batch.clear()
                }

                Log.i(TAG, "Imported $totalInserted domains from $urlString")

                // Reload in-memory set once after the full import is done
                reloadAll()
                totalInserted
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import blocklist from $urlString", e)
                throw e
            }
        }
    }

    fun getBlacklist(): Set<String> = blacklistedDomains.toSet()
    fun getWhitelist(): Set<String> = whitelistedDomains.toSet()

    fun addToBlacklist(domain: String) { blacklistedDomains.add(domain.lowercase()) }
    fun addToWhitelist(domain: String) { whitelistedDomains.add(domain.lowercase()) }
    fun removeFromBlacklist(domain: String) { blacklistedDomains.remove(domain) }
    fun removeFromWhitelist(domain: String) { whitelistedDomains.remove(domain) }

    /**
     * Checks if a domain is blocked. Called on the VPN packet loop thread — must be fast.
     * Uses exact match + suffix match (e.g. blocking "facebook.com" also blocks "api.facebook.com").
     */
    fun isDomainBlocked(domain: String): Boolean {
        val lower = domain.lowercase()

        // Whitelist takes priority
        if (whitelistedDomains.any { allowed -> lower == allowed || lower.endsWith(".$allowed") }) {
            return false
        }

        // Check blacklist
        return blacklistedDomains.any { blocked -> lower == blocked || lower.endsWith(".$blocked") }
    }

    /**
     * F13: DNS Exfiltration Detection — call this for every resolved domain.
     * Checks for long subdomains (base64/hex data) and high entropy.
     * Non-blocking: runs security check in background coroutine.
     */
    fun checkDnsExfiltration(domain: String, packageName: String, appName: String) {
        if (domain.count { it == '.' } < 2) return // Skip simple domains
        
        val subdomain = domain.substringBefore(".")
        val baseDomain = domain.substringAfter(".")
        
        // Skip if already alerted for this base domain
        if (exfiltAlertedDomains.contains(baseDomain)) return
        
        val reasons = mutableListOf<String>()
        
        // Check 1: Abnormally long subdomain (> 40 chars = likely encoded data)
        if (subdomain.length > 40) {
            reasons.add("Suspicious long subdomain (${subdomain.length} chars) — may encode stolen data")
        }
        
        // Check 2: High Shannon entropy (> 3.5 = likely base64 or hex encoded)
        val entropy = shannonEntropy(subdomain)
        if (entropy > 3.5 && subdomain.length > 10) {
            reasons.add("High entropy DNS query (${String.format("%.2f", entropy)}) — random-looking subdomain")
        }
        
        // Check 3: Rapid query rate to same base domain (> 20/minute = tunneling)
        val now = System.currentTimeMillis()
        if (now - dnsQueryRateResetTime.get() > 60_000) {
            dnsQueryRateCounter.clear()
            dnsQueryRateResetTime.set(now)
        }
        val count = dnsQueryRateCounter.merge(baseDomain, 1, Int::plus) ?: 1
        if (count > 20) {
            reasons.add("Rapid DNS queries ($count/min) to $baseDomain — possible DNS tunnel")
        }
        
        if (reasons.isNotEmpty()) {
            exfiltAlertedDomains.add(baseDomain)
            scope.launch {
                securityAlertRepository.addAlert(
                    type = "DNS_EXFILTRATION",
                    severity = "HIGH",
                    title = "Suspicious DNS Query from $appName",
                    description = reasons.joinToString(". ") + ". Domain: $domain",
                    packageName = packageName
                )
            }
            Log.w(TAG, "DNS exfiltration detected from $packageName: $domain — ${reasons.joinToString(";")}")            
        }
    }

    /**
     * F14: Register a successfully resolved domain's IPs in the DNS cache.
     * This is used by the VPN service to detect hardcoded-IP bypass attempts.
     */
    fun registerResolvedIps(ips: List<String>) {
        val now = System.currentTimeMillis()
        ips.forEach { ip -> recentDnsResolutions[ip] = now }
        // Clean old entries (older than 10 minutes)
        val cutoff = now - 10 * 60 * 1000L
        recentDnsResolutions.entries.removeIf { it.value < cutoff }
    }

    private fun shannonEntropy(s: String): Double {
        if (s.isEmpty()) return 0.0
        return -s.groupBy { it }.values
            .map { it.size.toDouble() / s.length }
            .sumOf { p -> if (p > 0) p * ln(p) / ln(2.0) else 0.0 }
    }
}

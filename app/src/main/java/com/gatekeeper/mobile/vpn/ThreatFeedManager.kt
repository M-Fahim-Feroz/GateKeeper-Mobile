package com.gatekeeper.mobile.vpn

import android.util.Log
import com.gatekeeper.mobile.data.db.entity.ThreatFeedEntry
import com.gatekeeper.mobile.data.repository.ThreatFeedRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThreatFeedManager @Inject constructor(
    private val repository: ThreatFeedRepository
) {
    companion object {
        private const val TAG = "ThreatFeedManager"
    }

    /**
     * Imports a threat feed list from a URL and parses it as IPs or Domains.
     */
    suspend fun importFromUrl(url: String, name: String, type: String, threatType: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Downloading feed from $url")
            // Purge existing entries for this feed first to prevent DB bloat on re-import
            repository.removeFeed(url)
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 15000

            val entries = mutableListOf<ThreatFeedEntry>()
            var count = 0

            connection.inputStream.bufferedReader().useLines { lines ->
                for (line in lines) {
                    val cleanLine = line.trim()
                    
                    // Skip comments or empty lines
                    if (cleanLine.isEmpty() || cleanLine.startsWith("#") || cleanLine.startsWith("//")) continue

                    // Take string up to first space or comment
                    val indicator = cleanLine.substringBefore(" ").substringBefore("#").trim()

                    if (indicator.isNotEmpty() && indicator != "localhost") {
                        entries.add(
                            ThreatFeedEntry(
                                indicator = indicator,
                                indicatorType = type,
                                feedSource = url,
                                feedName = name,
                                threatType = threatType
                            )
                        )
                        count++
                        
                        // Batch insert to avoid overwhelming memory
                        if (entries.size >= 5000) {
                            repository.importFeed(entries)
                            entries.clear()
                        }
                    }
                }
            }

            // Insert remaining
            if (entries.isNotEmpty()) {
                repository.importFeed(entries)
            }

            Log.i(TAG, "Successfully imported \$count threats from \$name")
            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import feed from \$url", e)
            Result.failure(e)
        }
    }
}

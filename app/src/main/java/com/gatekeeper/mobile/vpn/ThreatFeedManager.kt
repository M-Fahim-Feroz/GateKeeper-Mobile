package com.gatekeeper.mobile.vpn

import android.util.Log
import com.gatekeeper.mobile.data.db.entity.ThreatFeedEntry
import com.gatekeeper.mobile.data.repository.ThreatFeedRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import java.io.IOException
import java.io.InputStream
import java.io.Reader
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
        const val MAX_BYTES_LIMIT = 50L * 1024 * 1024 // 50 MB
        const val MAX_LINE_LENGTH = 1024
        const val MAX_ENTRIES_LIMIT = 300_000
        const val BATCH_SIZE = 5000
    }

    private class BoundedInputStream(private val inStream: InputStream, private val maxBytes: Long) : InputStream() {
        private var bytesRead = 0L
        override fun read(): Int {
            if (bytesRead >= maxBytes) throw IOException("Max downloaded-byte limit exceeded")
            val b = inStream.read()
            if (b != -1) bytesRead++
            return b
        }
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (bytesRead >= maxBytes) throw IOException("Max downloaded-byte limit exceeded")
            val maxRead = Math.min(len.toLong(), maxBytes - bytesRead).toInt()
            val read = inStream.read(b, off, maxRead)
            if (read != -1) bytesRead += read
            return read
        }
        override fun close() {
            inStream.close()
        }
    }

    private suspend fun readBoundedLine(reader: Reader, maxLen: Int): String? {
        val sb = StringBuilder()
        while (true) {
            yield() // Check cancellation
            val c = reader.read()
            if (c == -1) {
                return if (sb.isEmpty()) null else sb.toString()
            }
            val ch = c.toChar()
            if (ch == '\n') {
                return sb.toString()
            }
            if (ch != '\r') {
                sb.append(ch)
                if (sb.length > maxLen) {
                    throw IOException("Max line-length limit exceeded")
                }
            }
        }
    }

    /**
     * Imports a threat feed list from a URL and parses it as IPs or Domains.
     */
    suspend fun importFromUrl(url: String, name: String, type: String, threatType: String): Result<Int> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            Log.i(TAG, "Downloading feed from $url")
            repository.removeFeed(url)
            
            connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 15000

            val entries = mutableListOf<ThreatFeedEntry>()
            var count = 0

            val boundedStream = BoundedInputStream(connection.inputStream, MAX_BYTES_LIMIT)
            boundedStream.bufferedReader().use { reader ->
                while (isActive) {
                    val line = readBoundedLine(reader, MAX_LINE_LENGTH) ?: break
                    val cleanLine = line.trim()
                    
                    if (cleanLine.isEmpty() || cleanLine.startsWith("#") || cleanLine.startsWith("//")) continue

                    val indicator = cleanLine.substringBefore(" ").substringBefore("#").trim()

                    if (indicator.isNotEmpty() && indicator != "localhost") {
                        if (count >= MAX_ENTRIES_LIMIT) {
                            throw IOException("Max imported-entry limit exceeded")
                        }
                        
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
                        
                        if (entries.size >= BATCH_SIZE) {
                            repository.importFeed(entries)
                            entries.clear()
                        }
                    }
                }
            }

            if (entries.isNotEmpty()) {
                repository.importFeed(entries)
            }

            Log.i(TAG, "Successfully imported $count threats from $name")
            Result.success(count)
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.w(TAG, "Import cancelled for $url, cleaning up partial data")
            repository.removeFeed(url)
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Network or parsing error importing feed from $url", e)
            repository.removeFeed(url)
            Result.failure(e)
        } catch (e: android.database.sqlite.SQLiteException) {
            Log.e(TAG, "Database error importing feed from $url", e)
            repository.removeFeed(url)
            Result.failure(e)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception importing feed from $url", e)
            repository.removeFeed(url)
            Result.failure(e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid URL or argument importing feed from $url", e)
            repository.removeFeed(url)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }
}

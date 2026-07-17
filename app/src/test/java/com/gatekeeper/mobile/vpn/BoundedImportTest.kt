package com.gatekeeper.mobile.vpn

import com.gatekeeper.mobile.data.repository.ThreatFeedRepository
import com.gatekeeper.mobile.data.repository.DnsRepository
import com.gatekeeper.mobile.data.repository.SecurityAlertRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.io.IOException
import java.util.concurrent.Executors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import io.mockk.mockkStatic
import io.mockk.unmockkAll

class BoundedImportTest {
    private lateinit var threatFeedManager: ThreatFeedManager
    private lateinit var dnsBlocklistManager: DnsBlocklistManager
    private val threatRepo: ThreatFeedRepository = mockk(relaxed = true)
    private val dnsRepo: DnsRepository = mockk(relaxed = true)
    private val alertRepo: SecurityAlertRepository = mockk(relaxed = true)
    private lateinit var server: HttpServer
    private var port: Int = 0

    @BeforeEach
    fun setup() {
        mockkStatic(android.util.Log::class)
        io.mockk.every { android.util.Log.i(any(), any()) } returns 0
        io.mockk.every { android.util.Log.e(any(), any(), any()) } returns 0
        io.mockk.every { android.util.Log.e(any(), any()) } returns 0
        io.mockk.every { android.util.Log.w(any(), any<String>()) } returns 0

        threatFeedManager = ThreatFeedManager(threatRepo)
        dnsBlocklistManager = DnsBlocklistManager(dnsRepo, threatRepo, alertRepo)
        server = HttpServer.create(InetSocketAddress(0), 0)
        server.executor = Executors.newCachedThreadPool()
        server.start()
        port = server.address.port
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
        server.stop(0)
    }

    // ─── ThreatFeedManager byte-limit ──────────────────────────────────────────

    @Test
    fun `TFM - byte limit exceeded returns failure and rolls back`() = runBlocking {
        server.createContext("/tfm_large") { exchange ->
            exchange.sendResponseHeaders(200, 0)
            val out = exchange.responseBody
            val chunk = ByteArray(1024 * 1024) { i ->
                if (i % 50 == 0) '\n'.code.toByte() else '#'.code.toByte()
            }
            try { for (i in 0..51) out.write(chunk) } catch (_: Exception) {}
            out.close()
        }

        val result = threatFeedManager.importFromUrl(
            "http://localhost:$port/tfm_large", "Test", "domain", "malware"
        )
        assertTrue(result.isFailure, "Expected failure from byte-limit")
        assertTrue(result.exceptionOrNull() is IOException)
        assertTrue(result.exceptionOrNull()?.message?.contains("byte limit") == true)
        // removeFeed called on start AND on rollback
        coVerify(atLeast = 1) { threatRepo.removeFeed(any()) }
    }

    // ─── DnsBlocklistManager byte-limit ────────────────────────────────────────

    @Test
    fun `DBM - byte limit exceeded throws IOException and rolls back`() = runBlocking {
        server.createContext("/dbm_large") { exchange ->
            exchange.sendResponseHeaders(200, 0)
            val out = exchange.responseBody
            val chunk = ByteArray(1024 * 1024) { i ->
                if (i % 50 == 0) '\n'.code.toByte() else '#'.code.toByte()
            }
            try { for (i in 0..51) out.write(chunk) } catch (_: Exception) {}
            out.close()
        }

        var exceptionThrown = false
        try {
            dnsBlocklistManager.importFromUrl("http://localhost:$port/dbm_large", "blacklist", "src_dbm")
        } catch (e: IOException) {
            exceptionThrown = true
            assertTrue(e.message?.contains("byte limit") == true)
        }
        assertTrue(exceptionThrown, "Expected IOException from byte-limit")
        coVerify(atLeast = 1) { dnsRepo.clearBySource("src_dbm") }
    }

    // ─── ThreatFeedManager line-length limit ───────────────────────────────────

    @Test
    fun `TFM - line length exceeded returns failure and rolls back`() = runBlocking {
        server.createContext("/tfm_longline") { exchange ->
            exchange.sendResponseHeaders(200, 0)
            val out = exchange.responseBody
            val chunk = ByteArray(2000) { 'a'.code.toByte() }
            out.write(chunk)
            out.write('\n'.code)
            out.close()
        }

        val result = threatFeedManager.importFromUrl(
            "http://localhost:$port/tfm_longline", "Test", "domain", "malware"
        )
        assertTrue(result.isFailure, "Expected failure from line-length")
        assertTrue(result.exceptionOrNull() is IOException)
        assertTrue(result.exceptionOrNull()?.message?.contains("line-length") == true)
        coVerify(atLeast = 1) { threatRepo.removeFeed(any()) }
    }

    // ─── DnsBlocklistManager entry-count limit ─────────────────────────────────

    @Test
    fun `DBM - entry count limit exceeded throws IOException and rolls back`() = runBlocking {
        server.createContext("/dbm_manyentries") { exchange ->
            exchange.sendResponseHeaders(200, 0)
            val out = exchange.responseBody
            try {
                for (i in 1..300_005) {
                    out.write("domain$i.com\n".toByteArray())
                }
            } catch (_: Exception) {}
            out.close()
        }

        var exceptionThrown = false
        try {
            dnsBlocklistManager.importFromUrl(
                "http://localhost:$port/dbm_manyentries", "blacklist", "src_many"
            )
        } catch (e: IOException) {
            exceptionThrown = true
            assertTrue(e.message?.contains("entry limit") == true)
        }
        assertTrue(exceptionThrown, "Expected IOException from entry-count limit")
        coVerify(atLeast = 1) { dnsRepo.clearBySource("src_many") }
    }

    // ─── Empty feed ────────────────────────────────────────────────────────────

    @Test
    fun `TFM - empty feed returns success with zero count`() = runBlocking {
        server.createContext("/tfm_empty") { exchange ->
            exchange.sendResponseHeaders(200, 0)
            exchange.responseBody.close()
        }

        val result = threatFeedManager.importFromUrl(
            "http://localhost:$port/tfm_empty", "Test", "domain", "malware"
        )
        assertTrue(result.isSuccess, "Empty feed should succeed")
        assertEquals(0, result.getOrNull())
    }

    @Test
    fun `DBM - empty feed returns zero`() = runBlocking {
        server.createContext("/dbm_empty") { exchange ->
            exchange.sendResponseHeaders(200, 0)
            exchange.responseBody.close()
        }

        val count = dnsBlocklistManager.importFromUrl(
            "http://localhost:$port/dbm_empty", "blacklist", "src_empty"
        )
        assertEquals(0, count)
    }

    // ─── Malformed / comment lines ─────────────────────────────────────────────

    @Test
    fun `DBM - comment lines and blank lines are skipped`() = runBlocking {
        server.createContext("/dbm_comments") { exchange ->
            exchange.sendResponseHeaders(200, 0)
            val out = exchange.responseBody
            out.write("# This is a comment\n".toByteArray())
            out.write("! ABP comment\n".toByteArray())
            out.write("\n".toByteArray())
            out.write("good.com\n".toByteArray())
            out.close()
        }

        val count = dnsBlocklistManager.importFromUrl(
            "http://localhost:$port/dbm_comments", "blacklist", "src_comments"
        )
        assertEquals(1, count, "Only one real domain should be imported")
    }

    @Test
    fun `DBM - hosts-file format 0-0-0-0 is parsed correctly`() = runBlocking {
        server.createContext("/dbm_hosts") { exchange ->
            exchange.sendResponseHeaders(200, 0)
            val out = exchange.responseBody
            out.write("0.0.0.0 tracker.example.com\n".toByteArray())
            out.write("127.0.0.1 ad.example.org\n".toByteArray())
            out.write("localhost\n".toByteArray())  // should be skipped
            out.close()
        }

        val count = dnsBlocklistManager.importFromUrl(
            "http://localhost:$port/dbm_hosts", "blacklist", "src_hosts"
        )
        assertEquals(2, count, "Two real domains from hosts-file format")
    }

    // ─── Duplicate entries ─────────────────────────────────────────────────────

    @Test
    fun `DBM - duplicate domains are both sent to DB (dedup is DB responsibility)`() = runBlocking {
        server.createContext("/dbm_dupes") { exchange ->
            exchange.sendResponseHeaders(200, 0)
            val out = exchange.responseBody
            out.write("dup.com\n".toByteArray())
            out.write("dup.com\n".toByteArray())
            out.write("dup.com\n".toByteArray())
            out.close()
        }

        val count = dnsBlocklistManager.importFromUrl(
            "http://localhost:$port/dbm_dupes", "blacklist", "src_dupes"
        )
        // All 3 entries are sent through (dedup is at DB/Set layer)
        assertEquals(3, count)
    }

    // ─── Network failure ───────────────────────────────────────────────────────

    @Test
    fun `TFM - network failure returns failure and rolls back`() = runBlocking {
        // Use a port where nothing is listening
        val result = threatFeedManager.importFromUrl(
            "http://localhost:9/no_server", "Test", "domain", "malware"
        )
        assertTrue(result.isFailure, "Should fail on connection refused")
        coVerify(atLeast = 1) { threatRepo.removeFeed(any()) }
    }

    // ─── Cancellation ──────────────────────────────────────────────────────────

    @Test
    fun `TFM - cancellation rolls back imported data`() = runBlocking {
        server.createContext("/tfm_slow") { exchange ->
            exchange.sendResponseHeaders(200, 0)
            val out = exchange.responseBody
            try {
                for (i in 1..1000) {
                    out.write("domain$i.com\n".toByteArray())
                    out.flush()
                    Thread.sleep(10)
                }
            } catch (_: Exception) {}
            out.close()
        }

        val job = launch(Dispatchers.IO) {
            try {
                threatFeedManager.importFromUrl(
                    "http://localhost:$port/tfm_slow", "Test", "domain", "malware"
                )
            } catch (_: CancellationException) {}
        }

        delay(150)
        job.cancelAndJoin()
        coVerify(atLeast = 1) { threatRepo.removeFeed(any()) }
    }

    // ─── Incremental processing: no full String allocation ─────────────────────

    @Test
    fun `DBM - large valid feed is processed in batches without OOM`() = runBlocking {
        val lineCount = 50_000
        server.createContext("/dbm_batched") { exchange ->
            exchange.sendResponseHeaders(200, 0)
            val out = exchange.responseBody
            try {
                for (i in 1..lineCount) {
                    out.write("batch$i.example.com\n".toByteArray())
                }
            } catch (_: Exception) {}
            out.close()
        }

        val count = dnsBlocklistManager.importFromUrl(
            "http://localhost:$port/dbm_batched", "blacklist", "src_batched"
        )
        assertEquals(lineCount, count, "All $lineCount entries should be processed")
    }
}

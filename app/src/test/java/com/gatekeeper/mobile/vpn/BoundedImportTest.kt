package com.gatekeeper.mobile.vpn

import com.gatekeeper.mobile.data.repository.ThreatFeedRepository
import com.gatekeeper.mobile.data.repository.DnsRepository
import com.gatekeeper.mobile.data.repository.SecurityAlertRepository
import io.mockk.mockk
import io.mockk.verify
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
        threatFeedManager = ThreatFeedManager(threatRepo)
        dnsBlocklistManager = DnsBlocklistManager(dnsRepo, threatRepo, alertRepo)
        server = HttpServer.create(InetSocketAddress(0), 0)
        server.executor = Executors.newCachedThreadPool()
        server.start()
        port = server.address.port
    }

    @AfterEach
    fun teardown() {
        server.stop(0)
    }

    @Test
    fun `test byte limit exceeded throws exception and rolls back (ThreatFeedManager)`() = runBlocking {
        server.createContext("/large1") { exchange ->
            exchange.sendResponseHeaders(200, 0)
            val out = exchange.responseBody
            val chunk = ByteArray(1024 * 1024) { 'a'.code.toByte() }
            try {
                for (i in 0..51) out.write(chunk)
            } catch (e: Exception) {}
            out.close()
        }

        val result = threatFeedManager.importFromUrl("http://localhost:$port/large1", "Test", "domain", "malware")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals("Max downloaded-byte limit exceeded", result.exceptionOrNull()?.message)
        verify(exactly = 2) { threatRepo.removeFeed(any()) }
    }

    @Test
    fun `test byte limit exceeded throws exception and rolls back (DnsBlocklistManager)`() = runBlocking {
        server.createContext("/large2") { exchange ->
            exchange.sendResponseHeaders(200, 0)
            val out = exchange.responseBody
            val chunk = ByteArray(1024 * 1024) { 'a'.code.toByte() }
            try {
                for (i in 0..51) out.write(chunk)
            } catch (e: Exception) {}
            out.close()
        }

        var exceptionThrown = false
        try {
            dnsBlocklistManager.importFromUrl("http://localhost:$port/large2", "blacklist", "source")
        } catch (e: IOException) {
            exceptionThrown = true
            assertEquals("Max downloaded-byte limit exceeded", e.message)
        }
        assertTrue(exceptionThrown)
        verify(exactly = 2) { dnsRepo.clearBySource("source") }
    }

    @Test
    fun `test line limit exceeded throws exception and rolls back (ThreatFeedManager)`() = runBlocking {
        server.createContext("/longline1") { exchange ->
            exchange.sendResponseHeaders(200, 0)
            val out = exchange.responseBody
            val chunk = ByteArray(2000) { 'a'.code.toByte() }
            out.write(chunk)
            out.write('\n'.code)
            out.close()
        }

        val result = threatFeedManager.importFromUrl("http://localhost:$port/longline1", "Test", "domain", "malware")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals("Max line-length limit exceeded", result.exceptionOrNull()?.message)
        verify(exactly = 2) { threatRepo.removeFeed(any()) }
    }

    @Test
    fun `test entry limit exceeded throws exception and rolls back (DnsBlocklistManager)`() = runBlocking {
        server.createContext("/manyentries2") { exchange ->
            exchange.sendResponseHeaders(200, 0)
            val out = exchange.responseBody
            try {
                for (i in 1..300_005) {
                    out.write("domain$i.com\n".toByteArray())
                }
            } catch (e: Exception) {}
            out.close()
        }

        var exceptionThrown = false
        try {
            dnsBlocklistManager.importFromUrl("http://localhost:$port/manyentries2", "blacklist", "source")
        } catch (e: IOException) {
            exceptionThrown = true
            assertEquals("Max imported-entry limit exceeded", e.message)
        }
        assertTrue(exceptionThrown)
        verify(exactly = 2) { dnsRepo.clearBySource("source") }
    }

    @Test
    fun `test partial failure due to malformed network rolls back (ThreatFeedManager)`() = runBlocking {
        server.createContext("/malformed1") { exchange ->
            exchange.sendResponseHeaders(200, 0)
            val out = exchange.responseBody
            out.write("good.com\n".toByteArray())
            out.flush()
            exchange.close()
        }

        val result = threatFeedManager.importFromUrl("http://localhost:$port/malformed1", "Test", "domain", "malware")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
        verify(exactly = 2) { threatRepo.removeFeed(any()) }
    }

    @Test
    fun `test partial failure due to malformed network rolls back (DnsBlocklistManager)`() = runBlocking {
        server.createContext("/malformed2") { exchange ->
            exchange.sendResponseHeaders(200, 0)
            val out = exchange.responseBody
            out.write("good.com\n".toByteArray())
            out.flush()
            exchange.close()
        }

        var exceptionThrown = false
        try {
            dnsBlocklistManager.importFromUrl("http://localhost:$port/malformed2", "blacklist", "source")
        } catch (e: IOException) {
            exceptionThrown = true
        }
        assertTrue(exceptionThrown)
        verify(exactly = 2) { dnsRepo.clearBySource("source") }
    }

    @Test
    fun `test cancellation rolls back (ThreatFeedManager)`() = runBlocking {
        server.createContext("/slow1") { exchange ->
            exchange.sendResponseHeaders(200, 0)
            val out = exchange.responseBody
            try {
                for (i in 1..100) {
                    out.write("domain$i.com\n".toByteArray())
                    out.flush()
                    Thread.sleep(100)
                }
            } catch (e: Exception) {}
            out.close()
        }

        val job = launch(Dispatchers.IO) {
            try {
                threatFeedManager.importFromUrl("http://localhost:$port/slow1", "Test", "domain", "malware")
            } catch (e: CancellationException) {}
        }
        
        delay(300)
        job.cancelAndJoin()
        verify(exactly = 2) { threatRepo.removeFeed(any()) }
    }
}

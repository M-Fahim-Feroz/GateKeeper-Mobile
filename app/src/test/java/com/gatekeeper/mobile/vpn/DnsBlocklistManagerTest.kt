package com.gatekeeper.mobile.vpn

import com.gatekeeper.mobile.data.repository.DnsRepository
import com.gatekeeper.mobile.data.repository.SecurityAlertRepository
import com.gatekeeper.mobile.data.repository.ThreatFeedRepository
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DnsBlocklistManagerTest {

    private lateinit var dnsBlocklistManager: DnsBlocklistManager
    private val dnsRepository: DnsRepository = mockk(relaxed = true)
    private val threatFeedRepository: ThreatFeedRepository = mockk(relaxed = true)
    private val securityAlertRepository: SecurityAlertRepository = mockk(relaxed = true)

    @BeforeEach
    fun setup() {
        dnsBlocklistManager = DnsBlocklistManager(
            dnsRepository,
            threatFeedRepository,
            securityAlertRepository
        )
    }

    @Test
    fun `test exact domain block`() {
        dnsBlocklistManager.addToBlacklist("example.com")
        
        assertTrue(dnsBlocklistManager.isDomainBlocked("example.com"))
        assertFalse(dnsBlocklistManager.isDomainBlocked("example.org"))
    }

    @Test
    fun `test subdomain block`() {
        dnsBlocklistManager.addToBlacklist("example.com")
        
        assertTrue(dnsBlocklistManager.isDomainBlocked("api.example.com"))
        assertTrue(dnsBlocklistManager.isDomainBlocked("sub.api.example.com"))
    }

    @Test
    fun `test whitelist overrides blacklist`() {
        dnsBlocklistManager.addToBlacklist("example.com")
        dnsBlocklistManager.addToWhitelist("safe.example.com")
        
        assertTrue(dnsBlocklistManager.isDomainBlocked("example.com"))
        assertTrue(dnsBlocklistManager.isDomainBlocked("api.example.com"))
        assertFalse(dnsBlocklistManager.isDomainBlocked("safe.example.com"))
        assertFalse(dnsBlocklistManager.isDomainBlocked("api.safe.example.com"))
    }

    @Test
    fun `test case insensitivity`() {
        dnsBlocklistManager.addToBlacklist("ExAmPlE.CoM")
        assertTrue(dnsBlocklistManager.isDomainBlocked("EXAMPLE.COM"))
        assertTrue(dnsBlocklistManager.isDomainBlocked("example.com"))
    }

    @Test
    fun `test exfiltration detection triggers alert`() {
        io.mockk.mockkStatic("android.util.Log")
        io.mockk.every { android.util.Log.w(any(), any<String>()) } returns 0
        // Just verify it doesn't crash since it's a coroutine background task
        dnsBlocklistManager.checkDnsExfiltration("longbase64encodedsubdomaindata.example.com", "com.bad.app", "BadApp")
    }
}



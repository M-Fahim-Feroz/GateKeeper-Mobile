package com.gatekeeper.mobile.vpn

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class DnsResolverTest {

    @Test
    fun `test SafeSearch rewrites Google to safe ip`() {
        assertTrue(DnsResolver.SAFESEARCH_REWRITES.containsKey("google.com"))
        
        val safeIp = DnsResolver.SAFESEARCH_REWRITES["google.com"]!!
        assertEquals(216.toByte(), safeIp[0])
        assertEquals(239.toByte(), safeIp[1])
        assertEquals(38.toByte(), safeIp[2])
        assertEquals(120.toByte(), safeIp[3])
    }
}



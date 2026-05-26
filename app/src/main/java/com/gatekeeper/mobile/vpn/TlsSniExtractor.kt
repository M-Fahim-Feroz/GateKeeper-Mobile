package com.gatekeeper.mobile.vpn

import java.nio.ByteBuffer

object TlsSniExtractor {

    /**
     * Extracts the Server Name Indication (SNI) from a TLS ClientHello packet.
     * Returns the SNI hostname, or null if it's not a valid ClientHello or doesn't contain SNI.
     */
    fun extract(payload: ByteArray): String? {
        if (payload.size < 43) return null

        val buffer = ByteBuffer.wrap(payload)
        
        try {
            // TLS Record Layer
            val contentType = buffer.get().toInt() and 0xFF
            if (contentType != 0x16) return null // 0x16 = Handshake
            
            val versionMajor = buffer.get().toInt() and 0xFF
            val versionMinor = buffer.get().toInt() and 0xFF
            if (versionMajor != 0x03) return null // TLS 1.x
            
            val recordLength = buffer.short.toInt() and 0xFFFF
            if (buffer.remaining() < recordLength) return null

            // Handshake Protocol
            val handshakeType = buffer.get().toInt() and 0xFF
            if (handshakeType != 0x01) return null // 0x01 = ClientHello
            
            val handshakeLength = ((buffer.get().toInt() and 0xFF) shl 16) or 
                                  ((buffer.get().toInt() and 0xFF) shl 8) or 
                                  (buffer.get().toInt() and 0xFF)
            
            buffer.short // Client version
            buffer.position(buffer.position() + 32) // Client random
            
            val sessionIdLength = buffer.get().toInt() and 0xFF
            buffer.position(buffer.position() + sessionIdLength)
            
            val cipherSuitesLength = buffer.short.toInt() and 0xFFFF
            buffer.position(buffer.position() + cipherSuitesLength)
            
            val compressionMethodsLength = buffer.get().toInt() and 0xFF
            buffer.position(buffer.position() + compressionMethodsLength)
            
            if (buffer.remaining() < 2) return null
            val extensionsLength = buffer.short.toInt() and 0xFFFF
            
            val extensionsEnd = buffer.position() + extensionsLength
            while (buffer.position() < extensionsEnd && buffer.remaining() >= 4) {
                val extensionType = buffer.short.toInt() and 0xFFFF
                val extensionLen = buffer.short.toInt() and 0xFFFF
                
                if (extensionType == 0x0000) { // Server Name (SNI)
                    if (buffer.remaining() < extensionLen) return null
                    
                    val sniListLen = buffer.short.toInt() and 0xFFFF
                    val nameType = buffer.get().toInt() and 0xFF
                    
                    if (nameType == 0x00) { // 0x00 = host_name
                        val nameLen = buffer.short.toInt() and 0xFFFF
                        val nameBytes = ByteArray(nameLen)
                        buffer.get(nameBytes)
                        return String(nameBytes)
                    }
                } else {
                    buffer.position(buffer.position() + extensionLen)
                }
            }
        } catch (e: Exception) {
            // Malformed packet
        }
        
        return null
    }
}

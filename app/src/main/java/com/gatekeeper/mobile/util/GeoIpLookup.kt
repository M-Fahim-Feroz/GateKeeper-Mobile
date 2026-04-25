package com.gatekeeper.mobile.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility to resolve IP addresses to Country names/codes.
 * For the FYP, this is a skeleton that ideally loads a MaxMind GeoLite2-Country.mmdb
 * from the app's assets folder. If the database is missing, it falls back to a placeholder.
 */
@Singleton
class GeoIpLookup @Inject constructor() {
    
    // In a real application, we would initialize com.maxmind.geoip2.DatabaseReader here.
    private var isDbLoaded = false

    fun init(context: Context) {
        try {
            val dbFile = File(context.cacheDir, "GeoLite2-Country.mmdb")
            if (!dbFile.exists()) {
                // Try to copy from assets if it exists
                val assetManager = context.assets
                try {
                    val inputStream: InputStream = assetManager.open("GeoLite2-Country.mmdb")
                    val outputStream = FileOutputStream(dbFile)
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()
                    isDbLoaded = true
                    Log.i(TAG, "GeoIP database loaded successfully.")
                } catch (e: Exception) {
                    Log.w(TAG, "GeoLite2-Country.mmdb not found in assets. GeoIP will be disabled.")
                }
            } else {
                isDbLoaded = true
            }
            
            // If we had the dependency: reader = new DatabaseReader.Builder(dbFile).build();
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize GeoIpLookup", e)
        }
    }

    /**
     * Look up the country name for a given IP address.
     */
    fun getCountry(ipAddress: String): String? {
        if (!isDbLoaded) return null
        
        // Return a mock value since we don't have the 40MB database file in the repository.
        // In reality, this would be: val response = reader.country(InetAddress.getByName(ipAddress))
        // return response.country.name
        
        return if (ipAddress.startsWith("10.") || ipAddress.startsWith("192.168.")) {
            "Local Network"
        } else {
            "United States" // Placeholder for demonstration
        }
    }

    /**
     * Look up the 2-letter ISO country code for a given IP address.
     */
    fun getCountryCode(ipAddress: String): String? {
        if (!isDbLoaded) return null
        
        return if (ipAddress.startsWith("10.") || ipAddress.startsWith("192.168.")) {
            "LAN"
        } else {
            "US" // Placeholder for demonstration
        }
    }

    companion object {
        private const val TAG = "GeoIpLookup"
    }
}

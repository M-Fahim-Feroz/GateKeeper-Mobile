package com.gatekeeper.mobile.vpn

import android.content.Context
import com.maxmind.geoip2.DatabaseReader
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeoIpResolver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Lazy-load the DB once on first use. ~6MB in RAM but loaded once per app lifecycle.
    private val reader: DatabaseReader? by lazy {
        try {
            DatabaseReader.Builder(context.assets.open("GeoLite2-Country.mmdb")).build()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Returns Pair(countryName, isoCode) e.g. ("United States", "US")
     * Returns ("Unknown", "--") if lookup fails (private IP, unresolved, etc.)
     */
    fun resolve(ip: String): Pair<String, String> {
        return try {
            val address = InetAddress.getByName(ip)
            // Skip private/local IPs
            if (address.isSiteLocalAddress || address.isLoopbackAddress) {
                return Pair("Local Network", "LO")
            }
            if (reader == null) {
                return Pair("Unknown", "--")
            }
            val response = reader!!.country(address)
            val name = response.country.name ?: "Unknown"
            val code = response.country.isoCode ?: "--"
            Pair(name, code)
        } catch (e: Exception) {
            Pair("Unknown", "--")
        }
    }
}

package com.gatekeeper.mobile.data.repository

import com.gatekeeper.mobile.data.db.dao.KnownNetworkDao
import com.gatekeeper.mobile.data.db.entity.KnownNetwork
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KnownNetworkRepository @Inject constructor(
    private val dao: KnownNetworkDao
) {
    fun observeAll(): Flow<List<KnownNetwork>> = dao.observeAll()

    suspend fun getNetwork(ssid: String, bssid: String): KnownNetwork? {
        return dao.getNetwork(ssid, bssid)
    }

    suspend fun getKnownBssidsForSsid(ssid: String): List<String> {
        return dao.getBySsid(ssid).filter { it.isTrusted }.map { it.bssid }
    }

    suspend fun addOrUpdateNetwork(ssid: String, bssid: String, securityType: String) {
        val existing = dao.getNetwork(ssid, bssid)
        if (existing != null) {
            dao.updateLastSeen(existing.id, System.currentTimeMillis())
        } else {
            dao.upsert(
                KnownNetwork(
                    ssid = ssid,
                    bssid = bssid,
                    securityType = securityType
                )
            )
        }
    }

    suspend fun trustNetwork(network: KnownNetwork) {
        dao.upsert(network.copy(isTrusted = true))
    }

    suspend fun delete(network: KnownNetwork) {
        dao.delete(network)
    }
}

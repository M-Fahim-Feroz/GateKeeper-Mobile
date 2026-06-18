package com.gatekeeper.mobile.data.repository

import com.gatekeeper.mobile.data.db.dao.ConnectionLogDao
import com.gatekeeper.mobile.data.db.dao.CountryCount
import com.gatekeeper.mobile.data.db.entity.ConnectionLog
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrafficRepository @Inject constructor(
    private val dao: ConnectionLogDao
) {
    fun observeRecent(limit: Int = 200): Flow<List<ConnectionLog>> = dao.observeRecent(limit)
    fun observeSince(from: Long): Flow<List<ConnectionLog>> = dao.observeSince(from)
    fun observeByApp(packageName: String): Flow<List<ConnectionLog>> = dao.observeByApp(packageName)
    fun observeBlocked(): Flow<List<ConnectionLog>> = dao.observeBlocked()
    fun observeTopCountries(): Flow<List<CountryCount>> = dao.observeTopCountries()
    fun observeTotalCount(): Flow<Int> = dao.observeTotalCount()
    fun observeTotalBytesIn(): Flow<Long?> = dao.observeTotalBytesIn()
    fun observeTotalBytesOut(): Flow<Long?> = dao.observeTotalBytesOut()

    suspend fun logConnection(log: ConnectionLog) = dao.insert(log)
    suspend fun logConnections(logs: List<ConnectionLog>) = dao.insertAll(logs)
    suspend fun clearOlderThan(timestamp: Long) = dao.deleteBefore(timestamp)
    suspend fun deleteAll() = dao.deleteAll()

    suspend fun insertSystemEvent(reason: String) {
        logConnection(
            ConnectionLog(
                packageName = "system",
                appName = "System",
                protocol = "SYS",
                sourceIp = "",
                sourcePort = 0,
                destinationIp = "",
                destinationPort = 0,
                isSystemEvent = true,
                systemEventReason = reason
            )
        )
    }
}

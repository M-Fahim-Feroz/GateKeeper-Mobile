package com.gatekeeper.mobile.data.repository

import com.gatekeeper.mobile.data.db.dao.SecurityAlertDao
import com.gatekeeper.mobile.data.db.entity.SecurityAlert
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityAlertRepository @Inject constructor(
    private val dao: SecurityAlertDao
) {
    fun observeAll(): Flow<List<SecurityAlert>> = dao.observeAll()
    
    fun observeUnresolved(): Flow<List<SecurityAlert>> = dao.observeUnresolved()

    suspend fun addAlert(
        type: String,
        severity: String,
        title: String,
        description: String,
        packageName: String? = null
    ) {
        val alert = SecurityAlert(
            type = type,
            severity = severity,
            title = title,
            description = description,
            packageName = packageName
        )
        dao.insert(alert)
    }

    suspend fun resolveAlert(id: Long) {
        dao.markResolved(id)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}

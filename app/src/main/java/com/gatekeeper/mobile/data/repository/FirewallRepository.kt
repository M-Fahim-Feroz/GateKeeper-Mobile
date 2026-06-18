package com.gatekeeper.mobile.data.repository

import com.gatekeeper.mobile.data.db.dao.FirewallRuleDao
import com.gatekeeper.mobile.data.db.entity.FirewallRule
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirewallRepository @Inject constructor(
    private val dao: FirewallRuleDao
) {
    fun observeAll(): Flow<List<FirewallRule>> = dao.observeAll()
    fun observeBlocked(): Flow<List<FirewallRule>> = dao.observeBlocked()
    fun observeBlockedCount(): Flow<Int> = dao.observeBlockedCount()

    suspend fun getBlockedPackages(): List<String> = dao.getBlockedPackages()
    suspend fun getAllRules(): List<FirewallRule> = dao.getAllRules()

    suspend fun toggleBlock(packageName: String, appName: String, blocked: Boolean) {
        val existing = dao.getByPackage(packageName)
        if (existing != null) {
            dao.upsert(existing.copy(isBlocked = blocked, updatedAt = System.currentTimeMillis()))
        } else {
            dao.upsert(
                FirewallRule(
                    packageName = packageName,
                    appName = appName,
                    isBlocked = blocked,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteAll() = dao.deleteAll()

    suspend fun getRule(packageName: String): FirewallRule? = dao.getByPackage(packageName)

    suspend fun upsertRule(rule: FirewallRule) = dao.upsert(rule)
}

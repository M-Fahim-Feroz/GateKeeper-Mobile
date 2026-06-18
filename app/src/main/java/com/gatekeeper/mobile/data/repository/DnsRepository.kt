package com.gatekeeper.mobile.data.repository

import com.gatekeeper.mobile.data.db.dao.DnsBlocklistDao
import com.gatekeeper.mobile.data.db.entity.DnsEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DnsRepository @Inject constructor(
    private val dao: DnsBlocklistDao
) {
    fun observeBlacklist(): Flow<List<DnsEntry>> = dao.observeByType("blacklist")
    fun observeWhitelist(): Flow<List<DnsEntry>> = dao.observeByType("whitelist")
    fun observeBlacklistCount(): Flow<Int> = dao.observeCount("blacklist")
    fun observeWhitelistCount(): Flow<Int> = dao.observeCount("whitelist")

    suspend fun getActiveBlacklist(): List<String> = dao.getActiveBlacklistDomains()
    suspend fun getActiveWhitelist(): List<String> = dao.getActiveWhitelistDomains()

    // Paged variants for in-memory reload — avoids loading 175k+ strings into one List
    suspend fun getBlacklistPage(limit: Int, offset: Int): List<String> = dao.getBlacklistPage(limit, offset)
    suspend fun getWhitelistPage(limit: Int, offset: Int): List<String> = dao.getWhitelistPage(limit, offset)
    suspend fun getBlacklistCount(): Int = dao.getBlacklistCount()
    suspend fun getWhitelistCount(): Int = dao.getWhitelistCount()

    suspend fun addDomain(domain: String, listType: String, source: String = "user") {
        dao.insert(DnsEntry(domain = domain, listType = listType, source = source))
    }

    suspend fun addDomains(domains: List<String>, listType: String, source: String) {
        val entries = domains.map { DnsEntry(domain = it, listType = listType, source = source) }
        dao.insertAll(entries)
    }

    suspend fun removeDomain(domain: String, listType: String) {
        dao.deleteByDomain(domain, listType)
    }

    suspend fun clearBySource(source: String) = dao.deleteBySource(source)
    suspend fun deleteAll() = dao.deleteAll()
}

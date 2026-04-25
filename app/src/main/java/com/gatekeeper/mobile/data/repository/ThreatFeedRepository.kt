package com.gatekeeper.mobile.data.repository

import com.gatekeeper.mobile.data.db.dao.ThreatFeedDao
import com.gatekeeper.mobile.data.db.entity.ThreatFeedEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThreatFeedRepository @Inject constructor(
    private val dao: ThreatFeedDao
) {
    fun observeAll(): Flow<List<ThreatFeedEntry>> = dao.observeAll()
    fun observeCount(): Flow<Int> = dao.observeCount()
    fun observeFeedNames(): Flow<List<String>> = dao.observeFeedNames()

    suspend fun getThreatIps(): List<String> = dao.getThreatIps()
    suspend fun getThreatDomains(): List<String> = dao.getThreatDomains()

    suspend fun importFeed(entries: List<ThreatFeedEntry>) = dao.insertAll(entries)
    suspend fun removeFeed(source: String) = dao.deleteBySource(source)
    suspend fun deleteAll() = dao.deleteAll()
}

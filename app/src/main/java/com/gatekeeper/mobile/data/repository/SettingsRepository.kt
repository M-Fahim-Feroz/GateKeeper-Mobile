package com.gatekeeper.mobile.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gatekeeper_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val CAPTURE_PCAP = booleanPreferencesKey("capture_pcap")
        val BACKEND_IP = androidx.datastore.preferences.core.stringPreferencesKey("backend_ip")
    }

    val capturePcapFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[CAPTURE_PCAP] ?: false
    }

    val backendIpFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[BACKEND_IP]
    }

    suspend fun setCapturePcap(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[CAPTURE_PCAP] = enabled
        }
    }

    suspend fun setBackendIp(ip: String) {
        dataStore.edit { preferences ->
            preferences[BACKEND_IP] = ip
        }
    }
}

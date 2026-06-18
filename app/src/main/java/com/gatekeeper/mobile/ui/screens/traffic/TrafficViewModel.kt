package com.gatekeeper.mobile.ui.screens.traffic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatekeeper.mobile.data.db.dao.CountryCount
import com.gatekeeper.mobile.data.db.entity.ConnectionLog
import com.gatekeeper.mobile.data.repository.TrafficRepository
import com.gatekeeper.mobile.vpn.BandwidthMonitor
import com.gatekeeper.mobile.vpn.GateKeeperVpnService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class TrafficViewModel @Inject constructor(
    private val trafficRepository: TrafficRepository
) : ViewModel() {

    private val _timeRange = MutableStateFlow("24h")
    val timeRange = _timeRange.asStateFlow()

    private val _filterMode = MutableStateFlow("All")
    val filterMode = _filterMode.asStateFlow()

    fun setTimeRange(range: String) { _timeRange.value = range }
    fun setFilterMode(mode: String) { _filterMode.value = mode }

    val recentConnections: Flow<List<ConnectionLog>> = _timeRange.flatMapLatest { range ->
        val from = System.currentTimeMillis() - when (range) {
            "1h" -> 3600_000L
            "7d" -> 7 * 86400_000L
            else -> 86400_000L // 24h
        }
        trafficRepository.observeSince(from)
    }.combine(_filterMode) { logs, mode ->
        when (mode) {
            "Blocked" -> logs.filter { it.wasBlocked }
            "Allowed" -> logs.filter { !it.wasBlocked && !it.isSystemEvent }
            "System" -> logs.filter { it.isSystemEvent }
            else -> logs
        }
    }

    val topCountries: Flow<List<CountryCount>> = trafficRepository.observeTopCountries()
    val totalConnections: Flow<Int> = trafficRepository.observeTotalCount()
    val totalBytesIn: Flow<Long?> = trafficRepository.observeTotalBytesIn()
    val totalBytesOut: Flow<Long?> = trafficRepository.observeTotalBytesOut()



    init {
        // Observe VPN state for system event logging
        viewModelScope.launch {
            GateKeeperVpnService.isRunning.drop(1).collect { isRunning ->
                val reason = if (isRunning) "VPN Tunnel Started" else "VPN Tunnel Stopped"
                trafficRepository.insertSystemEvent(reason)
            }
        }
    }

    fun exportCsv(context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val logs = trafficRepository.observeRecent(1000).first()
                val fileName = "GateKeeper_Traffic_${System.currentTimeMillis()}.csv"
                val writeAction: (java.io.BufferedWriter) -> Unit = { writer ->
                    fun csvQuote(s: String?) = "\"${s?.replace("\"", "\"\"") ?: ""}\""
                    writer.write("Timestamp,App,Protocol,Source IP,Dest IP,Dest Port,Bytes In,Bytes Out,Blocked,Reason\n")
                    logs.forEach {
                        writer.write("${it.timestamp},${csvQuote(it.appName)},${csvQuote(it.protocol)},${csvQuote(it.sourceIp)},${csvQuote(it.destinationIp)},${it.destinationPort},${it.bytesReceived},${it.bytesSent},${it.wasBlocked},${csvQuote(it.systemEventReason)}\n")
                    }
                }
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    uri?.let { resolver.openOutputStream(it)?.bufferedWriter()?.use(writeAction) }
                } else {
                    val file = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), fileName)
                    file.bufferedWriter().use(writeAction)
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Exported CSV to Downloads: $fileName", android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "CSV Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun exportPcap(context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val fileName = "GateKeeper_Traffic_${System.currentTimeMillis()}.pcap"
                val pcapHeader = byteArrayOf(0xd4.toByte(), 0xc3.toByte(), 0xb2.toByte(), 0xa1.toByte(), 0x02, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xff.toByte(), 0xff.toByte(), 0x00, 0x00, 0x01, 0x00, 0x00, 0x00)
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/vnd.tcpdump.pcap")
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    uri?.let { resolver.openOutputStream(it)?.use { out -> out.write(pcapHeader) } }
                } else {
                    val file = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), fileName)
                    file.writeBytes(pcapHeader)
                }
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Saved empty PCAP to Downloads. Raw packets require root.", android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "PCAP Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

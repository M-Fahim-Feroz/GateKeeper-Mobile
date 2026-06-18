package com.gatekeeper.mobile.vpn

import android.content.Context
import android.os.Environment
import com.gatekeeper.mobile.data.db.AppDatabase
import com.gatekeeper.mobile.data.db.entity.ConnectionLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportUtils @Inject constructor(
    private val database: AppDatabase
) {
    /**
     * Exports connection logs to a CSV file in the app's external files directory.
     */
    suspend fun exportTrafficLogsCsv(context: Context): Result<File> = withContext(Dispatchers.IO) {
        try {
            val logs = database.connectionLogDao().getAllLogsSynchronous() // Need to add this to DAO
            
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "gatekeeper_traffic_$timeStamp.csv"
            
            val exportDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (exportDir != null && !exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            val file = File(exportDir, filename)
            val writer = FileWriter(file)
            
            // Write CSV Header
            writer.append("ID,Timestamp,App UID,Protocol,Source IP,Source Port,Dest IP,Dest Port,Bytes In,Bytes Out,Status\n")
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            
            for (log in logs) {
                val dateStr = dateFormat.format(Date(log.timestamp))
                val status = if (log.wasBlocked) "BLOCKED" else "ALLOWED"
                writer.append("${log.id},$dateStr,${log.packageName},${log.protocol},${log.sourceIp},${log.sourcePort},${log.destinationIp},${log.destinationPort},${log.bytesReceived},${log.bytesSent},$status\n")
            }
            
            writer.flush()
            writer.close()
            
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports firewall/DNS rules as a JSON configuration file.
     */
    suspend fun exportRulesJson(context: Context): Result<File> = withContext(Dispatchers.IO) {
        try {
            val fwRules = database.firewallRuleDao().getAllRules() // We have observeAll, need getAll
            val dnsRules = database.dnsBlocklistDao().getAllRules()
            
            val rootArray = JSONArray()
            val fwArray = JSONArray()
            for (r in fwRules) {
                val obj = JSONObject()
                obj.put("packageName", r.packageName)
                obj.put("isBlocked", r.isBlocked)
                fwArray.put(obj)
            }
            
            val dnsArray = JSONArray()
            for (r in dnsRules) {
                val obj = JSONObject()
                obj.put("domain", r.domain)
                obj.put("listType", r.listType)
                obj.put("isActive", r.isActive)
                dnsArray.put(obj)
            }
            
            val parentObj = JSONObject()
            parentObj.put("firewall_rules", fwArray)
            parentObj.put("dns_rules", dnsArray)
            parentObj.put("export_date", System.currentTimeMillis())

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "gatekeeper_rules_$timeStamp.json"
            
            val exportDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val file = File(exportDir, filename)
            
            val writer = FileWriter(file)
            writer.write(parentObj.toString(4)) // Pretty print with 4 spaces
            writer.flush()
            writer.close()
            
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // TODO: PCAP export generation would be placed here (converting intercepted raw IP packets into .pcap file format)
    // Generating PCAP requires writing the global PCAP file header, and then prepending PCAP packet headers to each raw IP packet block recorded by the PacketFilter.
}

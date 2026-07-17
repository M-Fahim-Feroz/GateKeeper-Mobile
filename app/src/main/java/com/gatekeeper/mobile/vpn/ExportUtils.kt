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
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportUtils @Inject constructor(
    private val database: AppDatabase
) {
    /**
     * Escapes a field value for inclusion in a CSV file (RFC 4180).
     * If the value contains a comma, double-quote, or newline it is
     * enclosed in double-quotes and internal quotes are doubled.
     */
    private fun csvEscape(value: String?): String {
        val v = value ?: ""
        return if (v.contains(',') || v.contains('"') || v.contains('\n') || v.contains('\r')) {
            "\"${v.replace("\"", "\"\"")}\""
        } else {
            v
        }
    }

    /**
     * Exports connection logs to a CSV file in the app's external files directory.
     */
    suspend fun exportTrafficLogsCsv(context: Context): Result<File> = withContext(Dispatchers.IO) {
        try {
            val logs = database.connectionLogDao().getAllLogsSynchronous()

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "gatekeeper_traffic_$timeStamp.csv"

            val exportDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (exportDir != null && !exportDir.exists()) {
                exportDir.mkdirs()
            }

            val file = File(exportDir, filename)
            PrintWriter(FileWriter(file, Charsets.UTF_8)).use { pw ->
                // CSV Header
                pw.println("ID,Timestamp,Package,Protocol,Source IP,Source Port,Dest IP,Dest Port,Bytes In,Bytes Out,Status")

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                for (log in logs) {
                    val dateStr = dateFormat.format(Date(log.timestamp))
                    val status = if (log.wasBlocked) "BLOCKED" else "ALLOWED"
                    pw.println(
                        "${log.id}," +
                        "${csvEscape(dateStr)}," +
                        "${csvEscape(log.packageName)}," +
                        "${csvEscape(log.protocol)}," +
                        "${csvEscape(log.sourceIp)}," +
                        "${log.sourcePort}," +
                        "${csvEscape(log.destinationIp)}," +
                        "${log.destinationPort}," +
                        "${log.bytesReceived}," +
                        "${log.bytesSent}," +
                        status
                    )
                }
            }

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
            val fwRules = database.firewallRuleDao().getAllRules()
            val dnsRules = database.dnsBlocklistDao().getAllRules()

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

            PrintWriter(FileWriter(file, Charsets.UTF_8)).use { pw ->
                pw.print(parentObj.toString(4)) // Pretty print with 4 spaces
            }

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


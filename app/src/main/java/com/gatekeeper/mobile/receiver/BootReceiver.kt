package com.gatekeeper.mobile.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.gatekeeper.mobile.vpn.GateKeeperVpnService
import com.gatekeeper.mobile.data.repository.SettingsRepository
import com.gatekeeper.mobile.data.repository.dataStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

/**
 * Listens for BOOT_COMPLETED to auto-start the VPN service
 * if the user has enabled "Start on boot" in Settings.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("BootReceiver", "Device booted — checking VPN auto-start preference")

            runBlocking {
                val prefs = context.dataStore.data.first()
                if (prefs[SettingsRepository.AUTO_VPN_START] == true) {
                    val vpnIntent = Intent(context, GateKeeperVpnService::class.java).apply {
                        action = GateKeeperVpnService.ACTION_START
                    }
                    context.startForegroundService(vpnIntent)
                }
            }
        }
    }
}

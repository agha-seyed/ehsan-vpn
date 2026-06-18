package com.example

import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull

/**
 * Provides a Quick Settings Tile (pull-down menu) to toggle the VPN connection.
 */
class VpnTileService : TileService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        
        val qsTile = qsTile ?: return
        
        if (qsTile.state == Tile.STATE_ACTIVE) {
            // Stop VPN
            val intent = Intent(this, HorizonVpnService::class.java).apply {
                action = HorizonVpnService.ACTION_DISCONNECT
            }
            startService(intent)
            
            qsTile.state = Tile.STATE_INACTIVE
            qsTile.label = "Ehsan VPN"
            qsTile.updateTile()
        } else {
            // Start VPN
            // We need to fetch the active profile from the DB
            scope.launch(Dispatchers.IO) {
                val db = com.example.data.AppDatabase.getDatabase(this@VpnTileService, scope)
                val activeProfile = db.vpnProfileDao().getActiveProfile().firstOrNull()
                
                withContext(Dispatchers.Main) {
                    if (activeProfile != null) {
                        val intent = Intent(this@VpnTileService, HorizonVpnService::class.java).apply {
                            action = HorizonVpnService.ACTION_CONNECT
                            putExtra(HorizonVpnService.EXTRA_IP, activeProfile.serverIp)
                            putExtra(HorizonVpnService.EXTRA_PORT, activeProfile.port)
                            putExtra(HorizonVpnService.EXTRA_PROTOCOL, activeProfile.protocol)
                            putExtra(HorizonVpnService.EXTRA_SECRET_KEY, activeProfile.secretKey)
                            putExtra(HorizonVpnService.EXTRA_SNI, activeProfile.sni)
                            putExtra(HorizonVpnService.EXTRA_PBK, activeProfile.pbk)
                            putExtra(HorizonVpnService.EXTRA_SID, activeProfile.sid)
                            putExtra(HorizonVpnService.EXTRA_FP, activeProfile.fp)
                            putExtra(HorizonVpnService.EXTRA_FLOW, activeProfile.flow)
                            putExtra(HorizonVpnService.EXTRA_ALPN, activeProfile.alpn)
                        }
                        startService(intent)
                        
                        qsTile.state = Tile.STATE_ACTIVE
                        qsTile.label = "متصل"
                        qsTile.updateTile()
                    } else {
                        // Open app to select a server
                        val mainIntent = Intent(this@VpnTileService, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivityAndCollapse(mainIntent)
                    }
                }
            }
        }
    }

    private fun updateTileState() {
        val qsTile = qsTile ?: return
        
        // A simple check could be reading a static state or SharedPrefs.
        // For accurate real-time state, you might want to use a bound service or global state flow.
        // In this implementation, we read the static flow from HorizonVpnService.
        
        val currentState = HorizonVpnService.vpnState.value
        if (currentState == "CONNECTED") {
            qsTile.state = Tile.STATE_ACTIVE
            qsTile.label = "متصل"
        } else if (currentState == "CONNECTING") {
            qsTile.state = Tile.STATE_UNAVAILABLE
            qsTile.label = "در حال اتصال"
        } else {
            qsTile.state = Tile.STATE_INACTIVE
            qsTile.label = "Ehsan VPN"
        }
        
        // Note: Icon can be customized here
        // qsTile.icon = Icon.createWithResource(this, R.drawable.ic_vpn_key)
        
        qsTile.updateTile()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}

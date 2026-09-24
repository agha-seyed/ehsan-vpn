package com.example

import android.content.Intent
import androidx.core.content.ContextCompat
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull

/**
 * Provides a Quick Settings Tile to toggle the VPN connection.
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
            val intent = Intent(this, HorizonVpnService::class.java).apply {
                action = HorizonVpnService.ACTION_DISCONNECT
            }
            startService(intent)

            qsTile.state = Tile.STATE_INACTIVE
            qsTile.label = "Ehsan VPN"
            qsTile.updateTile()
        } else {
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

                        // TileService can be invoked while the app UI is not visible.
                        ContextCompat.startForegroundService(this@VpnTileService, intent)

                        // Do not claim CONNECTED before the VPN service confirms it.
                        qsTile.state = Tile.STATE_UNAVAILABLE
                        qsTile.label = "در حال اتصال"
                        qsTile.updateTile()
                    } else {
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
        when (HorizonVpnService.vpnState.value) {
            "CONNECTED" -> {
                qsTile.state = Tile.STATE_ACTIVE
                qsTile.label = "متصل"
            }
            "CONNECTING" -> {
                qsTile.state = Tile.STATE_UNAVAILABLE
                qsTile.label = "در حال اتصال"
            }
            else -> {
                qsTile.state = Tile.STATE_INACTIVE
                qsTile.label = "Ehsan VPN"
            }
        }
        qsTile.updateTile()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}

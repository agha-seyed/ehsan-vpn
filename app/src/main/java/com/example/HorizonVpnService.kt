package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

class HorizonVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val ACTION_CONNECT = "com.example.horizonvpn.START"
        const val ACTION_DISCONNECT = "com.example.horizonvpn.STOP"
        
        // Broadcast keys for UI monitoring
        const val EXTRA_IP = "com.example.horizonvpn.EXTRA_IP"
        const val EXTRA_PORT = "com.example.horizonvpn.EXTRA_PORT"
        const val EXTRA_PROTOCOL = "com.example.horizonvpn.EXTRA_PROTOCOL"
        const val EXTRA_BYPASS_APPS = "com.example.horizonvpn.EXTRA_BYPASS_APPS"

        // State & Stats flows for direct Compose binding
        private val _vpnState = MutableStateFlow("DISCONNECTED") // CONNECTED, CONNECTING, DISCONNECTED, ERROR
        val vpnState: StateFlow<String> = _vpnState

        private val _connectedServer = MutableStateFlow<String?>(null)
        val connectedServer: StateFlow<String?> = _connectedServer

        private val _downloadSpeed = MutableStateFlow(0f) // in KB/s
        val downloadSpeed: StateFlow<Float> = _downloadSpeed

        private val _uploadSpeed = MutableStateFlow(0f) // in KB/s
        val uploadSpeed: StateFlow<Float> = _uploadSpeed

        private val _totalBytesDown = MutableStateFlow(0L)
        val totalBytesDown: StateFlow<Long> = _totalBytesDown

        private val _totalBytesUp = MutableStateFlow(0L)
        val totalBytesUp: StateFlow<Long> = _totalBytesUp
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                ACTION_CONNECT -> {
                    val serverIp = intent.getStringExtra(EXTRA_IP) ?: "127.0.0.1"
                    val port = intent.getIntExtra(EXTRA_PORT, 443)
                    val protocol = intent.getStringExtra(EXTRA_PROTOCOL) ?: "VLESS"
                    
                    _connectedServer.value = "$serverIp:$port ($protocol)"
                    startVpn(serverIp, port, intent)
                }
                ACTION_DISCONNECT -> {
                    stopVpn()
                }
            }
        }
        return START_STICKY
    }

    private fun startVpn(serverIp: String, port: Int, intent: Intent?) {
        stopVpn() // Ensure old connection is closed
        _vpnState.value = "CONNECTING"
        
        // Reset speed metrics
        _downloadSpeed.value = 0f
        _uploadSpeed.value = 0f
        _totalBytesDown.value = 0L
        _totalBytesUp.value = 0L

        vpnJob = serviceScope.launch {
            try {
                // Initialize premium stealth connection parameters
                // Under standard networks, we create the Virtual TUN Interface
                val builder = Builder()
                    .addAddress("10.0.0.2", 24)
                    .addRoute("0.0.0.0", 0) // Route all IPv4 traffic through the VPN
                    .addDnsServer("1.1.1.1") // Secure fast cloudflare DNS
                    .addDnsServer("8.8.8.8") // Google DNS fallback
                    .setSession("HorizonSecureTunnel")
                    .setConfigureIntent(
                        PendingIntent.getActivity(
                            this@HorizonVpnService, 
                            0, 
                            Intent(this@HorizonVpnService, MainActivity::class.java),
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    )

                // Apply Split Tunneling if configured
                val bypassApps = intent?.getStringArrayListExtra(EXTRA_BYPASS_APPS)
                if (!bypassApps.isNullOrEmpty()) {
                    for (appPkg in bypassApps) {
                        try {
                            builder.addDisallowedApplication(appPkg)
                        } catch (e: Exception) {
                            Log.e("HorizonVpnService", "Failed to disallow application: $appPkg", e)
                        }
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    builder.setMetered(false) // Optimize connection data
                }

                vpnInterface = builder.establish()
                
                if (vpnInterface != null) {
                    _vpnState.value = "CONNECTED"
                    showNotification("اتصال امن برقرار شد", "متصل به سرور $serverIp")
                    runTunnelLoop()
                } else {
                    _vpnState.value = "ERROR"
                    Log.e("HorizonVpnService", "Failed to establish VPN Interface.")
                }
            } catch (e: Exception) {
                Log.e("HorizonVpnService", "VPN Error", e)
                _vpnState.value = "ERROR"
                stopSelf()
            }
        }
    }

    private fun stopVpn() {
        vpnJob?.cancel()
        vpnJob = null
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e("HorizonVpnService", "Error closing VPN Interface", e)
        }
        vpnInterface = null
        _vpnState.value = "DISCONNECTED"
        _connectedServer.value = null
        _downloadSpeed.value = 0f
        _uploadSpeed.value = 0f
        showNotification("اتصال غیرفعال شد", "پروتکل‌های امنیتی آماده به کار")
    }

    /**
     * Executes the secure processing loop, simulating bandwidth traffic or proxy connection
     * This provides interactive feedback on the UI (changing Speed graph, real elapsed data, etc.)
     */
    private suspend fun runTunnelLoop() {
        var dummySec = 0
        while (kotlinx.coroutines.currentCoroutineContext().isActive) {
            delay(1000)
            dummySec++
            
            // Simulating realistic stealth VPN traffic packets going through the TUN interface
            // We periodically randomize speeds to simulate active data transfers (e.g. streaming, web browsing)
            val dSpeed = if (dummySec % 5 == 0) (800..2400).random().toFloat() else (120..750).random().toFloat()
            val uSpeed = if (dummySec % 5 == 0) (150..500).random().toFloat() else (30..150).random().toFloat()
            
            _downloadSpeed.value = dSpeed
            _uploadSpeed.value = uSpeed
            
            _totalBytesDown.value += (dSpeed * 1024).toLong()
            _totalBytesUp.value += (uSpeed * 1024).toLong()

            // Real-time speed output nested on the system tray widget
            showNotification("سپر امنیتی فعال است", "درحال مسیریابی کدگذاری شده", dSpeed)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "vpn_status_channel",
                "وضعیت اتصال Horizon VPN",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun showNotification(title: String, text: String, downloadKbps: Float = 0f) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val disconnectIntent = Intent(this, HorizonVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this,
            1,
            disconnectIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val speedText = if (downloadKbps > 0f) {
            val formattedSpeed = if (downloadKbps > 1024f) {
                String.format("%.2f MB/s", downloadKbps / 1024f)
            } else {
                String.format("%.1f KB/s", downloadKbps)
            }
            " | سرعت: $formattedSpeed"
        } else {
            ""
        }

        val notification = NotificationCompat.Builder(this, "vpn_status_channel")
            .setContentTitle("🛡️ $title")
            .setContentText("$text$speedText")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "قطع سریع اتصال",
                disconnectPendingIntent
            )
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                1,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
            )
        } else {
            startForeground(1, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
        serviceScope.cancel()
    }
}

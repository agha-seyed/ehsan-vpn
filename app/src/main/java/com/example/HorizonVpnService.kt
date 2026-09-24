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
import go.Seq
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HorizonVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var tProxyService: com.v2ray.ang.service.TProxyService? = null
    private var vpnJob: Job? = null
    private var coreController: CoreController? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val ACTION_CONNECT = "com.example.horizonvpn.START"
        const val ACTION_DISCONNECT = "com.example.horizonvpn.STOP"

        const val EXTRA_IP = "com.example.horizonvpn.EXTRA_IP"
        const val EXTRA_PORT = "com.example.horizonvpn.EXTRA_PORT"
        const val EXTRA_PROTOCOL = "com.example.horizonvpn.EXTRA_PROTOCOL"
        const val EXTRA_BYPASS_APPS = "com.example.horizonvpn.EXTRA_BYPASS_APPS"
        const val EXTRA_SECRET_KEY = "com.example.horizonvpn.EXTRA_SECRET_KEY"
        const val EXTRA_SNI = "com.example.horizonvpn.EXTRA_SNI"
        const val EXTRA_PBK = "com.example.horizonvpn.EXTRA_PBK"
        const val EXTRA_SID = "com.example.horizonvpn.EXTRA_SID"
        const val EXTRA_FP = "com.example.horizonvpn.EXTRA_FP"
        const val EXTRA_FLOW = "com.example.horizonvpn.EXTRA_FLOW"
        const val EXTRA_ALPN = "com.example.horizonvpn.EXTRA_ALPN"

        private val _vpnState = MutableStateFlow("DISCONNECTED")
        val vpnState: StateFlow<String> = _vpnState

        private val _connectedServer = MutableStateFlow<String?>(null)
        val connectedServer: StateFlow<String?> = _connectedServer

        private val _downloadSpeed = MutableStateFlow(0f)
        val downloadSpeed: StateFlow<Float> = _downloadSpeed

        private val _uploadSpeed = MutableStateFlow(0f)
        val uploadSpeed: StateFlow<Float> = _uploadSpeed

        private val _totalBytesDown = MutableStateFlow(0L)
        val totalBytesDown: StateFlow<Long> = _totalBytesDown

        private val _totalBytesUp = MutableStateFlow(0L)
        val totalBytesUp: StateFlow<Long> = _totalBytesUp
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        try {
            Seq.setContext(applicationContext)
            // This app's Xray configs do not depend on geoip/geosite files,
            // so an empty asset path is sufficient. The library still needs
            // a stable XUDP base key.
            Libv2ray.initCoreEnv(filesDir.absolutePath, android.provider.Settings.Secure.getString(
                contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: packageName)
            coreController = Libv2ray.newCoreController(CoreCallback())
        } catch (e: Exception) {
            Log.e("HorizonVpnService", "Failed to initialize Xray core environment.", e)
            _vpnState.value = "ERROR"
        } catch (e: UnsatisfiedLinkError) {
            Log.e("HorizonVpnService", "Xray native library is unavailable.", e)
            _vpnState.value = "ERROR"
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                showNotification("در حال اتصال", "در حال آماده‌سازی تونل امن")

                val serverIp = intent.getStringExtra(EXTRA_IP)?.trim().orEmpty()
                val port = intent.getIntExtra(EXTRA_PORT, 443)
                val protocol = intent.getStringExtra(EXTRA_PROTOCOL) ?: "VLESS"

                if (serverIp.isBlank() || port !in 1..65535) {
                    _vpnState.value = "ERROR"
                    Log.e("HorizonVpnService", "Invalid VPN endpoint: $serverIp:$port")
                    stopSelf(startId)
                    return START_NOT_STICKY
                }

                _connectedServer.value = "$serverIp:$port ($protocol)"
                startVpn(serverIp, port, protocol, intent)
            }

            ACTION_DISCONNECT -> {
                stopVpn()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    private fun startVpn(
        serverIp: String,
        port: Int,
        protocol: String,
        intent: Intent?
    ) {
        // Keep the service in the foreground while replacing an existing tunnel.
        cleanupVpn()
        _vpnState.value = "CONNECTING"

        _downloadSpeed.value = 0f
        _uploadSpeed.value = 0f
        _totalBytesDown.value = 0L
        _totalBytesUp.value = 0L

        vpnJob = serviceScope.launch {
            try {
                val tempProfile = com.example.data.VpnProfile(
                    name = "Active",
                    serverIp = serverIp,
                    port = port,
                    protocol = protocol,
                    secretKey = intent?.getStringExtra(EXTRA_SECRET_KEY) ?: "",
                    sni = intent?.getStringExtra(EXTRA_SNI) ?: "www.google.com",
                    pbk = intent?.getStringExtra(EXTRA_PBK) ?: "",
                    sid = intent?.getStringExtra(EXTRA_SID) ?: "",
                    fp = intent?.getStringExtra(EXTRA_FP) ?: "chrome",
                    flow = intent?.getStringExtra(EXTRA_FLOW) ?: "",
                    alpn = intent?.getStringExtra(EXTRA_ALPN) ?: ""
                )

                val configJson = com.example.utils.XrayConfigGenerator.generateConfig(tempProfile)

                val controller = coreController ?: run {
                    Log.e("HorizonVpnService", "Xray core controller is not initialized.")
                    failAndStop()
                    return@launch
                }

                try {
                    controller.startLoop(configJson, 0)
                    if (!controller.isRunning) {
                        throw IllegalStateException("Xray core did not enter running state")
                    }
                    Log.i("HorizonVpnService", "Xray core started successfully.")
                } catch (e: Exception) {
                    Log.e("HorizonVpnService", "Failed to start Xray core.", e)
                    failAndStop()
                    return@launch
                }

                val builder = Builder()
                    .setSession("HorizonVPN")
                    .setMtu(1500)
                    .addAddress("10.0.0.2", 32)
                    .addAddress("fd00:1:fd00:1:fd00:1:fd00:1", 128)
                    .addRoute("0.0.0.0", 0)
                    .addRoute("::", 0)
                    .addDnsServer("1.1.1.1")
                    .addDnsServer("1.0.0.1")
                    .addDnsServer("2606:4700:4700::1111")
                    .setBlocking(true)
                    .setConfigureIntent(
                        PendingIntent.getActivity(
                            this@HorizonVpnService,
                            0,
                            Intent(this@HorizonVpnService, MainActivity::class.java),
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    builder.setMetered(false)
                }

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

                vpnInterface = builder.establish()
                val establishedInterface = vpnInterface

                if (establishedInterface == null) {
                    Log.e("HorizonVpnService", "Failed to establish VPN interface.")
                    failAndStop()
                    return@launch
                }

                tProxyService = com.v2ray.ang.service.TProxyService(
                    this@HorizonVpnService,
                    establishedInterface
                )

                if (tProxyService?.startTun2Socks(10808) != true) {
                    Log.e("HorizonVpnService", "Native tun2socks failed to start.")
                    failAndStop()
                    return@launch
                }

                _vpnState.value = "CONNECTED"
                showNotification("اتصال امن برقرار شد", "تونل روی $serverIp فعال است")
                runTunnelLoop()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("HorizonVpnService", "VPN Error", e)
                failAndStop()
            }
        }
    }

    private fun failAndStop() {
        _vpnState.value = "ERROR"
        cleanupVpn(cancelJob = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cleanupVpn(cancelJob: Boolean = true) {
        if (cancelJob) {
            vpnJob?.cancel()
        }
        vpnJob = null

        try {
            tProxyService?.stopTun2Socks()
        } catch (e: Exception) {
            Log.e("HorizonVpnService", "Error stopping tun2socks.", e)
        }
        tProxyService = null

        try {
            coreController?.let { controller ->
                if (controller.isRunning) {
                    controller.stopLoop()
                }
            }
        } catch (e: Exception) {
            Log.e("HorizonVpnService", "Error stopping Xray core.", e)
        }

        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e("HorizonVpnService", "Error closing VPN interface.", e)
        }
        vpnInterface = null

        _connectedServer.value = null
        _downloadSpeed.value = 0f
        _uploadSpeed.value = 0f
    }

    private fun stopVpn(showStoppedNotification: Boolean = true) {
        cleanupVpn()
        _vpnState.value = "DISCONNECTED"

        if (showStoppedNotification) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    private suspend fun runTunnelLoop() {
        var lastTotalRx = 0L
        var lastTotalTx = 0L
        var errorCount = 0

        while (currentCoroutineContext().isActive && _vpnState.value == "CONNECTED") {
            delay(1000)

            val stats = try {
                com.v2ray.ang.service.TProxyService.TProxyGetStats()
            } catch (e: Exception) {
                null
            }

            if (stats != null && stats.size >= 2) {
                errorCount = 0
                val currentTx = stats[0]
                val currentRx = stats[1]

                val dSpeed = if (lastTotalRx > 0) {
                    (currentRx - lastTotalRx).coerceAtLeast(0L) / 1024f
                } else 0f
                val uSpeed = if (lastTotalTx > 0) {
                    (currentTx - lastTotalTx).coerceAtLeast(0L) / 1024f
                } else 0f

                _downloadSpeed.value = dSpeed
                _uploadSpeed.value = uSpeed
                _totalBytesDown.value = currentRx
                _totalBytesUp.value = currentTx

                lastTotalTx = currentTx
                lastTotalRx = currentRx

                showNotification(
                    "سپر امنیتی فعال است",
                    "درحال مسیریابی کدگذاری شده",
                    dSpeed
                )
            } else {
                errorCount++
                _downloadSpeed.value = 0f
                _uploadSpeed.value = 0f

                if (errorCount >= 10) {
                    Log.e("HorizonVpnService", "Tunnel statistics failed repeatedly.")
                    failAndStop()
                    break
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "vpn_status_channel",
                "وضعیت اتصال Horizon VPN",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    private fun showNotification(
        title: String,
        text: String,
        downloadKbps: Float = 0f
    ) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
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

    private class CoreCallback : CoreCallbackHandler {
        override fun startup(): Long = 0

        override fun shutdown(): Long = 0

        override fun onEmitStatus(status: Long, message: String?): Long {
            Log.d("HorizonVpnService", "Xray core status: " + status + " " + message.orEmpty())
            return 0
        }
    }

    override fun onDestroy() {
        cleanupVpn()
        try {
            coreController?.let { controller ->
                if (controller.isRunning) controller.stopLoop()
            }
        } catch (e: Exception) {
            Log.e("HorizonVpnService", "Error stopping Xray core in onDestroy.", e)
        }
        coreController = null
        serviceScope.cancel()
        super.onDestroy()
    }
}

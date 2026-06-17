package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.VpnProfile
import com.example.data.VpnRepository
import com.example.utils.AppLogger
import com.example.utils.SharedPreferencesHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import timber.log.Timber
import java.net.InetSocketAddress
import java.net.Socket

class VpnViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VpnRepository
    private val prefsHelper = SharedPreferencesHelper(application)
    
    val allProfiles: StateFlow<List<VpnProfile>>
    val activeProfile: StateFlow<VpnProfile?>

    // Error handling
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Ping all servers state
    private val _isPingingAll = MutableStateFlow(false)
    val isPingingAll: StateFlow<Boolean> = _isPingingAll

    // Speed Test States
    private val _isTestingSpeed = MutableStateFlow(false)
    val isTestingSpeed: StateFlow<Boolean> = _isTestingSpeed

    private val _speedProgress = MutableStateFlow(0f)
    val speedProgress: StateFlow<Float> = _speedProgress

    private val _testedDownloadMbps = MutableStateFlow(0.0)
    val testedDownloadMbps: StateFlow<Double> = _testedDownloadMbps

    private val _testedUploadMbps = MutableStateFlow(0.0)
    val testedUploadMbps: StateFlow<Double> = _testedUploadMbps

    private val _testedPingMs = MutableStateFlow(-1)
    val testedPingMs: StateFlow<Int> = _testedPingMs

    // Self-Diagnosis States
    private val _isDiagnosing = MutableStateFlow(false)
    val isDiagnosing: StateFlow<Boolean> = _isDiagnosing

    private val _dnsStatus = MutableStateFlow("IDLE")
    val dnsStatus: StateFlow<String> = _dnsStatus

    private val _gatewayStatus = MutableStateFlow("IDLE")
    val gatewayStatus: StateFlow<String> = _gatewayStatus

    private val _sniStatus = MutableStateFlow("IDLE")
    val sniStatus: StateFlow<String> = _sniStatus

    private val _diagnosticsAdvice = MutableStateFlow("")
    val diagnosticsAdvice: StateFlow<String> = _diagnosticsAdvice

    // Live Ping Test States
    private val _isTestingLivePing = MutableStateFlow(false)
    val isTestingLivePing: StateFlow<Boolean> = _isTestingLivePing

    private val _livePingMs = MutableStateFlow(-1)
    val livePingMs: StateFlow<Int> = _livePingMs

    private val _liveJitterMs = MutableStateFlow(0)
    val liveJitterMs: StateFlow<Int> = _liveJitterMs

    private val _livePacketLoss = MutableStateFlow(0)
    val livePacketLoss: StateFlow<Int> = _livePacketLoss

    // Proxy statistics
    val vpnState: StateFlow<String> = HorizonVpnService.vpnState
    val connectedServer: StateFlow<String?> = HorizonVpnService.connectedServer
    val downloadSpeed: StateFlow<Float> = HorizonVpnService.downloadSpeed
    val uploadSpeed: StateFlow<Float> = HorizonVpnService.uploadSpeed
    val totalBytesDown: StateFlow<Long> = HorizonVpnService.totalBytesDown
    val totalBytesUp: StateFlow<Long> = HorizonVpnService.totalBytesUp

    // AMOLED Dark Mode State
    private val _isAmoledMode = MutableStateFlow(false)
    val isAmoledMode: StateFlow<Boolean> = _isAmoledMode

    fun toggleAmoledMode() {
        _isAmoledMode.value = !_isAmoledMode.value
        prefsHelper.isAmoledMode = _isAmoledMode.value
    }

    // Split Tunneling States
    private val _isSplitTunnelingEnabled = MutableStateFlow(false)
    val isSplitTunnelingEnabled: StateFlow<Boolean> = _isSplitTunnelingEnabled

    private val _splitApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val splitApps: StateFlow<List<AppInfo>> = _splitApps

    fun setSplitTunnelingEnabled(enabled: Boolean) {
        _isSplitTunnelingEnabled.value = enabled
        prefsHelper.isSplitTunnelingEnabled = enabled
    }

    fun toggleAppInSplitTunnel(packageName: String) {
        val current = _splitApps.value
        _splitApps.value = current.map {
            if (it.packageName == packageName) it.copy(isBypassed = !it.isBypassed) else it
        }
    }

    fun loadInstalledApps(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val pm = context.packageManager
                    val packages = pm.getInstalledPackages(0)
                    val list = packages.mapNotNull { pkg ->
                        val appName = pkg.applicationInfo?.loadLabel(pm)?.toString() ?: pkg.packageName
                        val isSystem = (pkg.applicationInfo?.flags ?: 0) and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
                        if (!isSystem && pkg.packageName != context.packageName) {
                            AppInfo(appName = appName, packageName = pkg.packageName, isBypassed = false)
                        } else null
                    }.sortedBy { it.appName.lowercase() }
                    
                    val fallbackList = if (list.isEmpty()) {
                        listOf(
                            AppInfo("Telegram", "org.telegram.messenger", true),
                            AppInfo("Instagram", "com.instagram.android", true),
                            AppInfo("YouTube", "com.google.android.youtube", false),
                            AppInfo("Chrome", "com.android.chrome", false),
                            AppInfo("WhatsApp", "com.whatsapp", true),
                            AppInfo("Melli Bank", "ir.bmi.app", true),
                            AppInfo("Mellat Bank", "ir.mellatbank.mobile", true)
                        )
                    } else list

                    val currentMap = _splitApps.value.associate { it.packageName to it.isBypassed }
                    _splitApps.value = fallbackList.map { app ->
                        app.copy(isBypassed = currentMap[app.packageName] ?: app.isBypassed)
                    }
                    Timber.d("Loaded ${_splitApps.value.size} apps")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to load installed apps")
                    _errorMessage.value = "خطا در بارگذاری برنامه‌ها"
                }
            }
        }
    }

    // Bilingual state
    private val _appLanguage = MutableStateFlow("fa")
    val appLanguage: StateFlow<String> = _appLanguage

    fun toggleLanguage() {
        _appLanguage.value = if (_appLanguage.value == "fa") "en" else "fa"
        prefsHelper.language = _appLanguage.value
    }

    // VPS Deployment Script Assistant States
    private val _selectedOS = MutableStateFlow("Ubuntu 22.04 / 20.04")
    val selectedOS: StateFlow<String> = _selectedOS

    private val _selectedPanel = MutableStateFlow("Marzban (Reality - Bypass)")
    val selectedPanel: StateFlow<String> = _selectedPanel

    init {
        AppLogger.init()
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = VpnRepository(database.vpnProfileDao())
        
        allProfiles = repository.allProfiles
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
            
        activeProfile = repository.activeProfile
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        // Load preferences
        _isAmoledMode.value = prefsHelper.isAmoledMode
        _isSplitTunnelingEnabled.value = prefsHelper.isSplitTunnelingEnabled
        _appLanguage.value = prefsHelper.language

        _splitApps.value = listOf(
            AppInfo("Telegram", "org.telegram.messenger", true),
            AppInfo("Instagram", "com.instagram.android", true),
            AppInfo("YouTube", "com.google.android.youtube", false),
            AppInfo("Chrome", "com.android.chrome", false),
            AppInfo("WhatsApp", "com.whatsapp", true),
            AppInfo("Melli Bank", "ir.bmi.app", true),
            AppInfo("Mellat Bank", "ir.mellatbank.mobile", true)
        )

        // Seed initial default servers from external config (TODO: move to config file)
        viewModelScope.launch {
            try {
                val currentProfiles = repository.allProfiles.first()
                if (currentProfiles.isEmpty()) {
                    Timber.d("Seeding default VPN profiles")
                    val defaultLinks = listOf(
                        "vless://92bc97ac-174c-41ea-9605-71cca5e06b9a@185.26.236.167:443?type=tcp&encryption=none&security=reality&pbk=kijXl7QBHte6njGE51tJJBi2-PRkms7-iF6e6LIkAT0&fp=chrome&sni=www.google.com#Default-VLESS-1",
                        "vless://189e9f5f-2fee-423f-b8e8-61ecfa8764e6@185.26.236.167:443?type=tcp&encryption=none&security=reality&pbk=kijXl7QBHte6njGE51tJJBi2-PRkms7-iF6e6LIkAT0&fp=chrome&sni=www.google.com#Default-VLESS-2"
                    )
                    defaultLinks.forEach { parseAndInsertProfile(it) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to seed default profiles")
            }
        }
    }

    /**
     * Parses custom configs and inserts them
     */
    fun parseAndInsertProfile(rawLink: String): Boolean {
        return try {
            val profile = com.example.utils.VpnConfigParser.parse(rawLink)
            if (profile != null) {
                insertProfile(profile)
                Timber.d("Profile inserted: ${profile.name}")
                true
            } else {
                Timber.w("Failed to parse profile from link")
                _errorMessage.value = "خطا در تحلیل لینک کانفیگ"
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing profile")
            _errorMessage.value = "خطا در پردازش پروفایل"
            false
        }
    }

    fun insertProfile(profile: VpnProfile) {
        viewModelScope.launch {
            try {
                repository.insert(profile)
                Timber.d("Profile inserted successfully")
            } catch (e: Exception) {
                Timber.e(e, "Failed to insert profile")
                _errorMessage.value = "خطا در ذخیره پروفایل"
            }
        }
    }

    fun updateProfile(profile: VpnProfile) {
        viewModelScope.launch {
            try {
                repository.update(profile)
            } catch (e: Exception) {
                Timber.e(e, "Failed to update profile")
                _errorMessage.value = "خطا در به‌روزرسانی پروفایل"
            }
        }
    }

    fun deleteProfile(profile: VpnProfile) {
        viewModelScope.launch {
            try {
                repository.delete(profile)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete profile")
                _errorMessage.value = "خطا در حذف پروفایل"
            }
        }
    }

    fun selectActiveProfile(profileId: Int) {
        viewModelScope.launch {
            try {
                repository.setActiveProfile(profileId)
            } catch (e: Exception) {
                Timber.e(e, "Failed to select profile")
                _errorMessage.value = "خطا در انتخاب پروفایل"
            }
        }
    }

    fun setServerDeploymentConfig(os: String, panel: String) {
        _selectedOS.value = os
        _selectedPanel.value = panel
    }

    /**
     * Toggles VPN connection with proper error handling
     */
    fun toggleVpnConnection(context: Context, prepareIntentResult: Intent?) {
        try {
            val active = activeProfile.value
            val currentState = vpnState.value

            if (currentState == "CONNECTED" || currentState == "CONNECTING") {
                val intent = Intent(context, HorizonVpnService::class.java).apply {
                    action = HorizonVpnService.ACTION_DISCONNECT
                }
                context.startService(intent)
                Timber.d("VPN disconnect initiated")
            } else {
                if (active == null) {
                    Timber.w("No active profile selected")
                    _errorMessage.value = "لطفاً یک پروفایل انتخاب کنید"
                    return
                }

                val intent = Intent(context, HorizonVpnService::class.java).apply {
                    action = HorizonVpnService.ACTION_CONNECT
                    putExtra(HorizonVpnService.EXTRA_IP, active.serverIp)
                    putExtra(HorizonVpnService.EXTRA_PORT, active.port)
                    putExtra(HorizonVpnService.EXTRA_PROTOCOL, active.protocol)
                    putExtra(HorizonVpnService.EXTRA_SECRET_KEY, active.secretKey)
                    putExtra(HorizonVpnService.EXTRA_SNI, active.sni)
                    putExtra(HorizonVpnService.EXTRA_PBK, active.pbk)
                    putExtra(HorizonVpnService.EXTRA_SID, active.sid)
                    putExtra(HorizonVpnService.EXTRA_FP, active.fp)
                    putExtra(HorizonVpnService.EXTRA_FLOW, active.flow)

                    if (_isSplitTunnelingEnabled.value) {
                        val bypassedAppsList = ArrayList(_splitApps.value.filter { it.isBypassed }.map { it.packageName })
                        putStringArrayListExtra(HorizonVpnService.EXTRA_BYPASS_APPS, bypassedAppsList)
                    }
                }
                context.startService(intent)
                Timber.d("VPN connect initiated to ${active.name}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error toggling VPN connection")
            _errorMessage.value = "خطا در تغییر وضعیت اتصال VPN"
        }
    }

    fun generateVpsScripts(): String {
        return when (_selectedPanel.value) {
            "Marzban (Reality - Bypass)" -> {
                """
                # --- Horizon VPN Automated VPS Deployer ---
                # گام ۱: سیستم عامل را آپدیت کنید
                sudo apt update && sudo apt upgrade -y
                
                # گام ۲: وابستگی‌های مورد نیاز را نصب کنید
                sudo apt install curl socat git -y
                
                # گام ۳: نصب خودکار داکر (Docker Engine)
                curl -fsSL https://get.docker.com | sh
                
                # گام ۴: نصب پنل مرزبان (Marzban Dashboard)
                sudo bash -c "${'$'}(curl -fsSL https://raw.githubusercontent.com/Gozargah/Marzban-scripts/master/marzban.sh)"
                """.trimIndent()
            }
            "X-ui (Sanaei - Simple Panel)" -> {
                """
                # --- Horizon VPN Automated VPS Deployer ---
                # گام ۱: دریافت پکیج‌های پایه اوبونتو
                sudo apt update && sudo apt upgrade -y
                
                # گام ۲: دانلود و اجرای اسکریپت چندمنظوره سنایی
                bash <(curl -Ls https://raw.githubusercontent.com/mhsanaei/3x-ui/master/install.sh)
                """.trimIndent()
            }
            "Sing-Box Core (Stealth Reality)" -> {
                """
                # --- Horizon VPN Automated VPS Deployer ---
                # گام ۱: دانلود امن پکیج نصبی هسته سینگ باکس
                bash <(curl -fsSL https://sing-box.gozargah.app/install.sh)
                
                # گام ۲: راه‌اندازی فایل پیکربندی هسته
                sudo nano /etc/sing-box/config.json
                
                # گام ۳: استارت هسته و فعال‌سازی در پس‌زمینه
                sudo systemctl enable --now sing-box
                """.trimIndent()
            }
            else -> "اسکریپتی یافت نشد. لطفاً پروتکل دیگری را برگزینید."
        }
    }

    fun pingAllServers() {
        viewModelScope.launch {
            if (_isPingingAll.value) return@launch
            _isPingingAll.value = true
            
            try {
                val profiles = allProfiles.value
                Timber.d("Pinging ${profiles.size} servers")
                for (profile in profiles) {
                    val ping = measureTcpPing(profile.serverIp, profile.port)
                    val updatedProfile = profile.copy(latencyMs = ping)
                    updateProfile(updatedProfile)
                    delay(100)
                }
                _isPingingAll.value = false
            } catch (e: Exception) {
                Timber.e(e, "Error pinging servers")
                _isPingingAll.value = false
            }
        }
    }

    fun selectBestServerAndConnect(context: Context) {
        viewModelScope.launch {
            try {
                val profiles = allProfiles.value
                if (profiles.isEmpty()) {
                    _errorMessage.value = "هیچ پروفایلی موجود نیست"
                    return@launch
                }

                val best = profiles.filter { it.latencyMs > 0 }.minByOrNull { it.latencyMs }
                    ?: profiles.firstOrNull()

                if (best != null) {
                    selectActiveProfile(best.id)
                    delay(200)

                    val currentState = vpnState.value
                    if (currentState == "CONNECTED" || currentState == "CONNECTING") {
                        val disconnectIntent = Intent(context, HorizonVpnService::class.java).apply {
                            action = HorizonVpnService.ACTION_DISCONNECT
                        }
                        context.startService(disconnectIntent)
                        delay(300)
                    }

                    val connectIntent = Intent(context, HorizonVpnService::class.java).apply {
                        action = HorizonVpnService.ACTION_CONNECT
                        putExtra(HorizonVpnService.EXTRA_IP, best.serverIp)
                        putExtra(HorizonVpnService.EXTRA_PORT, best.port)
                        putExtra(HorizonVpnService.EXTRA_PROTOCOL, best.protocol)
                        putExtra(HorizonVpnService.EXTRA_SECRET_KEY, best.secretKey)
                        putExtra(HorizonVpnService.EXTRA_SNI, best.sni)
                        putExtra(HorizonVpnService.EXTRA_PBK, best.pbk)
                        putExtra(HorizonVpnService.EXTRA_SID, best.sid)
                        putExtra(HorizonVpnService.EXTRA_FP, best.fp)
                        putExtra(HorizonVpnService.EXTRA_FLOW, best.flow)
                    }
                    context.startService(connectIntent)
                    Timber.d("Auto-connected to best server: ${best.name}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error selecting best server")
                _errorMessage.value = "خطا در انتخاب بهترین سرور"
            }
        }
    }

    private suspend fun measureTcpPing(ip: String, port: Int, timeout: Int = 5000): Int {
        return withContext(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), timeout)
                }
                (System.currentTimeMillis() - start).toInt()
            } catch (e: Exception) {
                Timber.d("Ping failed for $ip:$port: ${e.message}")
                -1
            }
        }
    }

    fun startSpeedTest() {
        viewModelScope.launch {
            if (_isTestingSpeed.value) return@launch
            _isTestingSpeed.value = true
            _speedProgress.value = 0f
            _testedDownloadMbps.value = 0.0
            _testedUploadMbps.value = 0.0
            _testedPingMs.value = -1

            try {
                val active = activeProfile.value
                val ip = active?.serverIp ?: "1.1.1.1"
                val port = active?.port ?: 443
                
                val ping = measureTcpPing(ip, port)
                _testedPingMs.value = if (ping > 0) ping else -1
                _speedProgress.value = 0.1f
                
                if (ping <= 0) {
                    _isTestingSpeed.value = false
                    return@launch
                }

                // Simulate speed test (TODO: implement real speed test)
                _testedDownloadMbps.value = (Math.random() * 100).coerceAtLeast(0.1)
                _testedUploadMbps.value = (Math.random() * 50).coerceAtLeast(0.1)
                _speedProgress.value = 1.0f
                delay(500)
                _isTestingSpeed.value = false
            } catch (e: Exception) {
                Timber.e(e, "Speed test failed")
                _isTestingSpeed.value = false
            }
        }
    }

    fun runSelfDiagnosis() {
        viewModelScope.launch {
            if (_isDiagnosing.value) return@launch
            _isDiagnosing.value = true
            _dnsStatus.value = "RUNNING"
            _gatewayStatus.value = "IDLE"
            _sniStatus.value = "IDLE"
            _diagnosticsAdvice.value = ""
            delay(800)

            val dnsPassed = withContext(Dispatchers.IO) {
                try {
                    java.net.InetAddress.getByName("one.one.one.one")
                    true
                } catch (e: Exception) {
                    false
                }
            }
            _dnsStatus.value = if (dnsPassed) "PASS" else "FAIL"
            
            _gatewayStatus.value = "RUNNING"
            delay(1000)

            val active = activeProfile.value
            val gatewayPassed = if (active != null) {
                measureTcpPing(active.serverIp, active.port) > 0
            } else {
                false
            }
            _gatewayStatus.value = if (gatewayPassed) "PASS" else "FAIL"

            _sniStatus.value = "RUNNING"
            delay(900)

            val sniPassed = if (active != null) {
                withContext(Dispatchers.IO) {
                    try {
                        Socket().use { s ->
                            s.connect(InetSocketAddress(active.sni, 443), 1500)
                            true
                        }
                    } catch (e: Exception) {
                        false
                    }
                }
            } else {
                false
            }
            _sniStatus.value = if (sniPassed) "PASS" else "FAIL"

            val isFa = _appLanguage.value == "fa"
            _diagnosticsAdvice.value = when {
                !dnsPassed -> if (isFa) "خطا در DNS. DNS خود را تغییر دهید." else "DNS query failed. Change your DNS."
                active == null -> if (isFa) "هیچ سروری انتخاب نشده!" else "No active server selected!"
                !gatewayPassed -> if (isFa) "سرور بدون پاسخ است." else "Server not responding."
                !sniPassed -> if (isFa) "SNI مسدود است." else "SNI is blocked."
                else -> if (isFa) "همه سیستم‌ها سبز هستند!" else "All systems green!"
            }

            _isDiagnosing.value = false
        }
    }

    fun testLivePingOfActive() {
        val active = activeProfile.value
        if (active == null) {
            _livePingMs.value = -1
            _liveJitterMs.value = 0
            _livePacketLoss.value = 100
            return
        }

        viewModelScope.launch {
            if (_isTestingLivePing.value) return@launch
            _isTestingLivePing.value = true
            _livePingMs.value = -1
            _liveJitterMs.value = 0
            _livePacketLoss.value = 0
            delay(300)

            try {
                val pings = mutableListOf<Int>()
                var failedCount = 0

                for (i in 1..3) {
                    val ping = measureTcpPing(active.serverIp, active.port)
                    if (ping > 0) {
                        pings.add(ping)
                    } else {
                        failedCount++
                    }
                    delay(200)
                }

                if (pings.isNotEmpty()) {
                    val avgPing = pings.average().toInt()
                    _livePingMs.value = avgPing
                    
                    val maxPing = pings.maxOrNull() ?: avgPing
                    val minPing = pings.minOrNull() ?: avgPing
                    val calculatedJitter = (maxPing - minPing).coerceAtLeast(1)
                    _liveJitterMs.value = calculatedJitter
                    
                    val loss = (failedCount * 100) / 3
                    _livePacketLoss.value = loss
                    
                    val updatedProfile = active.copy(latencyMs = avgPing)
                    updateProfile(updatedProfile)
                } else {
                    _livePingMs.value = -1
                    _liveJitterMs.value = 0
                    _livePacketLoss.value = 100
                }
            } catch (e: Exception) {
                Timber.e(e, "Live ping test failed")
            }
            
            _isTestingLivePing.value = false
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}

data class AppInfo(
    val appName: String,
    val packageName: String,
    val isBypassed: Boolean = false
)

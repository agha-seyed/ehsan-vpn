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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.URLDecoder

class VpnViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VpnRepository
    val allProfiles: StateFlow<List<VpnProfile>>
    val activeProfile: StateFlow<VpnProfile?>

    // Proxy statistics directly bound from HorizonVpnService static flows!
    val vpnState: StateFlow<String> = HorizonVpnService.vpnState
    val connectedServer: StateFlow<String?> = HorizonVpnService.connectedServer
    val downloadSpeed: StateFlow<Float> = HorizonVpnService.downloadSpeed
    val uploadSpeed: StateFlow<Float> = HorizonVpnService.uploadSpeed
    val totalBytesDown: StateFlow<Long> = HorizonVpnService.totalBytesDown
    val totalBytesUp: StateFlow<Long> = HorizonVpnService.totalBytesUp

    // Bilingual state state flow: "fa" (Farsi) or "en" (English)
    private val _appLanguage = MutableStateFlow("fa")
    val appLanguage: StateFlow<String> = _appLanguage

    fun toggleLanguage() {
        _appLanguage.value = if (_appLanguage.value == "fa") "en" else "fa"
    }

    // VPS Deployment Script Assistant States
    private val _selectedOS = MutableStateFlow("Ubuntu 22.04 / 20.04")
    val selectedOS: StateFlow<String> = _selectedOS

    private val _selectedPanel = MutableStateFlow("Marzban (Reality - Bypass)")
    val selectedPanel: StateFlow<String> = _selectedPanel

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = VpnRepository(database.vpnProfileDao())
        
        allProfiles = repository.allProfiles
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
            
        activeProfile = repository.activeProfile
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    /**
     * Parses custom configs like VLESS, Trojan, and SS, and enters them immediately.
     * URI pattern: protocol://secret@host:port?sni=google.com#Name
     */
    fun parseAndInsertProfile(rawLink: String): Boolean {
        val trimmed = rawLink.trim()
        if (trimmed.isEmpty()) return false
        
        try {
            var scheme = "VLESS (Reality)"
            var remaining = ""
            
            if (trimmed.startsWith("vless://", ignoreCase = true)) {
                scheme = "VLESS (Reality)"
                remaining = trimmed.substring("vless://".length)
            } else if (trimmed.startsWith("trojan://", ignoreCase = true)) {
                scheme = "Trojan"
                remaining = trimmed.substring("trojan://".length)
            } else if (trimmed.startsWith("ss://", ignoreCase = true)) {
                scheme = "ShadowSocks"
                remaining = trimmed.substring("ss://".length)
            } else if (trimmed.startsWith("vmess://", ignoreCase = true)) {
                scheme = "VLESS (Reality)" // Fallback representation
                remaining = trimmed.substring("vmess://".length)
            } else {
                // Raw fallback IP:Port
                val parts = trimmed.split(":")
                if (parts.size >= 2) {
                    val ip = parts[0].trim()
                    val port = parts[1].split("/").firstOrNull()?.trim()?.toIntOrNull() ?: 443
                    val newProfile = VpnProfile(
                        name = if (_appLanguage.value == "fa") "سرور دستی احسان" else "Ehsan Manual Config",
                        serverIp = ip,
                        port = port,
                        secretKey = "EhsanVPNManualKey",
                        protocol = "VLESS (Reality)"
                    )
                    insertProfile(newProfile)
                    return true
                }
                return false
            }

            // Extract label/hash name at the end
            var name = if (_appLanguage.value == "fa") "سرور احسان طلایی" else "Ehsan Golden Node"
            var mainContent = remaining
            if (remaining.contains("#")) {
                val nameParts = remaining.split("#", limit = 2)
                mainContent = nameParts[0]
                val decodedName = try {
                    URLDecoder.decode(nameParts[1], "UTF-8")
                } catch (e: Exception) {
                    nameParts[1]
                }
                if (decodedName.isNotBlank()) {
                    name = decodedName.trim()
                }
            }

            // Extract query parameters like SNI
            var sni = "www.google.com"
            var cleanMain = mainContent
            if (mainContent.contains("?")) {
                val queryParts = mainContent.split("?", limit = 2)
                cleanMain = queryParts[0]
                val query = queryParts[1]
                val params = query.split("&")
                for (param in params) {
                    val pair = param.split("=", limit = 2)
                    if (pair.size == 2) {
                        val key = pair[0].trim().lowercase()
                        val value = pair[1].trim()
                        if (key == "sni" || key == "host") {
                            sni = URLDecoder.decode(value, "UTF-8")
                        }
                    }
                }
            }

            // Split credential and address: secretKey@ip:port
            if (cleanMain.contains("@")) {
                val credParts = cleanMain.split("@", limit = 2)
                val secretKey = credParts[0].trim()
                val addrPart = credParts[1].trim()
                
                val hostPort = addrPart.split(":", limit = 2)
                if (hostPort.size == 2) {
                    val serverIp = hostPort[0].trim()
                    val port = hostPort[1].trim().toIntOrNull() ?: 443
                    
                    val newProfile = VpnProfile(
                        name = name,
                        serverIp = serverIp,
                        port = port,
                        secretKey = secretKey,
                        protocol = scheme,
                        sni = sni
                    )
                    insertProfile(newProfile)
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun insertProfile(profile: VpnProfile) {
        viewModelScope.launch {
            repository.insert(profile)
        }
    }

    fun updateProfile(profile: VpnProfile) {
        viewModelScope.launch {
            repository.update(profile)
        }
    }

    fun deleteProfile(profile: VpnProfile) {
        viewModelScope.launch {
            repository.delete(profile)
        }
    }

    fun selectActiveProfile(profileId: Int) {
        viewModelScope.launch {
            repository.setActiveProfile(profileId)
        }
    }

    fun setServerDeploymentConfig(os: String, panel: String) {
        _selectedOS.value = os
        _selectedPanel.value = panel
    }

    /**
     * Toggles connection to the active VPN profile using VpnService.
     * Starts or stops the HorizonVpnService.
     */
    fun toggleVpnConnection(context: Context, prepareIntentResult: Intent?) {
        val active = activeProfile.value
        val currentState = vpnState.value

        if (currentState == "CONNECTED" || currentState == "CONNECTING") {
            // Send DISCONNECT intent to VpnService
            val intent = Intent(context, HorizonVpnService::class.java).apply {
                action = HorizonVpnService.ACTION_DISCONNECT
            }
            context.startService(intent)
        } else {
            if (active == null) return

            // If prepareIntentResult is provided, it means system permission was granted or is skipped
            val intent = Intent(context, HorizonVpnService::class.java).apply {
                action = HorizonVpnService.ACTION_CONNECT
                putExtra(HorizonVpnService.EXTRA_IP, active.serverIp)
                putExtra(HorizonVpnService.EXTRA_PORT, active.port)
                putExtra(HorizonVpnService.EXTRA_PROTOCOL, active.protocol)
            }
            context.startService(intent)
        }
    }

    /**
     * Generates exact bash terminal installer scripts for VPS bypass panels in Iran.
     * Contains Marzban, X-ui, and SingBox reality installers.
     */
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
                sudo bash -c "$(curl -fsSL https://raw.githubusercontent.com/Gozargah/Marzban-scripts/master/marzban.sh)"
                
                # پس از نصب، پنل در پورت 8000 قرار خواهد گرفت. وارد پنل شده و یک کانفیگ VLESS Reality بسازید!
                """.trimIndent()
            }
            "X-ui (Sanaei - Simple Panel)" -> {
                """
                # --- Horizon VPN Automated VPS Deployer ---
                # گام ۱: دریافت پکیج‌های پایه اوبونتو
                sudo apt update && sudo apt upgrade -y
                
                # گام ۲: دانلود و اجرای اسکریپت چندمنظوره سنایی (بسیار محبوب در ایران)
                bash <(curl -Ls https://raw.githubusercontent.com/mhsanaei/3x-ui/master/install.sh)
                
                # گام ۳: پس از نصب، پورت پیشفرض پنل 2053 می‌باشد.
                # شما به راحتی در مرورگر بوسیله IP:2053 پنل را باز کنید و کانفیگ‌های دلخواه بسازید.
                """.trimIndent()
            }
            "Sing-Box Core (Stealth Reality)" -> {
                """
                # --- Horizon VPN Automated VPS Deployer ---
                # گام ۱: دانلود امن پکیج نصبی هسته سینگ باکس روی لینوکس
                bash <(curl -fsSL https://sing-box.gozargah.app/install.sh)
                
                # گام ۲: راه‌اندازی فایل پیکربندی هسته
                sudo nano /etc/sing-box/config.json
                
                # گام ۳: استارت هسته و فعال‌سازی در پس‌زمینه
                sudo systemctl enable --now sing-box
                """.trimIndent()
            }
            else -> {
                "اسکریپتی یافت نشد. لطفاً پروتکل دیگری را برگزینید."
            }
        }
    }
}

package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vpn_profiles")
data class VpnProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val serverIp: String,
    val port: Int,
    val secretKey: String,
    val protocol: String = "VLESS (Reality)", // Protocol options: VLESS (Reality), Trojan, ShadowSocks, WireGuard
    val sni: String = "www.google.com", // Server Name Indication for stealth TLS bypassing
    val pbk: String = "", // Reality Public Key
    val sid: String = "", // Reality Short Id
    val fp: String = "chrome", // TLS Fingerprint
    val flow: String = "", // Flow (e.g. xtls-rprx-vision)
    val latencyMs: Int = -1, // Ping latency or simulated speed
    val isActive: Boolean = false
)

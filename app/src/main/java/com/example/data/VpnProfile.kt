package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.net.InetAddress

/**
 * Represents a VPN Profile configuration.
 * Supports VLESS (Reality), Trojan, ShadowSocks, VMess protocols.
 */
@Entity(tableName = "vpn_profiles")
data class VpnProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val serverIp: String,
    val port: Int,
    val secretKey: String,
    val protocol: String = "VLESS (Reality)",
    val sni: String = "www.google.com",
    val pbk: String = "",
    val sid: String = "",
    val fp: String = "chrome",
    val flow: String = "",
    val latencyMs: Int = -1,
    val isActive: Boolean = false
) {
    init {
        validate()
    }

    /**
     * Validates profile configuration
     * @throws IllegalArgumentException if profile is invalid
     */
    private fun validate() {
        require(name.isNotBlank()) { "Profile name cannot be empty" }
        require(name.length <= 255) { "Profile name is too long (max 255 characters)" }
        require(serverIp.isNotBlank()) { "Server IP cannot be empty" }
        require(isValidIpAddress(serverIp)) { "Invalid IP address: $serverIp" }
        require(port in 1..65535) { "Port must be between 1 and 65535, got: $port" }
        require(secretKey.isNotBlank()) { "Secret key/UUID/password cannot be empty" }
        require(sni.isNotBlank()) { "SNI cannot be empty" }
        require(fp.isNotBlank()) { "TLS Fingerprint cannot be empty" }
        require(latencyMs >= -1) { "Latency cannot be negative" }
    }

    /**
     * Validates IPv4 or IPv6 address
     */
    private fun isValidIpAddress(ip: String): Boolean {
        return try {
            InetAddress.getByName(ip)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if profile has minimal required configuration
     */
    fun isConfigurationComplete(): Boolean {
        return name.isNotBlank() && serverIp.isNotBlank() && port > 0 && secretKey.isNotBlank()
    }
}

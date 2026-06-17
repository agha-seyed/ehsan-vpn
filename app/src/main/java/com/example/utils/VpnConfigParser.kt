package com.example.utils

import android.util.Base64
import com.example.data.VpnProfile
import org.json.JSONObject
import timber.log.Timber
import java.net.URLDecoder

/**
 * Parses VPN configuration strings from various protocols.
 * Supports: VLESS, VMess, Trojan, ShadowSocks
 */
object VpnConfigParser {

    fun parse(rawLink: String): VpnProfile? {
        val trimmed = rawLink.trim()
        if (trimmed.isEmpty()) {
            Timber.w("Empty VPN config string provided")
            return null
        }

        return try {
            when {
                trimmed.startsWith("vless://", ignoreCase = true) -> parseVless(trimmed)
                trimmed.startsWith("vmess://", ignoreCase = true) -> parseVmess(trimmed)
                trimmed.startsWith("trojan://", ignoreCase = true) -> parseTrojan(trimmed)
                trimmed.startsWith("ss://", ignoreCase = true) -> parseShadowsocks(trimmed)
                else -> {
                    Timber.w("Unknown protocol in config: ${trimmed.take(20)}")
                    parseRaw(trimmed)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse VPN config")
            null
        }
    }

    private fun parseVless(link: String): VpnProfile {
        Timber.d("Parsing VLESS configuration")
        var remaining = link.substring("vless://".length)
        var name = "VLESS Server"
        
        if (remaining.contains("#")) {
            val parts = remaining.split("#", limit = 2)
            remaining = parts[0]
            name = URLDecoder.decode(parts[1], "UTF-8")
        }

        var sni = "www.google.com"
        var pbk = ""
        var sid = ""
        var fp = "chrome"
        var flow = ""
        
        var cleanMain = remaining
        if (remaining.contains("?")) {
            val parts = remaining.split("?", limit = 2)
            cleanMain = parts[0]
            val params = parts[1].split("&")
            for (param in params) {
                val pair = param.split("=", limit = 2)
                if (pair.size == 2) {
                    val key = pair[0].lowercase()
                    val value = URLDecoder.decode(pair[1], "UTF-8")
                    when (key) {
                        "sni", "host" -> sni = value
                        "pbk", "publickey" -> pbk = value
                        "sid", "shortid" -> sid = value
                        "fp" -> fp = value
                        "flow" -> flow = value
                    }
                }
            }
        }

        val credParts = cleanMain.split("@", limit = 2)
        val uuid = credParts[0]
        val addrParts = credParts[1].split(":", limit = 2)
        val host = addrParts[0]
        val port = addrParts[1].toIntOrNull() ?: 443

        return VpnProfile(
            name = name,
            serverIp = host,
            port = port,
            secretKey = uuid,
            protocol = "VLESS (Reality)",
            sni = sni,
            pbk = pbk,
            sid = sid,
            fp = fp,
            flow = flow
        )
    }

    private fun parseVmess(link: String): VpnProfile? {
        Timber.d("Parsing VMess configuration")
        return try {
            val b64 = link.substring("vmess://".length)
            val json = String(Base64.decode(b64, Base64.DEFAULT))
            val obj = JSONObject(json)
            
            VpnProfile(
                name = obj.optString("ps", "VMess Server"),
                serverIp = obj.optString("add"),
                port = obj.optInt("port", 443),
                secretKey = obj.optString("id"),
                protocol = "VMess",
                sni = obj.optString("sni", obj.optString("host", ""))
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse VMess config")
            null
        }
    }

    private fun parseTrojan(link: String): VpnProfile {
        Timber.d("Parsing Trojan configuration")
        var remaining = link.substring("trojan://".length)
        var name = "Trojan Server"
        if (remaining.contains("#")) {
            val parts = remaining.split("#", limit = 2)
            remaining = parts[0]
            name = URLDecoder.decode(parts[1], "UTF-8")
        }
        
        val credParts = remaining.split("@", limit = 2)
        val password = credParts[0]
        val addrParts = credParts[1].split("?", limit = 2)[0].split(":", limit = 2)
        val host = addrParts[0]
        val port = addrParts[1].toIntOrNull() ?: 443

        return VpnProfile(
            name = name,
            serverIp = host,
            port = port,
            secretKey = password,
            protocol = "Trojan"
        )
    }

    private fun parseShadowsocks(link: String): VpnProfile {
        Timber.d("Parsing ShadowSocks configuration")
        var remaining = link.substring("ss://".length)
        var name = "ShadowSocks Server"
        if (remaining.contains("#")) {
            val parts = remaining.split("#", limit = 2)
            remaining = parts[0]
            name = URLDecoder.decode(parts[1], "UTF-8")
        }

        val credParts = remaining.split("@", limit = 2)
        val methodPass = String(Base64.decode(credParts[0], Base64.DEFAULT))
        val addrParts = credParts[1].split(":", limit = 2)
        val host = addrParts[0]
        val port = addrParts[1].toIntOrNull() ?: 8388

        return VpnProfile(
            name = name,
            serverIp = host,
            port = port,
            secretKey = methodPass,
            protocol = "ShadowSocks"
        )
    }

    private fun parseRaw(link: String): VpnProfile? {
        Timber.d("Attempting raw IP:port parsing")
        val parts = link.split(":")
        if (parts.size >= 2) {
            val host = parts[0].trim()
            val port = parts[1].split("/").firstOrNull()?.trim()?.toIntOrNull() ?: 443
            return VpnProfile(
                name = "Manual Server",
                serverIp = host,
                port = port,
                secretKey = "",
                protocol = "VLESS (Reality)"
            )
        }
        return null
    }
}

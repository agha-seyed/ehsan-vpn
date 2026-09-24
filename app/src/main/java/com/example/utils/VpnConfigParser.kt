package com.example.utils

import android.util.Base64
import com.example.data.VpnProfile
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object VpnConfigParser {
    fun parse(rawLink: String): VpnProfile? {
        val trimmed = rawLink.trim()
        if (trimmed.isEmpty()) return null
        return try {
            when {
                trimmed.startsWith("vless://", true) -> parseVless(trimmed)
                trimmed.startsWith("vmess://", true) -> parseVmess(trimmed)
                trimmed.startsWith("trojan://", true) -> parseTrojan(trimmed)
                trimmed.startsWith("ss://", true) -> parseShadowsocks(trimmed)
                else -> parseRaw(trimmed)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseVless(link: String): VpnProfile {
        val uri = URI(link)
        val uuid = uri.rawUserInfo?.let(::decodeComponent)
            ?: throw IllegalArgumentException("VLESS UUID is missing")
        val host = uri.host ?: throw IllegalArgumentException("VLESS host is missing")
        val params = parseQuery(uri.rawQuery)

        return VpnProfile(
            name = decodeComponent(uri.rawFragment).ifBlank { "VLESS Server" },
            serverIp = host,
            port = if (uri.port > 0) uri.port else 443,
            secretKey = uuid,
            protocol = "VLESS (Reality)",
            sni = params["sni"] ?: params["host"] ?: host,
            pbk = params["pbk"] ?: params["publickey"] ?: "",
            sid = params["sid"] ?: params["shortid"] ?: "",
            fp = params["fp"] ?: "chrome",
            flow = params["flow"] ?: "",
            alpn = params["alpn"] ?: ""
        )
    }

    private fun parseVmess(link: String): VpnProfile? {
        val json = decodeBase64Flexible(link.substringAfter("vmess://", "")) ?: return null
        val obj = JSONObject(json)
        val host = obj.optString("add").trim()
        val id = obj.optString("id").trim()
        if (host.isBlank() || id.isBlank()) return null

        return VpnProfile(
            name = obj.optString("ps", "VMess Server"),
            serverIp = host,
            port = obj.optInt("port", 443),
            secretKey = id,
            protocol = "VMess",
            sni = obj.optString("sni", obj.optString("host", "")),
            alpn = obj.optString("alpn", "")
        )
    }

    private fun parseTrojan(link: String): VpnProfile {
        val uri = URI(link)
        val password = uri.rawUserInfo?.let(::decodeComponent)
            ?: throw IllegalArgumentException("Trojan password is missing")
        val host = uri.host ?: throw IllegalArgumentException("Trojan host is missing")
        val params = parseQuery(uri.rawQuery)

        return VpnProfile(
            name = decodeComponent(uri.rawFragment).ifBlank { "Trojan Server" },
            serverIp = host,
            port = if (uri.port > 0) uri.port else 443,
            secretKey = password,
            protocol = "Trojan",
            sni = params["sni"] ?: params["peer"] ?: params["host"] ?: host,
            fp = params["fp"] ?: "chrome",
            alpn = params["alpn"] ?: ""
        )
    }

    private fun parseShadowsocks(link: String): VpnProfile? {
        val fragmentIndex = link.indexOf('#')
        val withoutFragment = if (fragmentIndex >= 0) link.substring(0, fragmentIndex) else link
        val name = if (fragmentIndex >= 0) {
            decodeComponent(link.substring(fragmentIndex + 1)).ifBlank { "Shadowsocks Server" }
        } else "Shadowsocks Server"

        val payload = withoutFragment.substringAfter("ss://", "")
        if (payload.isBlank()) return null

        val at = payload.lastIndexOf('@')
        if (at > 0) {
            val credentials = (
                decodeBase64Flexible(payload.substring(0, at))
                    ?: decodeComponent(payload.substring(0, at))
            ).split(":", limit = 2)
            if (credentials.size != 2) return null
            val (host, port) = parseHostPort(payload.substring(at + 1), 8388)
            return VpnProfile(
                name = name,
                serverIp = host,
                port = port,
                secretKey = credentials[0] + ":" + credentials[1],
                protocol = "ShadowSocks"
            )
        }

        val decoded = decodeBase64Flexible(payload) ?: return null
        val endpointAt = decoded.lastIndexOf('@')
        if (endpointAt <= 0) return null
        val credentials = decoded.substring(0, endpointAt).split(":", limit = 2)
        if (credentials.size != 2) return null
        val (host, port) = parseHostPort(decoded.substring(endpointAt + 1), 8388)

        return VpnProfile(
            name = name,
            serverIp = host,
            port = port,
            secretKey = credentials[0] + ":" + credentials[1],
            protocol = "ShadowSocks"
        )
    }

    private fun parseRaw(link: String): VpnProfile? {
        val value = link.substringBefore('#').substringBefore('?').trim()
        if (value.isBlank()) return null
        val (host, port) = try {
            parseHostPort(value, 443)
        } catch (_: Exception) {
            return null
        }
        if (host.isBlank()) return null

        // host:port does not contain a VLESS identity/Reality key.
        // Keep it disabled instead of inventing credentials.
        return VpnProfile(
            name = "Manual Server",
            serverIp = host,
            port = port,
            secretKey = "",
            protocol = "VLESS (Reality)",
            isActive = false
        )
    }

    private fun parseHostPort(endpoint: String, defaultPort: Int): Pair<String, Int> {
        val value = decodeComponent(endpoint.trim())
        if (value.startsWith("[")) {
            val closing = value.indexOf(']')
            if (closing <= 0) throw IllegalArgumentException("Invalid IPv6 endpoint")
            val host = value.substring(1, closing)
            val port = if (value.length > closing + 1 && value[closing + 1] == ':') {
                value.substring(closing + 2).toIntOrNull() ?: defaultPort
            } else defaultPort
            return host to port
        }

        val colonCount = value.count { it == ':' }
        if (colonCount == 1) {
            val separator = value.lastIndexOf(':')
            return value.substring(0, separator) to
                (value.substring(separator + 1).toIntOrNull() ?: defaultPort)
        }

        return value to defaultPort
    }

    private fun parseQuery(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrBlank()) return emptyMap()
        return rawQuery.split('&').asSequence().mapNotNull { item ->
            val parts = item.split('=', limit = 2)
            if (parts.size != 2) null
            else decodeComponent(parts[0]).lowercase() to decodeComponent(parts[1])
        }.toMap()
    }

    private fun decodeComponent(value: String): String =
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())

    private fun decodeBase64Flexible(value: String): String? {
        val normalized = value.trim().replace('-', '+').replace('_', '/')
        val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
        val flags = listOf(Base64.DEFAULT, Base64.NO_WRAP, Base64.URL_SAFE or Base64.NO_WRAP)
        for (flag in flags) {
            try {
                return String(Base64.decode(padded, flag), StandardCharsets.UTF_8)
            } catch (_: IllegalArgumentException) {
            }
        }
        return null
    }
}

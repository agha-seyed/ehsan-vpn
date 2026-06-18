package com.example.utils

import org.json.JSONArray
import org.json.JSONObject

object XrayConfigGenerator {

    /**
     * Converts a VpnProfile into a full Xray JSON config.
     */
    fun generateConfig(profile: com.example.data.VpnProfile): String {
        return when {
            profile.protocol.contains("VLESS", ignoreCase = true) -> generateRealityConfig(
                serverIp = profile.serverIp,
                port = profile.port,
                uuid = profile.secretKey,
                pbk = profile.pbk,
                sni = profile.sni,
                sid = profile.sid,
                fp = profile.fp,
                flow = profile.flow,
                alpn = profile.alpn
            )
            profile.protocol.contains("Trojan", ignoreCase = true) -> generateTrojanConfig(profile)
            profile.protocol.contains("ShadowSocks", ignoreCase = true) -> generateShadowsocksConfig(profile)
            else -> generateRealityConfig(
                serverIp = profile.serverIp,
                port = profile.port,
                uuid = profile.secretKey,
                pbk = profile.pbk,
                sni = profile.sni,
                sid = profile.sid,
                fp = profile.fp,
                flow = profile.flow,
                alpn = profile.alpn
            )
        }
    }

    private fun generateTrojanConfig(profile: com.example.data.VpnProfile): String {
        val config = createBaseConfig()
        val outbounds = config.getJSONArray("outbounds")
        
        val trojanOutbound = JSONObject().apply {
            put("protocol", "trojan")
            put("tag", "proxy")
            val settings = JSONObject().apply {
                val servers = JSONArray()
                val server = JSONObject().apply {
                    put("address", profile.serverIp)
                    put("port", profile.port)
                    put("password", profile.secretKey)
                }
                servers.put(server)
                put("servers", servers)
            }
            put("settings", settings)
            
            val streamSettings = JSONObject().apply {
                put("network", "tcp")
                put("security", "tls")
                val tlsSettings = JSONObject().apply {
                    put("serverName", profile.sni)
                }
                put("tlsSettings", tlsSettings)
            }
            put("streamSettings", streamSettings)
        }
        
        outbounds.put(0, trojanOutbound)
        return config.toString(4)
    }

    private fun generateShadowsocksConfig(profile: com.example.data.VpnProfile): String {
        val config = createBaseConfig()
        val outbounds = config.getJSONArray("outbounds")
        
        val ssOutbound = JSONObject().apply {
            put("protocol", "shadowsocks")
            put("tag", "proxy")
            val settings = JSONObject().apply {
                val servers = JSONArray()
                val server = JSONObject().apply {
                    put("address", profile.serverIp)
                    put("port", profile.port)
                    // method:password format
                    val parts = profile.secretKey.split(":", limit = 2)
                    put("method", if (parts.size == 2) parts[0] else "aes-256-gcm")
                    put("password", if (parts.size == 2) parts[1] else profile.secretKey)
                }
                servers.put(server)
                put("servers", servers)
            }
            put("settings", settings)
        }
        
        outbounds.put(0, ssOutbound)
        return config.toString(4)
    }

    private fun createBaseConfig(): JSONObject {
        val config = JSONObject()
        config.put("log", JSONObject().apply { put("loglevel", "warning") })
        
        val inbounds = JSONArray()
        inbounds.put(JSONObject().apply {
            put("port", 10808)
            put("listen", "127.0.0.1")
            put("protocol", "socks")
            put("settings", JSONObject().apply {
                put("auth", "noauth")
                put("udp", true)
            })
        })
        config.put("inbounds", inbounds)
        
        val outbounds = JSONArray()
        outbounds.put(JSONObject()) // Placeholder for proxy
        outbounds.put(JSONObject().apply {
            put("protocol", "freedom")
            put("tag", "direct")
        })
        config.put("outbounds", outbounds)
        
        return config
    }

    fun generateRealityConfig(
        serverIp: String,
        port: Int,
        uuid: String,
        pbk: String,
        sni: String,
        sid: String,
        fp: String = "chrome",
        flow: String = "xtls-rprx-vision",
        alpn: String = ""
    ): String {
        
        // Root config
        val config = JSONObject()
        
        // Log
        val log = JSONObject().apply {
            put("loglevel", "warning")
        }
        config.put("log", log)

        // Inbounds (SOCKS5 proxy - TUN interface will route traffic here via tun2socks or core tun)
        val inbounds = JSONArray()
        val socksInbound = JSONObject().apply {
            put("port", 10808)
            put("listen", "127.0.0.1")
            put("protocol", "socks")
            val settings = JSONObject().apply {
                put("auth", "noauth")
                put("udp", true)
                put("ip", "127.0.0.1")
            }
            put("settings", settings)
        }
        inbounds.put(socksInbound)
        config.put("inbounds", inbounds)

        // Outbounds (VLESS Reality)
        val outbounds = JSONArray()
        val vlessOutbound = JSONObject().apply {
            put("protocol", "vless")
            put("tag", "proxy")
            
            val settings = JSONObject().apply {
                val vnext = JSONArray()
                val server = JSONObject().apply {
                    put("address", serverIp)
                    put("port", port)
                    val users = JSONArray()
                    val user = JSONObject().apply {
                        put("id", uuid)
                        put("encryption", "none")
                        put("flow", flow)
                    }
                    users.put(user)
                    put("users", users)
                }
                vnext.put(server)
                put("vnext", vnext)
            }
            put("settings", settings)

            val streamSettings = JSONObject().apply {
                put("network", "tcp")
                put("security", "reality")
                val realitySettings = JSONObject().apply {
                    put("show", false)
                    put("fingerprint", fp)
                    put("serverName", sni)
                    put("publicKey", pbk)
                    put("shortId", sid)
                    put("spiderX", "/")
                }
                put("realitySettings", realitySettings)
                
                if (alpn.isNotEmpty()) {
                    val alpnArray = JSONArray()
                    alpn.split(",").forEach { alpnArray.put(it) }
                    put("alpn", alpnArray)
                }
            }
            put("streamSettings", streamSettings)
        }
        outbounds.put(vlessOutbound)

        // Direct Outbound for bypassed traffic
        val directOutbound = JSONObject().apply {
            put("protocol", "freedom")
            put("tag", "direct")
        }
        outbounds.put(directOutbound)
        config.put("outbounds", outbounds)

        return config.toString(4) // Beautifully formatted JSON
    }
}

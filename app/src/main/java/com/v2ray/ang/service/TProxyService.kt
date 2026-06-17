package com.v2ray.ang.service

import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File

/**
 * Manages the tun2socks process that handles VPN traffic.
 * This class MUST be in the com.v2ray.ang.service package so the JNI mapping works with the precompiled libhev-socks5-tunnel.so.
 */
class TProxyService(
    private val context: Context,
    private val vpnInterface: ParcelFileDescriptor
) {
    companion object {
        @JvmStatic
        @Suppress("FunctionName")
        private external fun TProxyStartService(configPath: String, fd: Int)

        @JvmStatic
        @Suppress("FunctionName")
        private external fun TProxyStopService()

        @JvmStatic
        @Suppress("FunctionName")
        external fun TProxyGetStats(): LongArray?

        init {
            try {
                System.loadLibrary("hev-socks5-tunnel")
                Log.i("TProxyService", "Successfully loaded hev-socks5-tunnel JNI library!")
            } catch (e: Exception) {
                Log.e("TProxyService", "Failed to load hev-socks5-tunnel. Ensure .so files are in jniLibs.", e)
            }
        }
    }

    /**
     * Starts the tun2socks process with the appropriate parameters.
     */
    fun startTun2Socks(socksPort: Int = 10808) {
        val configContent = buildConfig(socksPort)
        val configFile = File(context.filesDir, "hev-socks5-tunnel.yaml").apply {
            writeText(configContent)
        }

        try {
            Log.i("TProxyService", "Starting TProxyStartService with FD: ${vpnInterface.fd}")
            TProxyStartService(configFile.absolutePath, vpnInterface.fd)
        } catch (e: Exception) {
            Log.e("TProxyService", "HevSocks5Tunnel start exception: ${e.message}")
        }
    }

    private fun buildConfig(socksPort: Int): String {
        return buildString {
            appendLine("tunnel:")
            appendLine("  mtu: 1500")
            appendLine("  ipv4: 10.0.0.2") // Match the builder.addAddress
            appendLine("socks5:")
            appendLine("  port: $socksPort")
            appendLine("  address: 127.0.0.1")
            appendLine("  udp: 'udp'")
            appendLine("misc:")
            appendLine("  tcp-read-write-timeout: 300000")
            appendLine("  udp-read-write-timeout: 60000")
            appendLine("  log-level: warn")
        }
    }

    /**
     * Stops the tun2socks process
     */
    fun stopTun2Socks() {
        try {
            Log.i("TProxyService", "Stopping TProxyStopService...")
            TProxyStopService()
        } catch (e: Exception) {
            Log.e("TProxyService", "Failed to stop hev-socks5-tunnel", e)
        }
    }
}

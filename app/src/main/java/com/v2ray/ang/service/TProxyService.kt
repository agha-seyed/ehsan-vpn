package com.v2ray.ang.service

import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File

/**
 * Manages the tun2socks process that handles VPN traffic.
 * This class MUST be in the com.v2ray.ang.service package so the JNI mapping works
 * with the precompiled libhev-socks5-tunnel.so.
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

        @Volatile
        private var libraryLoaded = false

        init {
            try {
                System.loadLibrary("hev-socks5-tunnel")
                libraryLoaded = true
                Log.i("TProxyService", "Successfully loaded hev-socks5-tunnel JNI library!")
            } catch (e: UnsatisfiedLinkError) {
                Log.e("TProxyService", "Failed to load hev-socks5-tunnel. Ensure the native .so files are packaged.", e)
            } catch (e: SecurityException) {
                Log.e("TProxyService", "Native library loading was denied.", e)
            }
        }
    }

    @Volatile
    private var started = false

    /**
     * Starts the tun2socks process.
     *
     * Returns false when the native library is unavailable or JNI startup throws.
     * The caller must not report the VPN as CONNECTED when this returns false.
     */
    fun startTun2Socks(socksPort: Int = 10808): Boolean {
        if (!libraryLoaded) {
            Log.e("TProxyService", "Cannot start tun2socks: native library is not loaded.")
            return false
        }

        if (started) {
            Log.w("TProxyService", "tun2socks is already started; stopping it before restart.")
            stopTun2Socks()
        }

        return try {
            val configContent = buildConfig(socksPort)
            val configFile = File(context.filesDir, "hev-socks5-tunnel.yaml").apply {
                writeText(configContent)
            }

            Log.i("TProxyService", "Starting TProxyStartService with FD: " + vpnInterface.fd)
            TProxyStartService(configFile.absolutePath, vpnInterface.fd)
            started = true
            true
        } catch (e: Exception) {
            Log.e("TProxyService", "HevSocks5Tunnel start exception.", e)
            started = false
            false
        } catch (e: UnsatisfiedLinkError) {
            Log.e("TProxyService", "HevSocks5Tunnel JNI symbol is unavailable.", e)
            started = false
            false
        }
    }

    private fun buildConfig(socksPort: Int): String {
        require(socksPort in 1..65535) { "Invalid SOCKS5 port: " + socksPort }

        return buildString {
            appendLine("tunnel:")
            appendLine("  mtu: 1500")
            appendLine("  ipv4: 10.0.0.2")
            appendLine("socks5:")
            appendLine("  port: " + socksPort)
            appendLine("  address: 127.0.0.1")
            appendLine("  udp: 'udp'")
            appendLine("misc:")
            appendLine("  tcp-read-write-timeout: 300000")
            appendLine("  udp-read-write-timeout: 60000")
            appendLine("  log-level: warn")
        }
    }

    /**
     * Stops the tun2socks process if this instance started it.
     */
    fun stopTun2Socks() {
        if (!started) return

        try {
            Log.i("TProxyService", "Stopping TProxyStopService...")
            TProxyStopService()
        } catch (e: Exception) {
            Log.e("TProxyService", "Failed to stop hev-socks5-tunnel.", e)
        } catch (e: UnsatisfiedLinkError) {
            Log.e("TProxyService", "HevSocks5Tunnel JNI stop symbol is unavailable.", e)
        } finally {
            started = false
        }
    }

    fun isStarted(): Boolean = started
}

package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [VpnProfile::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vpnProfileDao(): VpnProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "horizon_vpn_database"
                )
                .addCallback(AppDatabaseCallback(scope))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val dao = database.vpnProfileDao()
                    // Pre-populate with beautiful, functional server templates
                    dao.insertProfile(
                        VpnProfile(
                            name = "⚡ آلمان - VLESS Reality (پر سرعت)",
                            serverIp = "185.228.136.42",
                            port = 443,
                            secretKey = "vless://ae72c918-bb20-4357-be24-5cc0df5988a2@185.228.136.42:443?security=reality&sni=www.google.com&flow=xtls-rprx-vision",
                            protocol = "VLESS (Reality)",
                            sni = "www.google.com",
                            latencyMs = 84,
                            isActive = true
                        )
                    )
                    dao.insertProfile(
                        VpnProfile(
                            name = "🛡️ فنلاند - Trojan Stealth Filter",
                            serverIp = "95.216.14.88",
                            port = 8443,
                            secretKey = "trojan://superstealthsecret6a892b@95.216.14.88:8443?sni=www.wikipedia.org",
                            protocol = "Trojan",
                            sni = "www.wikipedia.org",
                            latencyMs = 112,
                            isActive = false
                        )
                    )
                    dao.insertProfile(
                        VpnProfile(
                            name = "🚀 سرور مجازی من (جک صدمتن - قابل ویرایش)",
                            serverIp = "192.168.1.1",
                            port = 443,
                            secretKey = "vless://your-vps-uuid-goes-here@your_server_ip:443?security=reality&sni=www.google.com",
                            protocol = "VLESS (Reality)",
                            sni = "www.google.com",
                            latencyMs = -1,
                            isActive = false
                        )
                    )
                }
            }
        }
    }
}

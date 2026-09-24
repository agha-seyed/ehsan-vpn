package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [VpnProfile::class], version = 4, exportSchema = false)
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
                // Version 3 had the same schema; keep existing profiles intact.
                .addMigrations(MIGRATION_3_4)
                // Very old development databases (v1/v2) can be recreated explicitly.
                .fallbackToDestructiveMigrationFrom(1, 2)
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
                            name = "⚙️ نمونه VLESS Reality (غیرفعال)",
                            serverIp = "example.com",
                            port = 443,
                            secretKey = "",
                            protocol = "VLESS (Reality)",
                            sni = "example.com",
                            latencyMs = -1,
                            isActive = false
                        )
                    )
                    dao.insertProfile(
                        VpnProfile(
                            name = "⚙️ نمونه Trojan (غیرفعال)",
                            serverIp = "example.com",
                            port = 443,
                            secretKey = "",
                            protocol = "Trojan",
                            sni = "example.com",
                            latencyMs = -1,
                            isActive = false
                        )
                    )
                    dao.insertProfile(
                        VpnProfile(
                            name = "⚙️ سرور شخصی من (برای ویرایش)",
                            serverIp = "example.com",
                            port = 443,
                            secretKey = "",
                            protocol = "VLESS (Reality)",
                            sni = "example.com",
                            latencyMs = -1,
                            isActive = false
                        )
                    )
                }
            }
        }
    }
}

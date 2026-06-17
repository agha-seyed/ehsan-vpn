package com.example.utils

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("vpn_settings", Context.MODE_PRIVATE)

    var isAmoledMode: Boolean
        get() = prefs.getBoolean("AMOLED_MODE", false)
        set(value) = prefs.edit().putBoolean("AMOLED_MODE", value).apply()

    var appLanguage: String
        get() = prefs.getString("APP_LANGUAGE", "fa") ?: "fa"
        set(value) = prefs.edit().putString("APP_LANGUAGE", value).apply()

    var isSplitTunnelingEnabled: Boolean
        get() = prefs.getBoolean("SPLIT_TUNNELING", false)
        set(value) = prefs.edit().putBoolean("SPLIT_TUNNELING", value).apply()

    fun saveBypassedApps(apps: List<String>) {
        prefs.edit().putStringSet("BYPASSED_APPS", apps.toSet()).apply()
    }

    fun getBypassedApps(): Set<String> {
        return prefs.getStringSet("BYPASSED_APPS", emptySet()) ?: emptySet()
    }
}

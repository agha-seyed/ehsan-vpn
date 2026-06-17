package com.example.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Helper for managing SharedPreferences with type-safe access.
 * Handles persistence of app settings like language, theme, etc.
 */
class SharedPreferencesHelper(context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences(
        "ehsan_vpn_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_AMOLED_MODE = "amoled_mode"
        private const val KEY_SPLIT_TUNNELING = "split_tunneling_enabled"
        private const val KEY_AUTO_CONNECT = "auto_connect"
    }

    var language: String
        get() = preferences.getString(KEY_LANGUAGE, "fa") ?: "fa"
        set(value) = preferences.edit().putString(KEY_LANGUAGE, value).apply()

    var isAmoledMode: Boolean
        get() = preferences.getBoolean(KEY_AMOLED_MODE, false)
        set(value) = preferences.edit().putBoolean(KEY_AMOLED_MODE, value).apply()

    var isSplitTunnelingEnabled: Boolean
        get() = preferences.getBoolean(KEY_SPLIT_TUNNELING, false)
        set(value) = preferences.edit().putBoolean(KEY_SPLIT_TUNNELING, value).apply()

    var isAutoConnectEnabled: Boolean
        get() = preferences.getBoolean(KEY_AUTO_CONNECT, false)
        set(value) = preferences.edit().putBoolean(KEY_AUTO_CONNECT, value).apply()

    fun clear() {
        preferences.edit().clear().apply()
    }
}

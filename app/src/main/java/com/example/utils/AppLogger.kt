package com.example.utils

import timber.log.Timber

/**
 * Centralized logging utility using Timber.
 * Provides consistent logging across the application.
 */
object AppLogger {
    
    fun init() {
        if (!Timber.forest().any { it is Timber.DebugTree }) {
            Timber.plant(Timber.DebugTree())
        }
    }

    fun d(message: String) = Timber.d(message)
    fun i(message: String) = Timber.i(message)
    fun w(message: String) = Timber.w(message)
    fun e(message: String, exception: Throwable? = null) {
        if (exception != null) Timber.e(exception, message) else Timber.e(message)
    }
}

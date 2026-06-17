package com.example.utils

import android.util.Log
import timber.log.Timber

/**
 * Centralized logging utility using Timber.
 * Provides debug, info, warning, and error logging with consistent formatting.
 */
object AppLogger {
    
    fun init() {
        Timber.plant(TimberDebugTree())
    }

    fun d(message: String) {
        Timber.d(message)
    }

    fun d(tag: String, message: String) {
        Timber.tag(tag).d(message)
    }

    fun i(message: String) {
        Timber.i(message)
    }

    fun i(tag: String, message: String) {
        Timber.tag(tag).i(message)
    }

    fun w(message: String) {
        Timber.w(message)
    }

    fun w(tag: String, message: String, exception: Throwable? = null) {
        if (exception != null) {
            Timber.tag(tag).w(exception, message)
        } else {
            Timber.tag(tag).w(message)
        }
    }

    fun e(message: String, exception: Throwable? = null) {
        if (exception != null) {
            Timber.e(exception, message)
        } else {
            Timber.e(message)
        }
    }

    fun e(tag: String, message: String, exception: Throwable? = null) {
        if (exception != null) {
            Timber.tag(tag).e(exception, message)
        } else {
            Timber.tag(tag).e(message)
        }
    }
}

/**
 * Custom Timber debug tree for formatted logging
 */
private class TimberDebugTree : Timber.DebugTree() {
    override fun createStackElementTag(element: StackTraceElement): String? {
        return String.format(
            "[%s:%s(%s)]",
            element.className.substringAfterLast("."),
            element.methodName,
            element.lineNumber
        )
    }
}

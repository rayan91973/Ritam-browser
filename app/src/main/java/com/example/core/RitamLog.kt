package com.example.core

import android.util.Log
import com.example.BuildConfig

/**
 * Single logging facade for the app.
 *
 * Debug builds log everything; release builds keep only warnings/errors. Feature code should use
 * this instead of raw [Log] calls so that a message can never leak browsing data into logcat on a
 * shipped build, and so that failure handling is consistent across modules.
 */
object RitamLog {

    /** Android truncates tags longer than 23 characters, so keep them valid up front. */
    private fun tag(raw: String): String = if (raw.length <= 23) raw else raw.substring(0, 23)

    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag(tag), message)
    }

    fun w(tag: String, message: String, error: Throwable? = null) {
        if (error != null) Log.w(tag(tag), message, error) else Log.w(tag(tag), message)
    }

    fun e(tag: String, message: String, error: Throwable? = null) {
        if (error != null) Log.e(tag(tag), message, error) else Log.e(tag(tag), message)
    }

    /** Runs [block], returning null (after logging) instead of letting a failure escape a coroutine. */
    inline fun <T> guarded(tag: String, action: String, block: () -> T): T? =
        try {
            block()
        } catch (error: Throwable) {
            e(tag, "$action failed", error)
            null
        }
}

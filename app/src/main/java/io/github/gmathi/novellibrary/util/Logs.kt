package io.github.gmathi.novellibrary.util

import android.util.Log
import com.crashlytics.android.Crashlytics
import io.github.gmathi.novellibrary.BuildConfig
import java.util.*

object Logs {

    fun debug(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    fun info(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message)
        }
    }

    fun warning(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, message)
        } else {
            Crashlytics.log(String.format(Locale.getDefault(), "Priority: %d; %s : %s", Log.WARN, tag, message))
        }
    }

    fun warning(tag: String, message: String, throwable: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, message, throwable)
        } else {
            Crashlytics.log(String.format(Locale.getDefault(), "Priority: %d; %s : %s", Log.WARN, tag, message))
            Crashlytics.logException(throwable)
        }
    }

    fun error(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message)
        } else {
            Crashlytics.log(String.format(Locale.getDefault(), "Priority: %d; %s : %s", Log.ERROR, tag, message))
        }
    }

    fun error(tag: String, message: String, throwable: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message, throwable)
        } else {
            Crashlytics.log(String.format(Locale.getDefault(), "Priority: %d; %s : %s", Log.ERROR, tag, message))
            Crashlytics.logException(throwable)
        }
    }

}
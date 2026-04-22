package io.github.gmathi.novellibrary.util.system

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable

/**
 * Version-safe wrapper around [Intent.getParcelableExtra].
 * Uses the type-safe overload on API 33+ and falls back to the deprecated one below that.
 */
inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key) as? T
    }
}

/**
 * Version-safe wrapper around [Bundle.getParcelable].
 * Uses the type-safe overload on API 33+ and falls back to the deprecated one below that.
 */
inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelable(key) as? T
    }
}

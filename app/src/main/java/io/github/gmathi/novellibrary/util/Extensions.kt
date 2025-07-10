package io.github.gmathi.novellibrary.util

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.model.other.LinkedPage

// General extensions that don't fit some specific extension category.

/**
 * Shows a toast message on the main thread
 */
fun Context.showToastWithMain(text: String, duration: Int = Toast.LENGTH_SHORT) {
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(this, text, duration).show()
    }
}

/**
 * Shows a toast message
 */
fun Context.showToast(text: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text, duration).show()
}

/**
 * Shows a toast message for activities
 */
fun Activity.showToast(message: String, length: Int = Toast.LENGTH_LONG) {
    Toast.makeText(this, message, length).show()
}

/**
 * Converts decimal to percentage (0.5 -> 50.0)
 */
fun Float.toHumanPercentage(): Float = this * 100.0f

/**
 * Converts percentage to decimal (50.0 -> 0.5)
 */
fun Float.fromHumanPercentage(): Float = this / 100.0f

/**
 * Converts decimal to percentage (0.5 -> 50.0)
 */
fun Double.toHumanPercentage(): Double = this * 100.0

/**
 * Converts percentage to decimal (50.0 -> 0.5)
 */
fun Double.fromHumanPercentage(): Double = this / 100.0

fun WebPageSettings.getLinkedPagesCompat(): ArrayList<LinkedPage> {
    val str = metadata.get(Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES) ?: "[]"
    return if (str.startsWith("[{")) {
        Gson().fromJson(str)
    } else {
        // Old list of strings format
        val list = ArrayList<LinkedPage>()
        Gson().fromJson<ArrayList<String>>(str).mapIndexedTo(list) { idx, s -> LinkedPage(s, "legacy $idx", false) }
    }
}
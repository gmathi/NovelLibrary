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
import org.checkerframework.checker.units.qual.Length

// General extensions that don't fit some specific extension category.

fun Context.showToastWithMain(text: String, duration: Int) {
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(this, text, duration).show()
    }
}

// Small helper to converted to/from percentage.
fun Float.toHumanPercentage():Float = (this * 100.0f)
fun Float.fromHumanPercentage():Float = (this / 100.0f)
fun Double.toHumanPercentage():Double = (this * 100.0)
fun Double.fromHumanPercentage():Double = (this / 100.0)

fun WebPageSettings.getLinkedPagesCompat():ArrayList<LinkedPage> {
    val str = metadata.get(Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES)?:"[]"
    return if (str.startsWith("[{")) {
        Gson().fromJson(str)
    } else {
        // Old list of strings format
        val list = ArrayList<LinkedPage>()
        Gson().fromJson<ArrayList<String>>(str).mapIndexedTo(list) { idx, s -> LinkedPage(s, "legacy $idx", false) }
    }
}

fun Activity.showToast(message: String, length: Int = Toast.LENGTH_LONG) {
    Toast.makeText(this, message, length).show()
}
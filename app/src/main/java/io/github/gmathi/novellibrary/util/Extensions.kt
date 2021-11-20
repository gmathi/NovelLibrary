package io.github.gmathi.novellibrary.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

// General extensions that don't fit some specific extension category.
fun Context.showToastWithMain(text: String, duration: Int) {
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(this, text, duration).show()
    }
}
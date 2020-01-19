package io.github.gmathi.novellibrary.extensions

import android.view.View
import android.view.ViewGroup


fun ViewGroup.enabled(enable: Boolean) {
    android.util.Log.i("MyViews", id.toString())
    isEnabled = enable
    for (i in 0 until childCount) {
        val child: View = getChildAt(i)
        if (child is ViewGroup)
            child.enabled(enable)
        else
            child.isEnabled = enable
    }
}
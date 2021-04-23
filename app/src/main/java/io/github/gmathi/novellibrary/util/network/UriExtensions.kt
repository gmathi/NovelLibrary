package io.github.gmathi.novellibrary.util.network

import android.net.Uri
import io.github.gmathi.novellibrary.util.lang.writableFileName

fun Uri.getFileName(): String {
    return ((this.lastPathSegment
        ?: "") + this.toString().substringAfter("?", "")).writableFileName()
}
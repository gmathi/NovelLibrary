package io.github.gmathi.novellibrary.util.network

import android.net.Uri
import io.github.gmathi.novellibrary.util.lang.writableFileName
import java.util.UUID

fun Uri.getFileName(): String {
    return "${UUID.randomUUID()}-$lastPathSegment".writableFileName()
}
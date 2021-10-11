package io.github.gmathi.novellibrary.util.network

import android.net.Uri
import io.github.gmathi.novellibrary.util.lang.writableFileName
import java.net.URI
import java.net.URISyntaxException
import java.util.*

fun Uri.getFileName(): String {
    return "${UUID.randomUUID()}-$lastPathSegment".writableFileName()
}

@Throws(URISyntaxException::class)
fun URI.replaceHostInUrl(
    newAuthority: String
): String = URI(scheme.lowercase(Locale.US), newAuthority, path, query, fragment).toString()

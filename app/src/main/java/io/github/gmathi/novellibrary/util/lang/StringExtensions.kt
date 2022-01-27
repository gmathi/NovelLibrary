package io.github.gmathi.novellibrary.util.lang

import android.webkit.CookieManager
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.model.preference.DataCenter
import uy.kohesive.injekt.injectLazy
import java.net.URL
import kotlin.math.floor

/**
 * Replaces the given string to have at most [count] characters using [replacement] at its end.
 * If [replacement] is longer than [count] an exception will be thrown when `length > count`.
 */
fun String.chop(count: Int, replacement: String = "..."): String {
    return if (length > count) {
        take(count - replacement.length) + replacement
    } else {
        this
    }
}

/**
 * Replaces the given string to have at most [count] characters using [replacement] near the center.
 * If [replacement] is longer than [count] an exception will be thrown when `length > count`.
 */
fun String.truncateCenter(count: Int, replacement: String = "..."): String {
    if (length <= count) {
        return this
    }

    val pieceLength: Int = floor((count - replacement.length).div(2.0)).toInt()

    return "${take(pieceLength)}$replacement${takeLast(pieceLength)}"
}


/**
 * Returns the size of the string as the number of bytes.
 */
fun String.byteSize(): Int {
    return toByteArray(Charsets.UTF_8).size
}

/**
 * Returns a string containing the first [n] bytes from this string, or the entire string if this
 * string is shorter.
 */
fun String.takeBytes(n: Int): String {
    val bytes = toByteArray(Charsets.UTF_8)
    return if (bytes.size <= n) {
        this
    } else {
        bytes.decodeToString(endIndex = n).replace("\uFFFD", "")
    }
}


fun String.addToNovelSearchHistory() {
    val dataCenter: DataCenter by injectLazy()
    val list = dataCenter.loadNovelSearchHistory()
    if (!list.contains(this)) {
        list.add(0, this)
        dataCenter.saveNovelSearchHistory(list)
    }
}

fun String.addToLibrarySearchHistory() {
    val dataCenter: DataCenter by injectLazy()
    val list = dataCenter.loadLibrarySearchHistory()
    if (!list.contains(this)) {
        list.add(0, this)
        dataCenter.saveLibrarySearchHistory(list)
    }
}

fun String.writableFileName(): String {
    val regex = Regex("[^a-zA-Z0-9.-]")
    var fileName = this.replace(regex, "-")
    if (fileName.length > 150) {
        fileName = fileName.replace("-", "")
        val subStringLength = if (fileName.length < 150) fileName.length else 150
        fileName = fileName.substring(0, subStringLength)
    }
    return fileName
}

fun String.writableOldFileName(): String {
    var fileName = this.replace(Regex.fromLiteral("[^a-zA-Z0-9.-]"), "_").replace("/", "_").replace(" ", "")
    if (fileName.length > 150)
        fileName = fileName.substring(0, 150)
    return fileName
}

fun String.getGlideUrl(): GlideUrl {
    val dataCenter: DataCenter by injectLazy()
    val url = URL(this)
    val hostName = url.host.replace("www.", "").replace("m.", "").trim()
    val builder = LazyHeaders.Builder()
        .addHeader("User-Agent", HostNames.USER_AGENT)
        .addHeader("Cookie", CookieManager.getInstance().getCookie(this) ?: CookieManager.getInstance().getCookie(".$hostName") ?: "")

    return GlideUrl(this, builder.build())
}

private fun String?.contains(chapter: String?): Boolean {
    return (this != null) && (chapter != null) && this.contains(chapter)
}

fun String.addPageNumberToUrl(pageNumber: Int, pageNumberExtension: String): String {
    val url = URL(this)
    return if (!url.query.isNullOrBlank()) {
        "$this&$pageNumberExtension=$pageNumber"
    } else {
        "$this?$pageNumberExtension=$pageNumber"
    }

}



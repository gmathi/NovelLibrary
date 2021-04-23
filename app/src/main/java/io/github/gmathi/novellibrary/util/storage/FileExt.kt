package io.github.gmathi.novellibrary.util.storage

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import io.github.gmathi.novellibrary.BuildConfig
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Returns the uri of a file
 *
 * @param context context of application
 */
fun File.getUriCompat(context: Context): Uri {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", this)
    } else {
        this.toUri()
    }
}


@Throws(IOException::class)
fun File.createFileIfNotExists() {
    val parent = parentFile
    if (parent == null || (!parent.exists() && !parent.mkdirs()))
        throw FileNotFoundException("Failed to ensure directory: $name")

    if (!exists())
        createNewFile()
}

@Throws(IOException::class)
fun File.createDirsIfNotExists() {
    val parent = parentFile
    if (parent == null || (!parent.exists() && !parent.mkdirs()))
        throw FileNotFoundException("Failed to ensure directory: $name")

    if (!exists())
        mkdirs()
}

fun File.getReadableSize(): String {
    if (!exists()) return "N/A"
    val sizes = arrayOf("B", "KB", "MB", "GB", "TB")

    var size = length().toDouble()
    for (i in sizes.indices) {
        val newSize = size / 1024
        if (newSize < 0) {
            return "%.${size}f ${sizes[i]}"
        } else size = newSize
    }

    return "%.${size}f EB"
}
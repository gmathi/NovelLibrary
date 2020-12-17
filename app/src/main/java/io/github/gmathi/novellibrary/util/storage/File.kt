package io.github.gmathi.novellibrary.util.storage

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException


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
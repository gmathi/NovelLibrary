package io.github.gmathi.novellibrary.extensions

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException


@Throws(IOException::class)
fun File.createIfNotExists() {
    val parent = parentFile
    if (parent == null || (!parent.exists() && !parent.mkdirs()))
        throw FileNotFoundException("Failed to ensure directory: $name")

    if (!exists())
        createNewFile()
}


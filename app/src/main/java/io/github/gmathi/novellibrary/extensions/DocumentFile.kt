package io.github.gmathi.novellibrary.extensions

import androidx.documentfile.provider.DocumentFile

fun DocumentFile.getOrCreateDirectory(displayName: String): DocumentFile? {
    return (findFile(displayName)
            ?: createDirectory(displayName))
}

fun DocumentFile.getOrCreateFile(displayName: String): DocumentFile? {
    return getOrCreateFile("*/*", displayName)
}

fun DocumentFile.getOrCreateFile(mimeType: String, displayName: String): DocumentFile? {
    return (findFile(displayName)
            ?: createFile(mimeType, displayName))
}

fun DocumentFile?.notNullAndExists(): Boolean =
    this?.exists() ?: false
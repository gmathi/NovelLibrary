package io.github.gmathi.novellibrary.extension.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import io.github.gmathi.novellibrary.extension.model.Extension

fun Extension.getApplicationIcon(context: Context): Drawable? {
    return try {
        context.packageManager.getApplicationIcon(pkgName)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}

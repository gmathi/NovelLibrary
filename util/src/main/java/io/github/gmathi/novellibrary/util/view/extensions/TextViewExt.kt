package io.github.gmathi.novellibrary.util.view.extensions

import android.content.res.AssetManager
import android.graphics.Typeface
import android.widget.TextView

fun TextView.applyFont(assetManager: AssetManager?): TextView {
    assetManager?.let {
        typeface = Typeface.createFromAsset(it, "fonts/source_sans_pro_regular.ttf")
    }
    return this
}

fun TextView.setTypeface(style: Int): TextView {
    setTypeface(typeface, style)
    return this
}
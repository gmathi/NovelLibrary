package io.github.gmathi.novellibrary.common.extensions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes

/**
 * Inflates a layout resource into a View.
 * @param layout the layout resource to inflate.
 * @param attachToRoot whether to attach the view to the root or not. Defaults to false.
 */
fun ViewGroup.inflate(@LayoutRes layout: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layout, this, attachToRoot)
}

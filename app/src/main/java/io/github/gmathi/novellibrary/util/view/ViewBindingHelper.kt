package io.github.gmathi.novellibrary.util.view

import android.app.Activity
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

object ViewBindingHelper {
    inline fun <T : ViewBinding> Fragment.withViewBinding(
        crossinline bindingFactory: (View) -> T
    ): Lazy<T> = lazy {
        bindingFactory(requireView())
    }
    
    inline fun <T : ViewBinding> Activity.withViewBinding(
        crossinline bindingFactory: (Activity) -> T
    ): Lazy<T> = lazy {
        bindingFactory(this)
    }
} 
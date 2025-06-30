package io.github.gmathi.novellibrary.util

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*

object CoroutinesExtensions {
    // Remove deprecated GlobalScope usage
    fun LifecycleOwner.launchUI(
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job = lifecycleScope.launch(Dispatchers.Main, start = start, block = block)
    
    fun LifecycleOwner.launchIO(
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job = lifecycleScope.launch(Dispatchers.IO, start = start, block = block)
    
    fun LifecycleOwner.launchDefault(
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job = lifecycleScope.launch(Dispatchers.Default, start = start, block = block)
} 
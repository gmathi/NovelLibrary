package io.github.gmathi.novellibrary.util.lang

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*

/**
 * DEPRECATED: Use lifecycleScope.launchUI() instead to avoid memory leaks
 * This function uses GlobalScope which can cause memory leaks
 */
@Deprecated("Use lifecycleScope.launchUI() instead", ReplaceWith("lifecycleScope.launchUI(block)"))
fun launchUI(block: suspend CoroutineScope.() -> Unit): Job =
    GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT, block)

/**
 * DEPRECATED: Use lifecycleScope.launchIO() instead to avoid memory leaks
 * This function uses GlobalScope which can cause memory leaks
 */
@Deprecated("Use lifecycleScope.launchIO() instead", ReplaceWith("lifecycleScope.launchIO(block)"))
fun launchIO(block: suspend CoroutineScope.() -> Unit): Job =
    GlobalScope.launch(Dispatchers.IO, CoroutineStart.DEFAULT, block)

/**
 * DEPRECATED: Use lifecycleScope.launchNow() instead to avoid memory leaks
 * This function uses GlobalScope which can cause memory leaks
 */
@Deprecated("Use lifecycleScope.launchNow() instead", ReplaceWith("lifecycleScope.launchNow(block)"))
fun launchNow(block: suspend CoroutineScope.() -> Unit): Job =
    GlobalScope.launch(Dispatchers.Main, CoroutineStart.UNDISPATCHED, block)

// Lifecycle-aware coroutine extensions
fun LifecycleOwner.launchUI(block: suspend CoroutineScope.() -> Unit): Job =
    lifecycleScope.launch(Dispatchers.Main, block = block)

fun LifecycleOwner.launchIO(block: suspend CoroutineScope.() -> Unit): Job =
    lifecycleScope.launch(Dispatchers.IO, block = block)

fun LifecycleOwner.launchNow(block: suspend CoroutineScope.() -> Unit): Job =
    lifecycleScope.launch(Dispatchers.Main, CoroutineStart.UNDISPATCHED, block = block)

// ViewModel coroutine extensions
fun ViewModel.launchUI(block: suspend CoroutineScope.() -> Unit): Job =
    viewModelScope.launch(Dispatchers.Main, block = block)

fun ViewModel.launchIO(block: suspend CoroutineScope.() -> Unit): Job =
    viewModelScope.launch(Dispatchers.IO, block = block)

// Existing CoroutineScope extensions (keep these)
fun CoroutineScope.launchUI(block: suspend CoroutineScope.() -> Unit): Job =
    launch(Dispatchers.Main, block = block)

fun CoroutineScope.launchIO(block: suspend CoroutineScope.() -> Unit): Job =
    launch(Dispatchers.IO, block = block)

suspend fun <T> withUIContext(block: suspend CoroutineScope.() -> T) = withContext(Dispatchers.Main, block)

suspend fun <T> withIOContext(block: suspend CoroutineScope.() -> T) = withContext(Dispatchers.IO, block)

package io.github.gmathi.novellibrary.network.cloudflare

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * App-wide bus for surfacing what the [CloudflareInterceptor] is doing on the background
 * OkHttp thread, so a loading screen (e.g. the chapters loader) can show live status text
 * as a request moves through: OkHttp first → challenge detected → resolving → WebView
 * fallback → done.
 *
 * The interceptor has no handle on any ViewModel/UI, so it posts short human-readable
 * status strings here. A screen that wants to reflect them observes [status] while it is
 * loading and forwards each value to its own loading label; it should stop reflecting once
 * its load completes so unrelated background requests (images, other API calls) don't
 * overwrite its final state.
 */
object CloudflareProgress {

    private val _status = MutableLiveData<String>()
    val status: LiveData<String> = _status

    /** Post a status update from any thread (interceptor runs off the main thread). */
    fun post(message: String) {
        _status.postValue(message)
    }
}

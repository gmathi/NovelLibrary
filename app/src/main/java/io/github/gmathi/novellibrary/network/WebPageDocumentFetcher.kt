package io.github.gmathi.novellibrary.network

import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.proxy.BaseProxyHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.network.postProxy.BasePostProxyHelper
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.network.asJsoup
import io.github.gmathi.novellibrary.util.network.safeExecute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import uy.kohesive.injekt.injectLazy
import kotlin.coroutines.coroutineContext

@Suppress("unused")
object WebPageDocumentFetcher {

    private val dataCenter: DataCenter by injectLazy()
    private val dbHelper: DBHelper by injectLazy()
    private val sourceManager: SourceManager by injectLazy()
    private val networkHelper: NetworkHelper by injectLazy()
    private val client: OkHttpClient
        get() = networkHelper.cloudflareClient

    // Synchronous methods (original API)
    fun response(url: String, proxy: BaseProxyHelper?): Response {
        try {
            val request = proxy?.request(url) ?: request(url)
            return proxy?.connectSync(request) ?: connectSync(request)
        } catch (e: Exception) {
            Logs.error("WebPageDocumentFetcher", "Url: $url, Proxy: $BaseProxyHelper", e)
            throw e
        }
    }

    fun document(url: String, useProxy: Boolean = true): Document {
        var proxy: BaseProxyHelper? = null
        if (useProxy) {
            proxy = BaseProxyHelper.getInstance(url)
        }
        val response = response(url, proxy)
        val postProxy = if (useProxy) BasePostProxyHelper.getInstance(response) else null
        var doc = postProxy?.document(response) ?: proxy?.document(response) ?: document(response)
        if (doc.location().contains("rssbook") && doc.location().contains(HostNames.QIDIAN)) {
            doc = document(doc.location().replace("rssbook", "book"), useProxy)
        }
        return doc
    }

    // Suspend versions for coroutine support
    suspend fun responseAsync(url: String, proxy: BaseProxyHelper?): Response = withContext(Dispatchers.IO) {
        try {
            coroutineContext.ensureActive() // Check for cancellation
            val request = proxy?.request(url) ?: request(url)
            proxy?.connect(request) ?: connect(request)
        } catch (e: Exception) {
            Logs.error("WebPageDocumentFetcher", "Url: $url, Proxy: $BaseProxyHelper", e)
            throw e
        }
    }

    suspend fun documentAsync(url: String, useProxy: Boolean = true): Document = withContext(Dispatchers.IO) {
        coroutineContext.ensureActive() // Check for cancellation
        
        var proxy: BaseProxyHelper? = null
        if (useProxy) {
            proxy = BaseProxyHelper.getInstance(url)
        }
        
        val response = responseAsync(url, proxy)
        coroutineContext.ensureActive() // Check for cancellation after network call
        
        val postProxy = if (useProxy) BasePostProxyHelper.getInstance(response) else null
        var doc = postProxy?.document(response) ?: proxy?.document(response) ?: document(response)
        
        if (doc.location().contains("rssbook") && doc.location().contains(HostNames.QIDIAN)) {
            coroutineContext.ensureActive() // Check for cancellation before recursive call
            doc = documentAsync(doc.location().replace("rssbook", "book"), useProxy)
        }
        
        doc
    }

//    private fun string(url: String, useProxy: Boolean = true): String? {
//        var proxy: BaseProxyHelper? = null
//        if (useProxy) {
//            proxy = BaseProxyHelper.getInstance(url)
//        }
//        val response = response(url, proxy)
//        return proxy?.string(response) ?: string(response)
//    }

    // Synchronous methods (original API)
    fun connect(request: Request): Response {
        return client.newCall(request).safeExecute()
    }

    fun document(response: Response): Document {
        return response.asJsoup()
    }

    fun string(response: Response): String? = response.body?.string()

    fun request(url: String): Request = GET(url)

    // Suspend versions for coroutine support
    suspend fun connectAsync(request: Request): Response = withContext(Dispatchers.IO) {
        coroutineContext.ensureActive() // Check for cancellation
        client.newCall(request).safeExecute()
    }

    suspend fun stringAsync(response: Response): String? = withContext(Dispatchers.IO) {
        coroutineContext.ensureActive() // Check for cancellation
        response.body?.string()
    }

    // Deprecated synchronous methods for backward compatibility
    // These will be removed in a future cleanup phase
    @Deprecated("Use connect instead", ReplaceWith("connect(request)"))
    fun connectSync(request: Request): Response {
        return client.newCall(request).safeExecute()
    }

    @Deprecated("Use string instead", ReplaceWith("string(response)"))
    fun stringSync(response: Response): String? = response.body?.string()

}
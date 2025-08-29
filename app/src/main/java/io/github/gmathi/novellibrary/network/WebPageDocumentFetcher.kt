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
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Suppress("unused")
@Singleton
class WebPageDocumentFetcher @Inject constructor(
    private val dataCenter: DataCenter,
    private val dbHelper: DBHelper,
    private val sourceManager: SourceManager,
    private val networkHelper: NetworkHelper,
    private val timeoutConfig: NetworkTimeoutConfig,
    private val errorHandler: NetworkErrorHandler
) {

    private val client: OkHttpClient
        get() = networkHelper.cloudflareClient

    // Synchronous methods (original API)
    fun response(url: String, proxy: BaseProxyHelper?): Response {
        var lastException: Exception? = null
        val maxRetries = timeoutConfig.getMaxRetries()
        
        repeat(maxRetries) { attempt ->
            try {
                val request = proxy?.request(url) ?: request(url)
                return proxy?.connectSync(request) ?: connectSync(request)
            } catch (e: Exception) {
                lastException = e
                errorHandler.logError(url, e, attempt + 1, maxRetries)
                
                // Don't retry on cancellation or non-retryable errors
                if (e.message?.contains("Canceled") == true || !errorHandler.isRetryableError(e)) {
                    throw e
                }
                
                // If this isn't the last attempt, wait before retrying
                if (attempt < maxRetries - 1) {
                    val delay = errorHandler.getRetryDelay(e, attempt)
                    Thread.sleep(delay)
                }
            }
        }
        
        // All retries failed, throw with user-friendly message
        val friendlyMessage = errorHandler.getErrorMessage(lastException!!)
        throw Exception("Network request failed after $maxRetries attempts: $friendlyMessage", lastException)
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
        var lastException: Exception? = null
        val maxRetries = timeoutConfig.getMaxRetries()
        
        repeat(maxRetries) { attempt ->
            try {
                coroutineContext.ensureActive() // Check for cancellation
                val request = proxy?.request(url) ?: request(url)
                return@withContext proxy?.connect(request) ?: connect(request)
            } catch (e: Exception) {
                lastException = e
                errorHandler.logError(url, e, attempt + 1, maxRetries)
                
                // Don't retry on cancellation or non-retryable errors
                if (e is kotlinx.coroutines.CancellationException || 
                    e.message?.contains("Canceled") == true ||
                    !errorHandler.isRetryableError(e)) {
                    throw e
                }
                
                // If this isn't the last attempt, wait before retrying
                if (attempt < maxRetries - 1) {
                    val delay = errorHandler.getRetryDelay(e, attempt)
                    kotlinx.coroutines.delay(delay)
                    coroutineContext.ensureActive() // Check for cancellation after delay
                }
            }
        }
        
        // All retries failed, throw with user-friendly message
        val friendlyMessage = errorHandler.getErrorMessage(lastException!!)
        throw Exception("Network request failed after $maxRetries attempts: $friendlyMessage", lastException)
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
        return client.newCall(request).safeExecute(dataCenter)
    }

    fun document(response: Response): Document {
        return response.asJsoup()
    }

    fun string(response: Response): String? = response.body?.string()

    fun request(url: String): Request = GET(url)

    // Suspend versions for coroutine support
    suspend fun connectAsync(request: Request): Response = withContext(Dispatchers.IO) {
        coroutineContext.ensureActive() // Check for cancellation
        client.newCall(request).safeExecute(dataCenter)
    }

    suspend fun stringAsync(response: Response): String? = withContext(Dispatchers.IO) {
        coroutineContext.ensureActive() // Check for cancellation
        response.body?.string()
    }

    // Deprecated synchronous methods for backward compatibility
    // These will be removed in a future cleanup phase
    @Deprecated("Use connect instead", ReplaceWith("connect(request)"))
    fun connectSync(request: Request): Response {
        return client.newCall(request).safeExecute(dataCenter)
    }

    @Deprecated("Use string instead", ReplaceWith("string(response)"))
    fun stringSync(response: Response): String? = response.body?.string()

    companion object {
        @Volatile
        private var INSTANCE: WebPageDocumentFetcher? = null
        
        fun getInstance(): WebPageDocumentFetcher {
            return INSTANCE ?: throw IllegalStateException("WebPageDocumentFetcher not initialized. Make sure Hilt is properly set up.")
        }
        
        internal fun setInstance(instance: WebPageDocumentFetcher) {
            INSTANCE = instance
        }
        
        // Static methods for backward compatibility
        fun response(url: String, proxy: BaseProxyHelper?): Response = getInstance().response(url, proxy)
        fun document(url: String, useProxy: Boolean = true): Document = getInstance().document(url, useProxy)
        suspend fun responseAsync(url: String, proxy: BaseProxyHelper?): Response = getInstance().responseAsync(url, proxy)
        suspend fun documentAsync(url: String, useProxy: Boolean = true): Document = getInstance().documentAsync(url, useProxy)
        fun connect(request: Request): Response = getInstance().connect(request)
        fun document(response: Response): Document = getInstance().document(response)
        fun string(response: Response): String? = getInstance().string(response)
        fun request(url: String): Request = getInstance().request(url)
        suspend fun connectAsync(request: Request): Response = getInstance().connectAsync(request)
        suspend fun stringAsync(response: Response): String? = getInstance().stringAsync(response)
        
        @Deprecated("Use connect instead", ReplaceWith("connect(request)"))
        fun connectSync(request: Request): Response = getInstance().connectSync(request)
        
        @Deprecated("Use string instead", ReplaceWith("string(response)"))
        fun stringSync(response: Response): String? = getInstance().stringSync(response)
    }
}
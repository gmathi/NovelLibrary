package io.github.gmathi.novellibrary.network

import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.proxy.BaseProxyHelper
import io.github.gmathi.novellibrary.util.DataCenter
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.network.asJsoup
import io.github.gmathi.novellibrary.util.network.safeExecute
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import uy.kohesive.injekt.injectLazy

@Suppress("unused")
object WebPageDocumentFetcher {

    private val dataCenter: DataCenter by injectLazy()
    private val dbHelper: DBHelper by injectLazy()
    private val sourceManager: SourceManager by injectLazy()
    private val networkHelper: NetworkHelper by injectLazy()
    private val client: OkHttpClient
        get() = networkHelper.cloudflareClient

    private fun response(url: String, proxy: BaseProxyHelper?): Response {
        try {
            val request = proxy?.request(url) ?: request(url)
            return proxy?.connect(request) ?: connect(request)
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
        var doc = proxy?.document(response) ?: document(response)
        if (doc.location().contains("rssbook") && doc.location().contains(HostNames.QIDIAN)) {
            doc = document(doc.location().replace("rssbook", "book"), useProxy)
        }
        return doc
    }

//    private fun string(url: String, useProxy: Boolean = true): String? {
//        var proxy: BaseProxyHelper? = null
//        if (useProxy) {
//            proxy = BaseProxyHelper.getInstance(url)
//        }
//        val response = response(url, proxy)
//        return proxy?.string(response) ?: string(response)
//    }

    fun connect(request: Request): Response {
        return client.newCall(request).safeExecute()
    }

    fun document(response: Response): Document {
        return response.asJsoup()
    }

//    private fun string(response: Response): String? {
//        return response.body?.string()
//    }

    fun request(url: String): Request = GET(url)

}
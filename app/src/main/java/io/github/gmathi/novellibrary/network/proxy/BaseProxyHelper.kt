package io.github.gmathi.novellibrary.network.proxy

import androidx.core.text.htmlEncode
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.network.WebPageDocumentFetcher
import io.github.gmathi.novellibrary.network.postProxy.JsonContentProxy
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import uy.kohesive.injekt.injectLazy

/**
 * @see io.github.gmathi.novellibrary.network.NovelApi.getDocumentWithParams
 */
open class BaseProxyHelper {

    val networkHelper: NetworkHelper by injectLazy()
    val client: OkHttpClient
        get() = networkHelper.cloudflareClient

    companion object {
        fun getInstance(url: String): BaseProxyHelper? = when {
            url.contains(HostNames.FOXTELLER) -> FoxTellerProxy()
            url.contains(HostNames.WATTPAD) -> WattPadProxy()
            url.contains(HostNames.BABEL_NOVEL) -> BabelNovelProxy()
            else -> null
        }
    }

    /** Connection used when requesting document. */
    open fun request(url: String): Request = WebPageDocumentFetcher.request(url)
    open fun connect(request: Request): Response = WebPageDocumentFetcher.connect(request)
    open fun document(response: Response): Document = WebPageDocumentFetcher.document(response)
    open fun string(response: Response): String? = WebPageDocumentFetcher.string(response)
}
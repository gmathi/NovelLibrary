package io.github.gmathi.novellibrary.network

import android.net.Uri
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.proxy.BaseProxyHelper
import io.github.gmathi.novellibrary.util.DataCenter
import io.github.gmathi.novellibrary.util.network.asJsoup
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import uy.kohesive.injekt.injectLazy
import java.io.IOException
import java.util.regex.Pattern
import javax.net.ssl.SSLPeerUnverifiedException

object WebPageDocumentFetcher {

    private val dataCenter: DataCenter by injectLazy()
    private val dbHelper: DBHelper by injectLazy()
    private val sourceManager: SourceManager by injectLazy()
    private val networkHelper: NetworkHelper by injectLazy()
    private val client: OkHttpClient
        get() = networkHelper.cloudflareClient

    fun document(url: String, useProxy: Boolean = true): Document {
        var proxy: BaseProxyHelper? = null
        if (useProxy) {
            proxy = BaseProxyHelper.getInstance(url)
        }

        try {
            val request = proxy?.request(url) ?: request(url)
            val response = proxy?.connect(request) ?: connect(request)
            return proxy?.document(response) ?: document(response)
        } catch (e: SSLPeerUnverifiedException) {
            val p = Pattern.compile("Hostname\\s(.*?)\\snot", Pattern.DOTALL or Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE or Pattern.MULTILINE) // Regex for the value of the key
            val m = p.matcher(e.localizedMessage ?: "")
            if (m.find()) {
                val hostName = m.group(1)
                val hostNames = dataCenter.getVerifiedHosts()
                if (!hostNames.contains(hostName ?: "")) {
                    dataCenter.saveVerifiedHost(hostName ?: "")
                }
            }
            throw e
        } catch (e: IOException) {
            val error = e.localizedMessage
            if (error != null && error.contains("was not verified")) {
                val hostName = Uri.parse(url)?.host!!
                if (!HostNames.isVerifiedHost(hostName)) {
                    dataCenter.saveVerifiedHost(hostName)
                }
            }
            throw e
        }
    }

    fun connect(request: Request): Response {
        return client.newCall(request).execute()
    }

    fun document(response: Response): Document {
        return response.asJsoup()
    }

    fun request(url: String): Request = GET(url)

}
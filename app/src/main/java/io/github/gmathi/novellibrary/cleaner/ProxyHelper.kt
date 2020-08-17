package io.github.gmathi.novellibrary.cleaner

import io.github.gmathi.novellibrary.network.CloudFlareByPasser
import io.github.gmathi.novellibrary.network.HostNames
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL

/**
 * @see io.github.gmathi.novellibrary.network.NovelApi.getDocumentWithParams
 */
open class ProxyHelper {
    companion object {
        fun getInstance(url: String): ProxyHelper? = when {
                url.contains(HostNames.FOXTELLER) -> FoxtellerProxy()
                else -> null
            }
    }

    /** Connection used when requesting document. */
    open fun connect(url: String): Connection =
        Jsoup.connect(url).referrer(url)
            .cookies(CloudFlareByPasser.getCookieMap(URL(url)))
            .ignoreHttpErrors(true)
            .timeout(30000)
            .userAgent(HostNames.USER_AGENT)
            .followRedirects(false)

    /** Data to use for parsing */
    open fun body(res: Connection.Response): String = res.body()

    /** Modify document after requesting */
    open fun document(body: String, res: Connection.Response): Document = Jsoup.parse(body)

    /** Return Document Type */
    open fun parse(res: Connection.Response): Document = res.parse()
}

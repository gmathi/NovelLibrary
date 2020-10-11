package io.github.gmathi.novellibrary.network.proxy

import io.github.gmathi.novellibrary.network.CloudFlareByPasser
import io.github.gmathi.novellibrary.network.HostNames
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL

/**
 * @see io.github.gmathi.novellibrary.network.NovelApi.getDocumentWithParams
 */
open class BaseProxyHelper {

    companion object {
        fun getInstance(url: String): BaseProxyHelper? = when {
            url.contains(HostNames.FOXTELLER) -> FoxTellerProxy()
            url.contains(HostNames.WATTPAD) -> WattPadProxy()
            url.contains(HostNames.BABEL_NOVEL) -> BabelNovelProxy()
            else -> null
        }
    }

    /** Connection used when requesting document. */
    open fun connect(url: String): Connection =
        Jsoup.connect(url).referrer(url)
            .cookies(CloudFlareByPasser.getCookieMap(URL(url)))
            .ignoreHttpErrors(true)
            .timeout(30000)
            .ignoreContentType(true)
            .userAgent(HostNames.USER_AGENT)
            .followRedirects(false)

    /** Data to use for parsing */
    open fun body(res: Connection.Response): String = res.body()

    /** Modify document after requesting */
    open fun document(res: Connection.Response): Document = res.parse()

    open fun document(body: String, res: Connection.Response): Document = Jsoup.parse(body)
}

package io.github.gmathi.novellibrary.network.proxy

import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class WattPadProxy : BaseProxyHelper() {

    companion object {

    }

    @ExperimentalStdlibApi
    override fun document(res: Connection.Response): Document {
        val doc = res.parse()

        // Get second half URL
        val urlRegex = """^[ \n\t]*window\.prefetched *= *\{".+?":\{"data":\{.*?"text_url":\{"text":"([^"]+?)"""".toRegex()
        val url = doc.getElementsByTag("script").mapNotNull {
            val finds = urlRegex.find(it.data())
            if (finds == null) null
            else finds.groups[1]?.value
        }.firstOrNull()

        val preElem = doc.selectFirst("div.page pre")!!
        val contentElem = preElem.parent()

        // Request and append second half
        if (url != null) {
            val secondHalfContent = Jsoup.connect(url).execute().body()
            contentElem.append(secondHalfContent)
        } else
            contentElem.append("<br /><p><b>ERROR: Failed to load second half of chapter.</b></p>")

        return doc
    }

}

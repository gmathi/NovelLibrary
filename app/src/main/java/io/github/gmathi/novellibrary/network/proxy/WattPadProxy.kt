package io.github.gmathi.novellibrary.network.proxy

import io.github.gmathi.novellibrary.util.Logs
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class WattPadProxy : BaseProxyHelper() {

    @ExperimentalStdlibApi
    override fun document(response: Response): Document {
        val doc = super.document(response)

        // Get second half URL
        val urlRegex = """^[ \n\t]*window\.prefetched *= *\{".+?":\{"data":\{.*?"text_url":\{"text":"([^"]+?)"""".toRegex()
        val url = doc.getElementsByTag("script").mapNotNull {
            val finds = urlRegex.find(it.data())
            if (finds == null) null
            else finds.groups[1]?.value
        }.firstOrNull()

        val preElem = doc.select("div.page pre").firstOrNull() ?: return doc
        val contentElem = preElem.parent()

        // Request and append second half
        if (url != null) {
            try {
                val secondHalfContent = Jsoup.connect(url).execute().body()
                contentElem.append(secondHalfContent)
            } catch (e: Exception) {
                Logs.error("WattPadProxy", "Url: $url", e)
            }
        } else
            contentElem.append("<br/><p><b>ERROR: Failed to load second half of chapter.</b></p>")
        return doc
    }

}

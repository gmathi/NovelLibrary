package io.github.gmathi.novellibrary.cleaner

import android.net.Uri
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File


class CloudFlareDDoSTagCleaner : HtmlHelper() {

    override fun additionalProcessing(doc: Document) {
        val contentElement = doc.body().getElementsByTag("a").firstOrNull { it.attr("href").contains("https://www.cloudflare.com/") && it.text().contains("DDoS protection by Cloudflare") }
        contentElement?.append("<br/><br/><br/><a href='abc://retry_internal'>Click here to retry with cloud flare cookies</a>")
    }

}

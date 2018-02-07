package io.github.gmathi.novellibrary.cleaner

import android.net.Uri
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File


class CloudFlareDDoSTagHelper() : HtmlHelper() {

    override fun additionalProcessing(doc: Document) {
        val contentElement = doc.body().getElementsByTag("a").firstOrNull { it.attr("href").contains("https://www.cloudflare.com/") && it.text().contains("DDoS protection by Cloudflare") }
        contentElement?.append("<br/><br/><br/><a href='abc://retry_internal'>Click here to retry with cloud flare cookies</a>")
    }

    override fun downloadImage(element: Element, dir: File): File? {
        val uri = Uri.parse(element.attr("src"))
        return if (uri.toString().contains("uploads/avatars")) null
        else super.downloadImage(element, dir)
    }

    override fun removeJS(doc: Document) {
        super.removeJS(doc)
        doc.getElementsByTag("noscript").remove()
    }

    override fun toggleTheme(isDark: Boolean, doc: Document): Document {
        return super.toggleThemeDefault(isDark, doc)
    }
}

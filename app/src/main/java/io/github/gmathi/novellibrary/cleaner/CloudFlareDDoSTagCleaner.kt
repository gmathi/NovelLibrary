package io.github.gmathi.novellibrary.cleaner

import io.github.gmathi.novellibrary.model.preference.DataCenter
import org.jsoup.nodes.Document


class CloudFlareDDoSTagCleaner(dataCenter: DataCenter) : HtmlCleaner(dataCenter) {

    override fun additionalProcessing(doc: Document) {
        val contentElement = doc.body().getElementsByTag("a").firstOrNull { it.attr("href").contains("https://www.cloudflare.com/") && it.text().contains("DDoS protection by Cloudflare") }
        contentElement?.append("<br/><br/><br/><a href='abc://retry_internal'>Click here to retry with cloud flare cookies</a>")
    }

}

package io.github.gmathi.novellibrary.network

import io.github.gmathi.novellibrary.network.HostNames.USER_AGENT
import org.jsoup.Jsoup
import org.jsoup.nodes.Document


class NovelApi {

    fun getDocument(url: String): Document {
        return Jsoup.connect(url).get()
    }

    fun getDocumentWithUserAgent(url: String): Document {
        return Jsoup.connect(url).userAgent(USER_AGENT).get()
    }


}
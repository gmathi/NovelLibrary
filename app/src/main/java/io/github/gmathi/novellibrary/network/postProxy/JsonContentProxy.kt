package io.github.gmathi.novellibrary.network.postProxy

import io.github.gmathi.novellibrary.network.proxy.BaseProxyHelper
import io.github.gmathi.novellibrary.util.network.asJsoup
import okhttp3.Response
import org.jsoup.nodes.Document

abstract class JsonContentProxy<T> : BasePostProxyHelper() {
    abstract fun extractJson(doc: Document): Document?

    override fun document(response: Response): Document {
        val doc = response.asJsoup()
        val jdoc = extractJson(doc)
        return extractJson(doc) ?: doc
    }

}
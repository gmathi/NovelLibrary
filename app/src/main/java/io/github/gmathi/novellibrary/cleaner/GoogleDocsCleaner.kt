package io.github.gmathi.novellibrary.cleaner

import android.net.Uri
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Constants.FILE_PROTOCOL
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File

class GoogleDocsCleaner : HtmlHelper() {

    override fun additionalProcessing(doc: Document) {
        removeCSS(doc)
        cleanAttributes(doc.body())
    }

    private fun cleanAttributes(contentElement: Element?) {
        if (contentElement?.tagName() != "a" || contentElement.tagName() != "img")
            contentElement?.clearAttributes()
        contentElement?.children()?.forEach {
            cleanAttributes(it)
        }
    }
}
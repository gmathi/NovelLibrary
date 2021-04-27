package io.github.gmathi.novellibrary.network.proxy

import com.google.gson.JsonParser
import io.github.gmathi.novellibrary.util.lang.asJsonNullFreeString
import okhttp3.Response
import org.jsoup.nodes.Document
import java.net.URL

class BabelNovelProxy : BaseProxyHelper() {

    @ExperimentalStdlibApi
    override fun document(response: Response): Document {
        val doc = super.document(response)
        val uri = URL(doc.location())
        val docUrl = "${uri.protocol}://${uri.host}/api${uri.path}/content"

        try {
            val request = super.request(docUrl)
            val contentResponse = super.connect(request)
            val responseDataJson = JsonParser.parseString(contentResponse.body?.string())?.asJsonObject?.get("data")?.asJsonObject
            val title = responseDataJson?.get("name")?.asJsonNullFreeString ?: "Chapter name not found!"
            val content = responseDataJson?.get("content")?.asJsonNullFreeString ?: "No Content Found!"
            val lines = content.split("\n")
            val htmlFormattedContent = lines.joinToString(separator = "") {
                """
                <div class="paragraph"><p>$it</p></div>    
                """
            }

            val html = """
                <div class="content-container">
                    <h1 class="title">$title</h1>
                    <div class="content">$htmlFormattedContent</div>
                </div>
            """.trimIndent()
            doc.body().children().remove()
            doc.body().append(html)

        } catch (e: Exception) {
            //Do Nothing
        }
        return doc
    }

}

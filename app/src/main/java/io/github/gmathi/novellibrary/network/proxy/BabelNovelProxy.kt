package io.github.gmathi.novellibrary.network.proxy

import com.google.gson.JsonParser
import io.github.gmathi.novellibrary.extensions.asJsonNullFreeString
import io.github.gmathi.novellibrary.network.CloudFlareByPasser
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.util.Constants
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL

class BabelNovelProxy : BaseProxyHelper() {

    @ExperimentalStdlibApi
    override fun document(res: Connection.Response): Document {
        val doc = res.parse()
        val uri = URL(doc.location())
        val url = "${uri.protocol}://${uri.host}/api${uri.path}/content"

        try {
            val contentResponse = connect(url).cookies(res.cookies()).execute()
            val responseDataJson = JsonParser.parseString(contentResponse.body())?.asJsonObject?.get("data")?.asJsonObject
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
            // In-case of exception,
            // we do nothing so it escapes the catch block and
            // we send back the doc we initially got from the parent response.
            e.printStackTrace()
        }
        return doc
    }

}

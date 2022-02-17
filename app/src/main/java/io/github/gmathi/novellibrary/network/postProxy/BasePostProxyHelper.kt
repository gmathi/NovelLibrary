package io.github.gmathi.novellibrary.network.postProxy

import androidx.core.text.htmlEncode
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.network.WebPageDocumentFetcher
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uy.kohesive.injekt.injectLazy

/**
 * @see io.github.gmathi.novellibrary.network.NovelApi.getDocumentWithParams
 */
open class BasePostProxyHelper {

    val networkHelper: NetworkHelper by injectLazy()
    val client: OkHttpClient
        get() = networkHelper.cloudflareClient

    companion object {
        fun getInstance(response: Response): BasePostProxyHelper? {
            val url = response.request.url.toString()
            return when {
                url.contains("inoveltranslation.com") -> object : JsonContentProxy<INovelTranslationJson>() {
                    override fun extractJson(doc: Document): Document? {
                        """/chapters/(\d+)""".toRegex().find(doc.location())?.let { match ->
                            val jsonString = string(connect(request("https://api.inoveltranslation.com/chapters/${match.groups[1]!!.value}"))) ?: return null
                            val json: INovelTranslationJson = Gson().fromJson(jsonString)
                            val raw = json.content
                                // Hide obnoxious T/N notes in TTS.
                                .replace("""---\s*(T/N:|Hello!\s?\?\?+).+(\n\[!$|---)""".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)), "<div tts-disable=\"true\">\n\n$0\n\n</div>")
                                .replace("""T/N:.+shoutout to the following:.*?for being.*?patre?ons?.*?$""".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)), "<div tts-disable=\"true\">\n\n$0\n\n</div>")
                                .replace("""(Editor|Translator|T/N): .+""".toRegex(), "<p tts-disable=\"true\">$0</p>")
                            val html = raw.let {
                                val flavour = CommonMarkFlavourDescriptor()
                                val tree = MarkdownParser(flavour).buildMarkdownTreeFromString(it)
                                HtmlGenerator(it, tree, flavour).generateHtml()
                            }
                            return Jsoup.parse("""
                            <!DOCTYPE html>
                            <html>
                                <head>
                                    <title>${json.novel.title} -${json.volume?.let { " vol.$it" } ?: ""} ch.${json.chapter}${json.title?.let { " - ${it.htmlEncode()}" } ?: ""}</title>
                                </head>
                                <body>
                                    <div class="content">$html</div>
                                </body>
                            </html>
                            """.trimIndent(), doc.location()
                            )
                        }
                        return null
                    }
                }
                else -> null
            }
        }
    }

    /** Connection used when requesting document. */
    open fun request(url: String): Request = WebPageDocumentFetcher.request(url)
    open fun connect(request: Request): Response = WebPageDocumentFetcher.connect(request)
    open fun document(response: Response): Document = WebPageDocumentFetcher.document(response)
    open fun string(response: Response): String? = WebPageDocumentFetcher.string(response)
}

private data class INovelTranslationJson(val chapter: Int, val content: String, val id: Int, val tier: Any?, val tierId: Any?, val title: String?, val volume: Int?, val novel: INovelTranslationJsonNovel)
private data class INovelTranslationJsonNovel(val cover: Any?, val id: Int, val patreon: Any?, val title: String)
package io.github.gmathi.novellibrary.cleaner

import android.graphics.Bitmap
import android.net.Uri
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.util.Constants.FILE_PROTOCOL
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.getFileName
import io.github.gmathi.novellibrary.util.writableFileName
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File
import java.io.FileOutputStream


open class HtmlHelper protected constructor() {

    companion object {

        private const val TAG = "HtmlHelper"

        fun getInstance(doc: Document, hostName: String = doc.location()): HtmlHelper {

            val host =
                    if (hostName.startsWith("www."))
                        hostName.replace("www.", "")
                    else
                        hostName

            when {
                host.contains(HostNames.WUXIA_WORLD) -> return WuxiaWorldHelper()
                host.contains(HostNames.CIRCUS_TRANSLATIONS) -> return CircusTranslationsHelper()
                host.contains(HostNames.QIDIAN) -> return QidianHelper()
                host.contains(HostNames.GOOGLE_DOCS) -> return GoogleDocsCleaner()
                host.contains(HostNames.BLUE_SILVER_TRANSLATIONS) -> return BlueSilverTranslationsHelper()
                host.contains(HostNames.TUMBLR) -> return TumblrCleaner()
                host.contains(HostNames.BAKA_TSUKI) -> return BakaTsukiCleaner()
                host.contains(HostNames.SCRIBBLE_HUB) -> return ScribbleHubHelper()
            }

            var contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("chapter-content") }
            if (contentElement != null) return GeneralClassTagHelper(host, "div", "chapter-content")

            contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("entry-content") }
            if (contentElement != null) return GeneralClassTagHelper(host, "div", "entry-content")

            contentElement = doc.body().getElementsByTag("article").firstOrNull { it.hasClass("hentry") }
            if (contentElement != null) return GeneralClassTagHelper(host, "article", "hentry")

            contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("hentry") }
            if (contentElement != null) return GeneralClassTagHelper(host, "div", "hentry")

            contentElement = doc.body().getElementsByTag("div").firstOrNull { it.id() == "chapter_body" }
            if (contentElement != null) return GeneralIdTagHelper(host, "div", "chapter_body")

            contentElement = doc.body().getElementsByTag("article").firstOrNull { it.id() == "releases" }
            if (contentElement != null) return GeneralIdTagHelper(host, "article", "releases")

            contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("td-main-content") }
            if (contentElement != null) return GeneralClassTagHelper(host, "div", "td-main-content")

            contentElement = doc.body().getElementsByTag("div").firstOrNull { it.id() == "content" }
            if (contentElement != null) return GeneralIdTagHelper(host, "div", "content")

            contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("post-inner") }
            if (contentElement != null) return GeneralClassTagHelper(host, "div", "post-inner", appendTitle = false)

            contentElement = doc.body().getElementsByTag("a").firstOrNull { it.attr("href").contains("https://www.cloudflare.com/") && it.text().contains("DDoS protection by Cloudflare") }
            if (contentElement != null) return CloudFlareDDoSTagHelper()

            contentElement = doc.body().select("div#chapter-content").firstOrNull()
            if (contentElement != null) return GeneralIdTagHelper(host, "div", "chapter-content")

            return HtmlHelper()
        }

    }

    fun clean(doc: Document, hostDir: File, novelDir: File) {
        // removeJS(doc)
        downloadCSS(doc, hostDir)
        downloadImages(doc, novelDir)
        // additionalProcessing(doc)
        // addTitle(doc)
    }

    open fun removeJS(doc: Document) {
        doc.getElementsByTag("script").remove()
        doc.getElementsByTag("noscript").remove()
    }

    open fun removeCSS(doc: Document) {
        doc.getElementsByTag("link").remove()
    }


    open fun downloadCSS(doc: Document, downloadDir: File) {
        val elements = doc.head().getElementsByTag("link").filter { element -> element.hasAttr("rel") && element.attr("rel") == "stylesheet" }
        for (element in elements) {
            val cssFile = downloadFile(element, downloadDir)
            if (cssFile != null) {
                element.removeAttr("href")
                element.attr("href", "../" + cssFile.name)
                //doc.head().appendElement("link").attr("rel", "stylesheet").attr("type", "text/css").attr("href", "../" + cssFile.name)
            } else {
                element.remove()
            }
        }
    }

    open fun additionalProcessing(doc: Document) {

    }

    open fun downloadFile(element: Element, dir: File): File? {
        val uri = Uri.parse(element.absUrl("href"))
        val file: File
        val doc: Document
        try {
            if (uri.scheme == null || uri.host == null) throw Exception("Invalid URI: $uri")
            val fileName = uri.getFileName()
            file = File(dir, fileName)
            doc = NovelApi.getDocumentWithUserAgentIgnoreContentType(uri.toString())
        } catch (e: Exception) {
            Logs.warning(TAG, "Uri: $uri", e)
            return null
        }
        return convertDocToFile(doc, file)
    }

    open fun convertDocToFile(doc: Document, file: File): File? {
        try {
            if (file.exists()) file.delete()
            val stream = FileOutputStream(file)
            val content = doc.toString()
            stream.use { it.write(content.toByteArray()) }
        } catch (e: Exception) {
            Logs.warning(TAG, "convertDocToFile: ${file.name}", e)
            return null
        }
        return file
    }

    open fun downloadImages(doc: Document, novelDir: File) {
        val elements = doc.getElementsByTag("img").filter { element -> element.hasAttr("src") }
        for (element in elements) {
            val imageFile = getImageFile(element, novelDir)
            if (imageFile != null) {
                if (!imageFile.exists())
                    downloadImage(element, imageFile)
                element.removeAttr("src")
                element.attr("src", "./${imageFile.name}")
            }
        }
    }

    open fun getImageFile(element: Element, dir: File): File? {
        val uri = Uri.parse(element.absUrl("src"))
        val file: File
        try {
            if (uri.scheme == null || uri.host == null) throw Exception("Invalid URI: $uri")
            val fileName = (uri.lastPathSegment ?: "" + uri.query).writableFileName()
            file = File(dir, fileName)
        } catch (e: Exception) {
            Logs.debug(TAG, "Exception Downloading Image: $uri")
            return null
        }
        return file
    }

    open fun downloadImage(element: Element, file: File): File? {
        val uri = Uri.parse(element.absUrl("src"))
        try {
            val response = Jsoup.connect(uri.toString()).userAgent(HostNames.USER_AGENT).ignoreContentType(true).execute()
            val bytes = response.bodyAsBytes()
            val bitmap = Utils.getImage(bytes)
            val os = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
        } catch (e: Exception) {
            Logs.debug(TAG, "Exception Downloading Image: $uri")
            return null
        }
        return file
    }

    fun getTitle(doc: Document): String? = doc.head().getElementsByTag("title").text()

    open fun toggleTheme(isDark: Boolean, doc: Document): Document = doc

    fun toggleThemeDefault(isDark: Boolean, doc: Document): Document {

        var fontName = "source_sans_pro_regular.ttf"
        var fontUrl = "/android_asset/fonts/$fontName"

        val fontFile = File(dataCenter.fontPath)
        if (fontFile.exists()) {
            fontName = fontFile.name
            fontUrl = fontFile.path
        }

        val fontFamily = fontName.substring(0, fontName.lastIndexOf("."))
        val nightModeTextBrightness = 0
        doc.head().getElementById("darkTheme")?.remove()
        doc.head().append("""
            <style id="darkTheme">
                @font-face {
                    font-family: $fontFamily;
                    src: url("$FILE_PROTOCOL$fontUrl");
                }
                html {
                    scroll-behavior: smooth;
                }
                body {
                    ${if (isDark) "background-color" else "color"}: #3A3A3A;
                    ${if (isDark) "color" else "background-color"}: rgba(200, 200, 200, 1.$nightModeTextBrightness);
                    font-family: '$fontFamily';
                    line-height: 1.3;
                    padding: 3px;
                    text-align: left;
                }
                a {
                    color: rgba(${if (isDark) "135, 206, 250" else "0, 0, 238"}, .$nightModeTextBrightness);
                }
                table {
                    background: #004b7a;
                    margin: 10px auto;
                    width: 90%;
                    border: none;
                    box-shadow: 1px 1px 1px rgba(0, 0, 0, .75);
                    border-collapse: separate;
                    border-spacing: 2px;
                }
                p  {
                    text-align: left;
                }
            </style>
            """.trimIndent())

        return doc
    }

    open fun getLinkedChapters(doc: Document): ArrayList<String> = ArrayList()

    fun cleanClassAndIds(contentElement: Element?) {
        contentElement?.classNames()?.forEach { contentElement.removeClass(it) }
        contentElement?.removeAttr("style")
        if (contentElement != null && contentElement.hasAttr("id"))
            contentElement.removeAttr("id")
    }

    fun cleanCSSFromChildren(contentElement: Element?) {
        cleanClassAndIds(contentElement)
        contentElement?.children()?.forEach {
            cleanCSSFromChildren(it)
        }
    }
}
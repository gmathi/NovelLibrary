package io.github.gmathi.novellibrary.cleaner

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.util.Constants.FILE_PROTOCOL
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.getFileName
import io.github.gmathi.novellibrary.util.writableFileName
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File
import java.io.FileOutputStream
import java.net.SocketException


open class HtmlHelper protected constructor() {

    companion object {

        private const val TAG = "HtmlHelper"

        fun getInstance(doc: Document, url: String = doc.location()): HtmlHelper {
            when {
                url.contains(HostNames.WATTPAD) -> return WattPadHelper()
                url.contains(HostNames.WUXIA_WORLD) -> return WuxiaWorldHelper()
                url.contains(HostNames.CIRCUS_TRANSLATIONS) -> return CircusTranslationsHelper()
                url.contains(HostNames.QIDIAN) -> return QidianHelper()
                url.contains(HostNames.GOOGLE_DOCS) -> return GoogleDocsCleaner()
                url.contains(HostNames.BLUE_SILVER_TRANSLATIONS) -> return BlueSilverTranslationsHelper()
                url.contains(HostNames.TUMBLR) -> return TumblrCleaner()
                url.contains(HostNames.BAKA_TSUKI) -> return BakaTsukiCleaner()
                url.contains(HostNames.SCRIBBLE_HUB) -> return ScribbleHubHelper()
            }

            var contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("chapter-content") }
            if (contentElement != null) return GeneralClassTagHelper(url, "div", "chapter-content")

            contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("entry-content") }
            if (contentElement != null) return GeneralClassTagHelper(url, "div", "entry-content")

            contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("elementor-widget-theme-post-content") }
            if (contentElement != null) return GeneralClassTagHelper(url, "div", "elementor-widget-theme-post-content", appendTitle = false)

            contentElement = doc.body().getElementsByTag("article").firstOrNull { it.hasClass("hentry") }
            if (contentElement != null) return GeneralClassTagHelper(url, "article", "hentry")

            contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("hentry") }
            if (contentElement != null) return GeneralClassTagHelper(url, "div", "hentry")

            contentElement = doc.body().getElementsByTag("div").firstOrNull { it.id() == "chapter_body" }
            if (contentElement != null) return GeneralIdTagHelper(url, "div", "chapter_body")

            contentElement = doc.body().getElementsByTag("article").firstOrNull { it.id() == "releases" }
            if (contentElement != null) return GeneralIdTagHelper(url, "article", "releases")

            contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("td-main-content") }
            if (contentElement != null) return GeneralClassTagHelper(url, "div", "td-main-content")

            contentElement = doc.body().getElementsByTag("div").firstOrNull { it.id() == "content" }
            if (contentElement != null) return GeneralIdTagHelper(url, "div", "content")

            contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("post-inner") }
            if (contentElement != null) return GeneralClassTagHelper(url, "div", "post-inner", appendTitle = false)

            contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("blog-content") }
            if (contentElement != null) return GeneralClassTagHelper(url, "div", "blog-content")

            contentElement = doc.body().select("div#chapter-content").firstOrNull()
            if (contentElement != null) return GeneralIdTagHelper(url, "div", "chapter-content")

            contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("panel-body") }
            if (contentElement != null) return GeneralClassTagHelper(url, "div", "panel-body", appendTitle = false)

            contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("post-entry") }
            if (contentElement != null) return GeneralClassTagHelper(url, "div", "post-entry")

            contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("text-formatting") }
            if (contentElement != null) return GeneralClassTagHelper(url, "div", "text-formatting")

            contentElement = doc.body().select("article.single__contents").firstOrNull()
            if (contentElement != null) return GeneralClassTagHelper(url, "article", "single__contents")

//            contentElement = doc.body().select("article.story-part").firstOrNull()
//            if (contentElement != null) return GeneralClassTagHelper(url, "article", "story-part")

            contentElement = doc.body().select("div#chapter").firstOrNull()
            if (contentElement != null) return GeneralIdTagHelper(url, "div", "chapter")

            //TODO: Xiaxia novel, needs more analysis to fix pre-formatting (jsoup not supporting)
            contentElement = doc.body().select("section#StoryContent").firstOrNull()
            if (contentElement != null) return GeneralIdTagHelper(url, "section", "StoryContent")

            contentElement = doc.body().select("div.content-container").firstOrNull()
            if (contentElement != null) return GeneralClassTagHelper(url, "div", "content-container")

            contentElement = doc.body().select("article.article-content").firstOrNull()
            if (contentElement != null) return GeneralClassTagHelper(url, "article", "article-content")

            //Lastly let's check for cloud flare
            contentElement = doc.body().getElementsByTag("a").firstOrNull { it.attr("href").contains("https://www.cloudflare.com/") && it.text().contains("DDoS protection by Cloudflare") }
            if (contentElement != null) return CloudFlareDDoSTagHelper()

            return HtmlHelper()
        }

    }

    open var keepContentStyle = false

    fun downloadResources(doc: Document, hostDir: File, novelDir: File) {
        // removeJS(doc)
        downloadCSS(doc, hostDir)
        downloadImages(doc, novelDir)
        // additionalProcessing(doc)
        // addTitle(doc)
    }

    fun setProperHrefUrls(doc: Document) {
        doc.body().select("[href]").forEach { it.attr("href", it.absUrl("href")) }
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

    open fun downloadFile(element: Element, dir: File, retryCount: Int = 0): File? {
        val uri = Uri.parse(element.absUrl("href"))
        val file: File
        val doc: Document
        try {
            if (uri.scheme == null || uri.host == null) throw Exception("Invalid URI: $uri")
            val fileName = uri.getFileName()
            file = File(dir, fileName)
            doc = NovelApi.getDocument(uri.toString(), ignoreHttpErrors = false, useProxy = false)
        } catch (e: Exception) {
            when (e) {
                is SocketException -> {
                    // Let's try this one more time
                    if (retryCount == 0) return downloadFile(element, dir, retryCount = 1)
                }
                is HttpStatusException -> {
                    //Do Nothing
                }
            }

            // Let's log all other exceptions
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
            Logs.warning(TAG, "convertDocToFile: Document:${doc.location()}, File: ${file.absolutePath}", e)
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

    open fun toggleTheme(isDark: Boolean, doc: Document): Document = toggleThemeDefault(isDark, doc)

    fun toggleThemeDefault(isDark: Boolean, doc: Document): Document {
        val fontFile = File(dataCenter.fontPath)
        val fontFamily = fontFile.name.substringBeforeLast(".")
        val nightModeTextBrightness = 87
        doc.head().getElementById("darkTheme")?.remove()
        doc.head().append(
            """
            <style id="darkTheme">
                @font-face {
                    font-family: $fontFamily;
                    src: url("$FILE_PROTOCOL${fontFile.path}");
                }
                html {
                    scroll-behavior: smooth;
                }
                body {
                    ${if (isDark) "background-color" else "color"}: #000;
                    ${if (isDark) "color" else "background-color"}: rgba(255, 255, 255, .$nightModeTextBrightness);
                    font-family: '$fontFamily';
                    line-height: 1.5;
                    padding: 20px;
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
                ${if (dataCenter.limitImageWidth) "img { max-width: 100%; }" else ""}
            </style>
            """.trimIndent()
        )

        return doc
    }

    open fun getLinkedChapters(doc: Document): ArrayList<String> = ArrayList()

    fun getLinkedChapters(sourceURL: String, contentElement: Element?): ArrayList<String> {
        val links = ArrayList<String>()
        val baseUrlDomain = sourceURL.toHttpUrlOrNull()?.topPrivateDomain()
        val otherLinks = contentElement?.select("a[href]")
        otherLinks?.forEach {
            // Other Share links
            if (it.hasAttr("title") && it.attr("title").contains("Click to share", true)) {
                return@forEach
            }

            val linkedUrl = it.absUrl("href").split("#").first()
            if (linkedUrl == sourceURL || links.contains(linkedUrl)) return@forEach

            try {
                // Check if URL is from chapter provider, only download from same domain
                val urlDomain = linkedUrl.toHttpUrlOrNull()?.topPrivateDomain()
                if (urlDomain == baseUrlDomain) {
                    links.add(linkedUrl)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return links
    }


    private fun processColorComponent(comp: String): Double {
        if (comp.endsWith("%")) return comp.substring(0, comp.length - 1).toDouble() / 100.0
        return comp.toDouble()
    }

    fun cleanCSSFromChildren(contentElement: Element?) {
        cleanClassAndIds(contentElement)
        contentElement?.children()?.forEach {
            cleanCSSFromChildren(it)
        }
    }

    fun cleanClassAndIds(contentElement: Element?) {
        if (contentElement == null) return
        contentElement.classNames()?.forEach { contentElement.removeClass(it) }

        if (contentElement.hasAttr("id"))
            contentElement.removeAttr("id")

        if (keepContentStyle) {
            return
        }

        if (dataCenter.keepTextColor && contentElement.hasAttr("style")) {
            fixStyleWhileRetainingColors(contentElement)
        } else {
            contentElement.removeAttr("style")
        }
    }

    private fun fixStyleWhileRetainingColors(contentElement: Element) {
        val nodeColor = getNodeColor(contentElement) ?: contentElement.removeAttr("style")
        contentElement.attr("style", "color: $nodeColor")
    }

    private fun getNodeColor(contentElement: Element): String? {
        val colorRegex = Regex("(?:^|;)\\s*color\\s*:\\s*(.*?)(?:;|\$)", RegexOption.IGNORE_CASE)
        val result = colorRegex.matchEntire(contentElement.attr("style")) ?: return null

        if (!dataCenter.alternativeTextColors || !dataCenter.isDarkTheme) {
            return result.groupValues[1]
        }

        try {
            val col = result.groupValues[1]
            // Since #RGB and #RGBA are valid CSS colors, handle hex values manually.
            // They expand from #RGBA to #RRGGBBAA, duplicating the 4 bits of corresponding compressed color.
            // Color.parseColor is unable to parse those.
            if (col.startsWith("#")) {
                when (col.length) {
                    4 -> {
                        // #RGB
                        val tmp = col.substring(1).toLong(16)
                        return invertColor(
                            tmp.and(0xf00).shr(4) + tmp.and(0xf00).shr(8),
                            tmp.and(0xf0) + tmp.and(0xf0).shr(4),
                            tmp.and(0xf) + tmp.and(0xf).shl(4), 0xff
                        )
                    }
                    5 -> {
                        // #RGBA
                        val tmp = col.substring(1).toLong(16)
                        return invertColor(
                            tmp.and(0xf000).shr(8) + tmp.and(0xf000).shr(12),
                            tmp.and(0xf00).shr(4) + tmp.and(0xf00).shr(8),
                            tmp.and(0xf0) + tmp.and(0xf0).shr(4),
                            tmp.and(0xf) + tmp.and(0xf).shl(4)
                        )
                    }
                    7 -> {
                        // #RRGGBB
                        val tmp = col.substring(1).toLong(16)
                        return invertColor(
                            tmp.and(0xff0000).shr(16),
                            tmp.and(0xff00).shr(8),
                            tmp.and(0xff),
                            0xff
                        )
                    }
                    9 -> {
                        // #RRGGBBAA
                        val tmp = col.substring(1).toLong(16)
                        return invertColor(
                            tmp.and(0xff000000).shr(24),
                            tmp.and(0xff0000).shr(16),
                            tmp.and(0xff00).shr(8),
                            tmp.and(0xff)
                        )
                    }
                    else -> {
                        // Most likely invalid color
                        return result.groupValues[1]
                    }
                }

            } else if (col.startsWith("rgb", true) || col.startsWith("hsl", true)) {
                // rgb/rgba/hsl/hsla functional notations
                val colorReg = Regex("(?:[,(]\\s*)([0-9\\-+.e]+%?)")
                var notationResult = colorReg.matchEntire(col)

                val compA = processColorComponent(notationResult!!.groupValues[1])
                notationResult = notationResult.next()
                val compB = processColorComponent(notationResult!!.groupValues[1])
                notationResult = notationResult.next()
                val compC = processColorComponent(notationResult!!.groupValues[1])
                notationResult = notationResult.next()
                val alpha = processColorComponent(notationResult?.groupValues?.get(1) ?: "1")

                return if (col.startsWith("rgb"))
                    invertColor(compA, compB, compC, alpha)
                else
                    "hsla($compA, ${compB * 100.0}%, ${(1.0 - compC) * 100.0}%, $alpha)"

            } else {
                val tmp = Color.parseColor(col).toLong()
                return invertColor(
                    tmp.and(0xff0000).shr(16),
                    tmp.and(0xff00).shr(8),
                    tmp.and(0xff),
                    tmp.and(0xff000000).shr(24)
                )
                // TODO: Use proper CSS-compliant color table, since Color utility is incomplete.
                // Ref: https://developer.mozilla.org/en-US/docs/Web/CSS/color_value
            }
        } catch (e: IllegalArgumentException) {
            // Do not modify color if Color.parseColor yield no result (valid CSS color, but Color can't parse it)
            return result.groupValues[1]
        } catch (e: NullPointerException) {
            // Most likely caused by functional notation having math in it.
            // Or hsl notation using deg/rad/turn postfixes in hue value
            return result.groupValues[1]
        }
    }

    /**
     * Performs RGB->HSL conversion and returns CSS HSLA color string with inverted lightness.
     * It could be done with Android capabilities, but it seems that Color utilities do not have HSL color space (only HSV).
     */
    private fun invertColor(red: Double, green: Double, blue: Double, alpha: Double): String {
        val rf = red / 255.0
        val gf = green / 255.0
        val bf = blue / 255.0
        val min = red.coerceAtMost(green).coerceAtMost(blue)
        val max = red.coerceAtLeast(green).coerceAtLeast(blue)
        val lightness = (min + max) / 2.0
        val hue: Double
        val saturation: Double
        if (min == max) {
            hue = 0.0
            saturation = 0.0
        } else {
            val diff = max - min
            saturation = if (lightness > 0.5) diff / (2.0 - max - min) else diff / (max + min)
            hue = (when (max) {
                red -> (green - blue) / diff + (if (green < blue) 6.0 else 0.0)
                green -> (blue - red) / diff + 2.0
                else -> (red - green) / diff + 4.0
            }) / 6.0
        }
        return "hsla(${hue * 360.0}, ${saturation * 100.0}%, ${(1 - lightness) * 100.0}%, $alpha)"
    }

    private fun invertColor(red: Long, green: Long, blue: Long, alpha: Long): String {
        return invertColor(red / 255.0, green / 255.0, blue / 255.0, alpha / 255.0)
    }

}

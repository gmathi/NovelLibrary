package io.github.gmathi.novellibrary.cleaner

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.model.QueryLookup
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
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.SocketException


open class HtmlCleaner protected constructor() {

    companion object {

        private const val TAG = "HtmlHelper"

        private val defaultLookups = listOf(
            QueryLookup("div.chapter-content"),
            QueryLookup("div.entry-content"),
            QueryLookup("div.elementor-widget-theme-post-content", appendTitleHeader = false),
            QueryLookup("article.hentry"),
            QueryLookup("div.hentry"),
            QueryLookup("div#chapter_body"),
            QueryLookup("article#releases"),
            QueryLookup("div.td-main-content"),
            QueryLookup("div#content"),
            QueryLookup("div.post-inner", appendTitleHeader = false),
            QueryLookup("div.blog-content"),
            QueryLookup("div#chapter-content"),
            QueryLookup("div.panel-body", appendTitleHeader = false),
            QueryLookup("div.post-entry"),
            QueryLookup("div.text-formatting"),
            QueryLookup("article.single__contents"),
            //QueryLookup("article.story-part"),
            QueryLookup("div#chapter"),
            //TODO: Xiaxia novel, needs more analysis to fix pre-formatting (jsoup not supporting)
            QueryLookup("section#StoryContent"),
            QueryLookup("div.content-container"),
            QueryLookup("article.article-content"),
            QueryLookup("div.page-content"),
            QueryLookup("div.legacy-journal") // Sample: deviantart journals (NU group: darksilencer)
        )

        fun getInstance(doc: Document, url: String = doc.location()): HtmlCleaner {
            when {
                url.contains(HostNames.WATTPAD) -> return WattPadCleaner()
                url.contains(HostNames.WUXIA_WORLD) -> return WuxiaWorldCleaner()
                url.contains(HostNames.CIRCUS_TRANSLATIONS) -> return CircusTranslationsCleaner()
                url.contains(HostNames.QIDIAN) -> return QidianCleaner()
                url.contains(HostNames.GOOGLE_DOCS) -> return GoogleDocsCleaner()
                url.contains(HostNames.BLUE_SILVER_TRANSLATIONS) -> return BlueSilverTranslationsCleaner()
                url.contains(HostNames.TUMBLR) -> return TumblrCleaner()
                url.contains(HostNames.BAKA_TSUKI) -> return BakaTsukiCleaner()
                url.contains(HostNames.SCRIBBLE_HUB) -> return ScribbleHubCleaner()
                url.contains(HostNames.NEOVEL) -> return NeovelCleaner()
                url.contains(HostNames.ACTIVE_TRANSLATIONS) -> return ActiveTranslationsCleaner()
            }

            val body = doc.body()
            val lookup = getQueryLookups().firstOrNull { body.select(it.query).isNotEmpty() }
            if (lookup != null) return GeneralQueryCleaner(url, lookup.query, appendTitle = lookup.appendTitleHeader)

            //Lastly let's check for cloud flare
            val contentElement = doc.body().getElementsByTag("a").firstOrNull { it.attr("href").contains("https://www.cloudflare.com/") && it.text().contains("DDoS protection by Cloudflare") }
            if (contentElement != null) return CloudFlareDDoSTagCleaner()

            return HtmlCleaner()
        }

        private fun getQueryLookups(): List<QueryLookup> {
            val lookups = dataCenter.customQueryLookups
            if (lookups.isNotBlank()) {
                return defaultLookups + lookups.split('\n').filter { it.isNotBlank() }.map { QueryLookup(it.trim()) }
            }
            return defaultLookups
        }

    }

    open var keepContentStyle = false
    open var keepContentIds = false
    open var keepContentClasses = false

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

    open fun removeCSS(doc: Document, clearHead : Boolean = true) {
        doc.getElementsByTag("link").remove()
        if (clearHead) {
            doc.head()?.getElementsByTag("style")?.remove()
            doc.head()?.getElementsByTag("link")?.remove()
        }
        doc.getElementById("custom-background-css")?.remove()
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
        if (uri.toString().contains("uploads/avatars")) return null
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

        val dayBackgroundColor = dataCenter.dayModeBackgroundColor
        val dayBackgroundColorTransparency = BigDecimal(dayBackgroundColor.alpha.toDouble() / 255).setScale(2, RoundingMode.HALF_EVEN)
        val dayTextColor = dataCenter.dayModeTextColor
        val dayTextColorTransparency =  BigDecimal(dayTextColor.alpha.toDouble() / 255).setScale(2, RoundingMode.HALF_EVEN)

        val nightBackgroundColor = dataCenter.nightModeBackgroundColor
        val nightBackgroundColorTransparency = BigDecimal(nightBackgroundColor.alpha.toDouble() / 255).setScale(2, RoundingMode.HALF_EVEN)
        val nightTextColor = dataCenter.nightModeTextColor
        val nightTextColorTransparency = BigDecimal(nightTextColor.alpha.toDouble() / 255).setScale(2, RoundingMode.HALF_EVEN)

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
                    background-color: ${
                        if (isDark)
                            "rgba(${nightBackgroundColor.red}, ${nightBackgroundColor.green}, ${nightBackgroundColor.blue}, $nightBackgroundColorTransparency)"
                        else
                            "rgba(${dayBackgroundColor.red}, ${dayBackgroundColor.green}, ${dayBackgroundColor.blue}, $dayBackgroundColorTransparency)"
                        };  
                    color: ${
                        if (isDark)
                            "rgba(${nightTextColor.red}, ${nightTextColor.green}, ${nightTextColor.blue}, $nightTextColorTransparency)"
                        else
                            "rgba(${dayTextColor.red}, ${dayTextColor.green}, ${dayTextColor.blue}, $dayTextColorTransparency)"
                        };
                    font-family: '$fontFamily';
                    line-height: 1.5;
                    padding: 20px;
                    text-align: left;
                }
                a {
                    color: rgba(${if (isDark) "135, 206, 250, .$nightTextColorTransparency" else "0, 0, 238, $dayTextColorTransparency" });
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
                ${if (dataCenter.limitImageWidth) "img { max-width: 100%; height: initial !important; }" else ""}
                img.full-size-image {
                    max-width: initial !important;
                    max-height: initial !important;
                    width: initial !important;
                    height: initial !important;
                }
                svg {
                    max-width: 100vw;
                }
            </style>
            """.trimIndent()
        )
        doc.body().append(
            """
            <script>
                function longPressStart(e) {
                    var img = e.currentTarget;
                    var t = e.changedTouches[0];
                    img._touch = t.identifier;
                    img._touchX = t.screenX;
                    img._touchY = t.screenY;
                    img._pressTime = e.timeStamp;
                }
                function longPressEnd(e) {
                    var img = e.currentTarget;
                    var t = e.changedTouches[0];
                    var dx = t.screenX - img._touchX;
                    var dy = t.screenY - img._touchY;
                    var radius = Math.max(t.radiusX||20, t.radiusY||20) * 2;
                    // Only accept touch zoom action when we don't scroll and we unpressed same finger that initiated the touch.
                    if (t.identifier === img._touch) {
                        var dt = e.timeStamp = img._pressTime;
                        if (dt > 600 && (dx*dx+dy*dy) < radius*radius) {
                            // Pressed for 0.6s
                            toggleZoom(e);
                        }
                        img._touch = null;
                    }
                }
                function toggleZoom(e) {
                    var img = e.currentTarget;
                    // Some websites use thumbnails + attribute to load images in full-screen mode
                    // Try to detect that and swap the image url to the full one.
                    var original = img.getAttribute("data-orig-file");
                    if (img.classList.toggle("full-size-image")) {
                        if (original && img.src != original) {
                            img._srcset = img.srcset;
                            img._thumbnail = img.src;
                            img.srcset = "";
                            img.src = original;
                        }
                    } else if (original && img._thumbnail) {
                        img.src = img._thumbnail;
                        img.srcset = img._srcset;
                    }
                }
                var images = document.querySelectorAll("img");
                for (var i = 0; i < images.length; i++) {
                    var img = images[i];
                    // Check if image is part of the link.
                    var parent = img.parentElement;
                    do {
                      if (parent.tagName == "A") break;
                       parent = parent.parentElement;
                    } while (parent);
                    
                    if (parent) {
                        // Image is part of the link, apply zoom toggle on long press.
                        img.addEventListener("touchstart", longPressStart);
                        img.addEventListener("touchend", longPressEnd);
                    } else {
                        // Image is not linking anywhere - apply zoom on click.
                        img.addEventListener("click", toggleZoom);
                    }
                }
                
                // Attempt to mitigate SVGs being extremely big by limiting their size based off `viewBox` attribute.
                var svg = document.querySelectorAll("svg");
                for (var i = 0; i < svg.length; i++) {
                    var s = svg[i];
                    s.style.maxWidth = "min(100vw, " + s.viewBox.baseVal.width + "px)";
                }
            </script>
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
        if (!keepContentClasses)
            contentElement.classNames()?.forEach { contentElement.removeClass(it) }

        if (!keepContentIds && contentElement.hasAttr("id"))
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

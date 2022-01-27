package io.github.gmathi.novellibrary.cleaner

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.webkit.URLUtil
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import io.github.gmathi.novellibrary.model.other.*
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.online.HttpSource
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.WebPageDocumentFetcher
import io.github.gmathi.novellibrary.util.*
import io.github.gmathi.novellibrary.util.Constants.FILE_PROTOCOL
import io.github.gmathi.novellibrary.util.lang.writableFileName
import io.github.gmathi.novellibrary.util.network.getFileName
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Tag
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.SocketException


open class HtmlCleaner protected constructor() {

    companion object {
        // Usual text used for links leading to actual chapter text
        private val genericMainContentUrlText = listOf(
            "Enjoy", "Enjoy.", "Enjoy~",
            "Click here to read the chapter",
            "Click here for chapter",
            "Read chapter",
            "Read the chapter",
            "Continue reading",
        )

        // Fairly generic selectors for specific content types
        private const val genericCommentsSubquery = "#comments,.comments,#disqus_thread"
        private const val genericShareSubquery = ".sd-block,.sharedaddy"
        private const val genericMetaSubquery = ".byline,.posted-on,.cat-links,.tags-links,.entry-author,.post-date,.post-info,.post-meta,.entry-meta"

        private val imageAttributes = listOf(
            "data-orig-file",
            "data-large-file",
            "lazy-src",
            "src",
            "data-lazy-src",
            "data-medium-file",
            "data-small-file",
            "data-srcset",
            "srcset"
        )

        private val defaultSelectorQueries = listOf(
            // Comprehensive selectors
            // Note: Subquery ordering is important, one that are attached to the end-results are attached in that order.
            // Hence the following order is recommended:
            // RHeader, RContent, RPage, RFooter, RNavigation, RMeta, RShare, RComments
            // Make sure to put host-restricted queries first, since they likely trigger some other selector.

            SelectorQuery(
                ".nv__main", host = "activetranslations.xyz", subQueries = listOf(
                    SelectorSubQuery(".nv-page-title", SubqueryRole.RHeader, optional = false, multiple = false),
                    SelectorSubQuery("div[class*='entry-content']", SubqueryRole.RContent, optional = false, multiple = false),
                    SelectorSubQuery(".nnl_container", SubqueryRole.RNavigation, optional = true, multiple = false),
                    SelectorSubQuery("#comments", SubqueryRole.RComments, optional = true, multiple = false),
                    SelectorSubQuery("div[class*='entry-content']>style", SubqueryRole.RWhitelist, optional = false, multiple = true),
                ), keepContentClasses = true, customCSS = """
                *,*::before,*::after {
                    user-select: initial !important;
                    
                    top: initial!important;
                    bottom: initial!important;
                    left: initial!important;
                    right: initial!important;
                }
            """.trimIndent()
            ),
            SelectorQuery(
                ".content-area", host = "a-t.nu", subQueries = listOf(
                    SelectorSubQuery("#chapter-heading", SubqueryRole.RHeader, optional = false, multiple = false),
                    SelectorSubQuery(".reading-content", SubqueryRole.RContent, optional = false, multiple = false),
                    SelectorSubQuery(".manga-discussion", SubqueryRole.RComments, optional = true, multiple = false),
                    // Contains css with text pseudo-elements.
                    SelectorSubQuery(".reading-content style", SubqueryRole.RWhitelist, optional = false, multiple = true),
                    SelectorSubQuery(".wp-community-credits", SubqueryRole.RBlacklist, optional = true, multiple = true),
                ), keepContentClasses = true, customCSS = """
                *,*::before,*::after {
                    user-select: initial !important;
                    
                    top: initial!important;
                    bottom: initial!important;
                    left: initial!important;
                    right: initial!important;
                }
            """.trimIndent()
            ),

            // Make lazytranslations more bearable, ref -> https://lazytranslations.com/tl/oc/oc1/
            SelectorQuery(".elementor-inner", host="lazytranslations.com", subQueries = listOf(
                SelectorSubQuery(".entry-header h1.entry-title", SubqueryRole.RHeader, optional = false, multiple = false),
                SelectorSubQuery("#innerbody,.elementor-text-editor", SubqueryRole.RContent, optional = false, multiple = false),
                // Horrible abomination
                SelectorSubQuery(".elementor-inner>.elementor-section:nth-child(3)", SubqueryRole.RNavigation, optional = true, multiple = false),
                SelectorSubQuery("#innerbody>div>p>span[style*='color: #ffffff'],.elementor-text-editor div>p>span[style*='color: #ffffff'],.lazyt-announcement", SubqueryRole.RBlacklist, optional = true, multiple = true)
            )),
            SelectorQuery(".post-content", host="lazytranslations.com", subQueries = listOf(
                SelectorSubQuery(".entry-header h1.entry-title", SubqueryRole.RHeader, optional = false, multiple = false),
                SelectorSubQuery(".entry-content", SubqueryRole.RContent, optional = false, multiple = false),
                SelectorSubQuery(".lazyt-announcement", SubqueryRole.RBlacklist, optional = true, multiple = true),
                SelectorSubQuery(".post-content figure.wp-block-image>a", SubqueryRole.RRealChapter, optional = true, multiple = false),
                SelectorSubQuery(".post-content figure.wp-block-image>a>img", SubqueryRole.RProcess, optional = true, multiple = false,
                    extraProcessing = listOf(
                        SubQueryProcessingCommandInfo(SubQueryProcessingCommand.AddAttribute, "alt=my image")
                    )
                ),
            )),

            // Scrambled fonts
            SelectorQuery("div.entry-content", host = "secondlifetranslations.com", subQueries = listOf(
                SelectorSubQuery(".entry-header .entry-title", SubqueryRole.RHeader, optional = false, multiple = false),
                SelectorSubQuery("div.entry-content", SubqueryRole.RContent, optional = true, multiple = false),
            ), keepContentClasses = true, customCSS = """
                @font-face {
                    font-family: 'open_sansscrambled';
                    src: url('https://secondlifetranslations.com/wp-content/plugins/slt-scramble-text/public/fonts/opensans-scrambled-webfont.eot');
                    src: url('https://secondlifetranslations.com/wp-content/plugins/slt-scramble-text/public/fonts/opensans-scrambled-webfont.eot?#iefix') format('embedded-opentype'),
                         url('https://secondlifetranslations.com/wp-content/plugins/slt-scramble-text/public/fonts/opensans-scrambled-webfont.woff2') format('woff2'),
                         url('https://secondlifetranslations.com/wp-content/plugins/slt-scramble-text/public/fonts/opensans-scrambled-webfont.woff') format('woff'),
                         url('https://secondlifetranslations.com/wp-content/plugins/slt-scramble-text/public/fonts/opensans-scrambled-webfont.ttf') format('truetype'),
                         url('https://secondlifetranslations.com/wp-content/plugins/slt-scramble-text/public/fonts/opensans-scrambled-webfont.svg#open_sansscrambled') format('svg');
                    font-weight: normal;
                    font-style: normal;
                }

                span.scrmbl {
                    font-family: 'open_sansscrambled' !important;
                }

                span.scrmbl .scrmbl-ent {
                    font-family: "Open Sans", sans-serif !important;
                }

                .scrmbl-ent {
                    visibility:hidden;
                }

                .scrmbl-disclaimer {
                    color: transparent;
                    height:1px;
                    margin:0;
                    padding:0;
                    overflow:hidden;
                }
            """.trimIndent()),

            SelectorQuery(".reading-content", host="dragontea.ink", subQueries = listOf(
                SelectorSubQuery("#chapter-heading", SubqueryRole.RHeader, optional = false, multiple = false),
                SelectorSubQuery(".reading-content", SubqueryRole.RContent, optional = true, multiple = false),
            ), customCSS = """
                @font-face {
                  font-family: 'DragonTea';
                  src: url(https://dragontea.ink/wp-content/themes/madara-child/font/DragonTea-Regular.eot);
                  src: url(https://dragontea.ink/wp-content/themes/madara-child/font/DragonTea-Regular.eot?#iefix) format('embedded-opentype'), url(//dragontea.ink/wp-content/themes/madara-child/font/DragonTea-Regular.woff2) format('woff2'), url(//dragontea.ink/wp-content/themes/madara-child/font/DragonTea-Regular.woff) format('woff'), url(//dragontea.ink/wp-content/themes/madara-child/font/DragonTea-Regular.ttf) format('truetype'), url(//dragontea.ink/wp-content/themes/madara-child/font/DragonTea-Regular.svg#DragonTea-Regular) format('svg');
                  font-weight: normal;
                  font-style: normal;
                  font-display: swap!important;
                }
                div[data-role=RContent] {
                    font-family: 'DragonTea'!important;
                }
            """.trimIndent()),

            // They use annoying 2-page splitting: https://tigertranslations.org/2018/08/31/jack-of-all-trades-1/
            SelectorQuery(".the-content", host="tigertranslations.org", subQueries = listOf(
                SelectorSubQuery("#chapter-heading,.entry-header .entry-title", SubqueryRole.RHeader, optional = false, multiple = false),
                SelectorSubQuery(".the-content", SubqueryRole.RContent, optional = true, multiple = false),
                SelectorSubQuery("a:containsOwn(PAGE)", SubqueryRole.RPage, optional = true, multiple = true),
                SelectorSubQuery("a:containsOwn(NEXT CHAPTER)", SubqueryRole.RChapterLink, optional = true, multiple = true),
                SelectorSubQuery("$genericMetaSubquery,.post-meta-container,.taxonomies", SubqueryRole.RMeta, optional = true, multiple = true),
                SelectorSubQuery("$genericShareSubquery, .jp-relatedposts, #jp-relatedposts", SubqueryRole.RShare, optional = true, multiple = true),
                SelectorSubQuery(genericCommentsSubquery, SubqueryRole.RComments, optional = true, multiple = false),
            )),

            // Github, DIY Translations as an example
            SelectorQuery("div#readme", host = "github.com"),

            // https://novelonomicon.com/
            SelectorQuery(
                "div#content[role='main']", subQueries = listOf(
                    SelectorSubQuery("h1.entry-title", SubqueryRole.RHeader, optional = true, multiple = false),
                    SelectorSubQuery("div.entry-content", SubqueryRole.RContent, optional = true, multiple = false),
                    SelectorSubQuery(".entry-footer,.entry-bottom", SubqueryRole.RFooter, optional = true, multiple = false),
                    SelectorSubQuery(genericMetaSubquery, SubqueryRole.RMeta, optional = true, multiple = true),
                    SelectorSubQuery(".post-navigation", SubqueryRole.RNavigation, optional = true, multiple = false),
                    SelectorSubQuery(genericShareSubquery, SubqueryRole.RShare, optional = true, multiple = true),
                    SelectorSubQuery(genericCommentsSubquery, SubqueryRole.RComments, optional = true, multiple = false),
                )
            ),

            // Most common in wordpress-hosted websites, but also nicely matches a bunch of others.
            SelectorQuery(
                "div.entry-content", subQueries = listOf(
                    SelectorSubQuery(".entry-title,.entry-header", SubqueryRole.RHeader, optional = true, multiple = false),
                    SelectorSubQuery("div.entry-content", SubqueryRole.RContent, optional = true, multiple = false),
                    SelectorSubQuery(".entry-footer,.entry-bottom", SubqueryRole.RFooter, optional = true, multiple = false),
                    SelectorSubQuery(genericMetaSubquery, SubqueryRole.RMeta, optional = true, multiple = true),
                    SelectorSubQuery(".post-navigation", SubqueryRole.RNavigation, optional = true, multiple = false),
                    SelectorSubQuery(genericShareSubquery, SubqueryRole.RShare, optional = true, multiple = true),
                    SelectorSubQuery(genericCommentsSubquery, SubqueryRole.RComments, optional = true, multiple = false),
                )
            ),
            // Alternative version where instead of entry- it has post- prefixes
            // Also common for tumblr
            SelectorQuery(
                "div.post-content", subQueries = listOf(
                    SelectorSubQuery(".post-title,.post-header", SubqueryRole.RHeader, optional = true, multiple = false),
                    SelectorSubQuery("div.post-content", SubqueryRole.RContent, optional = true, multiple = false),
                    SelectorSubQuery(".post-footer,.post-bottom", SubqueryRole.RFooter, optional = true, multiple = false),
                    SelectorSubQuery("$genericMetaSubquery,.post-meta-container", SubqueryRole.RMeta, optional = true, multiple = true),
                    SelectorSubQuery(".post-navigation", SubqueryRole.RNavigation, optional = true, multiple = false),
                    SelectorSubQuery(genericShareSubquery, SubqueryRole.RShare, optional = true, multiple = true),
                    SelectorSubQuery(genericCommentsSubquery, SubqueryRole.RComments, optional = true, multiple = false),
                )
            ),

            // Modern tumblr
            SelectorQuery(
                "div#content", host = "tumblr.com", subQueries = listOf(
                    SelectorSubQuery("div.entry>.body", SubqueryRole.RContent, optional = true, multiple = false),
                    SelectorSubQuery(".posttitle", SubqueryRole.RHeader, optional = true, multiple = false),
                    SelectorSubQuery("#jp-post-flair,.wpcnt,.permalink", SubqueryRole.RMeta, optional = true, multiple = true),
                    SelectorSubQuery(genericCommentsSubquery, SubqueryRole.RComments, optional = true, multiple = false),
                )
            ),

            // Legacy TumblrCleaner. Boy, tumblr has so many variations.
            SelectorQuery(
                "div.textpostbody", host = "tumblr.com", subQueries = listOf(
                    SelectorSubQuery(".textposttitle", SubqueryRole.RHeader, optional = true, multiple = false),
                    SelectorSubQuery("", SubqueryRole.RContent, optional = true, multiple = false),
                    SelectorSubQuery("#jp-post-flair,.wpcnt,.permalink", SubqueryRole.RMeta, optional = true, multiple = true),
                    SelectorSubQuery(genericCommentsSubquery, SubqueryRole.RComments, optional = true, multiple = false),
                )
            ),

            // Legacy selectors
            SelectorQuery("div.chapter-content"),
            SelectorQuery("div.entry-content"),
            SelectorQuery("div.elementor-widget-theme-post-content", appendTitleHeader = false),
            SelectorQuery("article.hentry"),
            SelectorQuery("div.hentry"),
            SelectorQuery("div#chapter_body"),
            SelectorQuery("article#releases"),
            SelectorQuery("div.td-main-content"),
            SelectorQuery("div#content"),
            SelectorQuery("div.post-inner", appendTitleHeader = false),
            SelectorQuery("div.blog-content"),
            SelectorQuery("div#chapter-content"),
            SelectorQuery("div.panel-body", appendTitleHeader = false),
            SelectorQuery("div.post-entry"),
            SelectorQuery("div.text-formatting"),
            SelectorQuery("article.single__contents"),
            //SelectorQuery("article.story-part"),
            SelectorQuery("div#chapter"), // HostedNovel
            SelectorQuery("div.chapter"), //HostedNovel
            //TODO: Xiaxia novel, needs more analysis to fix pre-formatting (jsoup not supporting)
            SelectorQuery("section#StoryContent"),
            SelectorQuery("div.content-container"),
            SelectorQuery("article.article-content"),
            SelectorQuery("div.page-content"),
            SelectorQuery("div.legacy-journal"), // Sample: deviantart journals (NU group: darksilencer)
            SelectorQuery("article.entry-content"), //GitHub
            SelectorQuery("article"),
        )

        private const val TAG = "HtmlHelper"

        fun getInstance(doc: Document, url: String = doc.location()): HtmlCleaner {
            when {
                url.contains(HostNames.WATTPAD) -> return WattPadCleaner()
                url.contains(HostNames.WUXIA_WORLD) -> return WuxiaWorldCleaner()
                url.contains(HostNames.QIDIAN) -> return QidianCleaner()
                url.contains(HostNames.GOOGLE_DOCS) -> return GoogleDocsCleaner()
                url.contains(HostNames.BLUE_SILVER_TRANSLATIONS) -> return BlueSilverTranslationsCleaner()
                url.contains(HostNames.BAKA_TSUKI) -> return BakaTsukiCleaner()
                url.contains(HostNames.SCRIBBLE_HUB) -> return ScribbleHubCleaner()
                url.contains(HostNames.NEOVEL) -> return NeovelCleaner()
                url.contains(HostNames.CHRYSANTHEMUMGARDEN) -> return ChrysanthemumgardenCleaner()
            }

            val body = doc.body()
            val lookup = getSelectorQueries().firstOrNull {
                if ((it.host == null || url.contains(it.host)) && body.select(it.selector).isNotEmpty()) {
                    // Check non-optional subqueries to ensure we match the correct website.
                    // TODO: Optimise with running all queries at once and storing them, instead of rerunning them a second time inside cleaner
                    //if (it.host != null) Log.d(TAG, "${it.host}, ${it.selector}")
                    if (it.subQueries.isEmpty()) true
                    else it.subQueries.all { sub ->
                        //if (it.host != null) Log.d(TAG, "${sub.selector} -> ${sub.optional} : ${body.select(sub.selector).isNotEmpty()}")
                        sub.optional || body.select(sub.selector).isNotEmpty()
                    }
                } else false
            }
            if (lookup != null) return GenericSelectorQueryCleaner(url, lookup)

            //Lastly let's check for cloud flare
            val contentElement = doc.body().getElementsByTag("a").firstOrNull { it.attr("href").contains("https://www.cloudflare.com/") && it.text().contains("DDoS protection by Cloudflare") }
            if (contentElement != null) return CloudFlareDDoSTagCleaner()

            return HtmlCleaner()
        }

        private fun getSelectorQueries(): List<SelectorQuery> {
            val dataCenter: DataCenter by injectLazy()
            val htmlCleanerSelectorQueries = dataCenter.htmlCleanerSelectorQueries
            htmlCleanerSelectorQueries.addAll(defaultSelectorQueries)

            val userSpecifiedSelectorQueries = dataCenter.userSpecifiedSelectorQueries
            if (userSpecifiedSelectorQueries.isNotBlank()) {
                htmlCleanerSelectorQueries.addAll(0, userSpecifiedSelectorQueries.split('\n').filter { it.isNotBlank() }.map { SelectorQuery(it.trim()) })
            }
            return htmlCleanerSelectorQueries
        }
    }

    val dataCenter: DataCenter by injectLazy()
    open var keepContentStyle = false
    open var keepContentIds = true
    open var keepContentClasses = false

    fun downloadResources(doc: Document, novelDir: File) {
        // removeJS(doc)
        downloadCSS(doc, novelDir)
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

    open fun removeCSS(doc: Document, clearHead: Boolean = true) {
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
            val cssFile = downloadOtherFiles(element, downloadDir)
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

    open fun downloadOtherFiles(element: Element, dir: File, retryCount: Int = 0): File? {
        val uri = Uri.parse(element.absUrl("href"))
        val file: File
        val doc: Document
        try {
            if (uri.scheme == null || uri.host == null) throw Exception("Invalid URI: $uri")
            val fileName = uri.getFileName()
            file = File(dir, fileName)
            doc = WebPageDocumentFetcher.document(uri.toString(), useProxy = false)
        } catch (e: Exception) {
            when (e) {
                is SocketException -> {
                    // Let's try this one more time
                    if (retryCount == 0) return downloadOtherFiles(element, dir, retryCount = 1)
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
        val elements = doc.getElementsByTag("img")
        for (element in elements) {
            val imageFile = getImageFile(element, novelDir)
            if (imageFile != null) {
                if (!imageFile.exists())
                    downloadImage(element, imageFile)
                cleanImageTag(element)
                element.attr("src", "./${imageFile.name}")
            }
        }
    }

    open fun getImageUrl(element: Element, absolute: Boolean = false): String? {
        val attr = imageAttributes.firstOrNull { element.hasAttr(it) }
        return when {
            attr == null -> null
            attr.endsWith("srcset") -> {
                val src = element.attr(attr).substringAfterLast(',').trim().substringBeforeLast(' ')
                if (absolute) {
                    element.attr("_srcset", src)
                    val ret = element.absUrl("_srcset")
                    element.removeAttr("_srcset")
                    ret
                } else {
                    src
                }
            }
            else ->
                if (absolute) element.absUrl(attr)
                else element.attr(attr)
        }
    }

    open fun cleanImageTag(element: Element) {
        element.removeAttr("data-orig-file")
        element.removeAttr("data-large-file")
        element.removeAttr("lazy-src")
        element.removeAttr("src")
        element.removeAttr("data-lazy-src")
        element.removeAttr("data-medium-file")
        element.removeAttr("data-small-file")
        element.removeAttr("data-srcset")
        element.removeAttr("srcset")
    }

    open fun getImageFile(element: Element, dir: File): File? {
        val uri = Uri.parse(getImageUrl(element, true) ?: return null)
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
        val uri = Uri.parse(getImageUrl(element, true) ?: return null)
        if (uri.toString().contains("uploads/avatars")) return null
        try {
            val response = Jsoup.connect(uri.toString()).userAgent(HttpSource.DEFAULT_USER_AGENT).ignoreContentType(true).execute()
            val bytes = response.bodyAsBytes()
            val bitmap = Utils.getImage(bytes)
            val os = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
        } catch (e: Exception) {
            Logs.error(TAG, "Exception Downloading Image: $uri", e)
            return null
        }
        return file
    }

    open fun getTitle(doc: Document): String? = doc.head().getElementsByTag("title").text()

    open fun toggleTheme(isDark: Boolean, doc: Document): Document = toggleThemeDefault(isDark, doc)

    private fun toggleThemeDefault(isDark: Boolean, doc: Document): Document {
        val fontFile = File(dataCenter.fontPath)
        val fontFamily = fontFile.name.substringBeforeLast(".")

        val dayBackgroundColor = dataCenter.dayModeBackgroundColor
        val dayBackgroundColorTransparency = BigDecimal(dayBackgroundColor.alpha.toDouble() / 255).setScale(2, RoundingMode.HALF_EVEN)
        val dayTextColor = dataCenter.dayModeTextColor
        val dayTextColorTransparency = BigDecimal(dayTextColor.alpha.toDouble() / 255).setScale(2, RoundingMode.HALF_EVEN)

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
                    overflow-wrap: break-word;
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
                    color: rgba(${if (isDark) "135, 206, 250, .$nightTextColorTransparency" else "0, 0, 238, $dayTextColorTransparency"});
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
                .footnote, .footnote+span {
                    border-bottom: 1px dashed;
                }
                .footnote+span::before {
                    content: " ";
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
                
                // Footnote integration QOL
                function makeFootnote(tag, text) {
                    if (tag.classList.contains("footnote") || tag._footnoteSource) return;
                    tag.classList.add("footnote");
                    var disp = document.createElement("span");
                    disp.innerHTML = text;
                    disp.style.display = "none";
                    tag.insertAdjacentElement("afterend", disp);
                    
                    function toggleFootnote(e) {
                        e.preventDefault();
                        e.stopImmediatePropagation();
                        if (disp.style.display == "none") disp.style.display = "";
                        else disp.style.display = "none";
                    }
                    tag.addEventListener("click", toggleFootnote);
                    disp.addEventListener("click", toggleFootnote);
                }
                // Footnotes
                var footnotes = document.querySelectorAll("sup");
                for (var i = 0; i < footnotes.length; i++) {
                    var parent = footnotes[i];
                    var anchor = parent.querySelector("a");
                    if (anchor && new URL(parent.href).hash != "") anchor = null;
                    
                    while (parent && !parent.title) {
                        if (!anchor && parent.tagName == "A" && new URL(parent.href).hash != "") anchor = parent;
                        parent = parent.parentElement;
                    }
                    if (parent) {
                        makeFootnote(parent, parent.title);
                    } else if (anchor) {
                        // footnote doesn't have a title with footnote contents: Attempt to find the footnote data
                        var footnoteBase = document.querySelector(new URL(anchor.href).hash);
                        if (footnoteBase) {
                            var nextEl = footnoteBase.nextSibling;
                            footnoteBase._footnoteSource = true;
                            if (nextEl && (footnoteBase.textContent == "" || footnoteBase.textContent.trim().length <= anchor.textContent.trim().length)) {
                                makeFootnote(anchor, nextEl.textContent);
                            } else {
                                makeFootnote(anchor, footnoteBase.textContent);
                            }
                        }
                    }
                }
                // <abbr> support
                var abbrs = document.querySelectorAll("a[title][href^='#'],abbr[title]");
                for (var i = 0; i < abbrs.length; i++) {
                    makeFootnote(abbrs[i], abbrs[i].title);
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

    open fun getLinkedChapters(doc: Document): ArrayList<LinkedPage> = ArrayList()

    fun getLinkedChapters(sourceURL: String, contentElement: Element?): ArrayList<LinkedPage> {
        val links = ArrayList<LinkedPage>()
        val baseUrlDomain = sourceURL.toHttpUrlOrNull()?.topPrivateDomain()
        val otherLinks = contentElement?.select("a[href]")
        otherLinks?.forEach {
            // Other Share links
            if ((it.hasAttr("title") && it.attr("title").contains("Click to share", true)) ||
                it.attr("data-role") == "RChapterLink") {
                return@forEach
            }

            val linkedUrl = it.absUrl("href").split("#").first()
            if (linkedUrl == sourceURL || links.find { l -> l.href == linkedUrl } != null) return@forEach

            try {
                // Check if URL is from chapter provider, only download from same domain
                val urlDomain = linkedUrl.toHttpUrlOrNull()?.topPrivateDomain()
                if (urlDomain == baseUrlDomain) {
                    var text = it.text().trim()
                    if (text.isEmpty()) {
                        text = it.selectFirst("img")?.let { img ->
                            if (img.hasAttr("title")) img.attr("title")
                            else "(image url)"
                        } ?: linkedUrl
                    }
                    val isMainContent = genericMainContentUrlText.find { cmp -> cmp.equals(text, true) } != null ||
                            Regex("""Chapter \d+""", RegexOption.IGNORE_CASE).containsMatchIn(text) ||
                            it.attr("data-role") == "RBuffer" || it.attr("data-role") == "RRealChapter"
                    links.add(LinkedPage(linkedUrl, text, isMainContent))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return links
    }

    fun linkify(element: Element) {
        if (!dataCenter.linkifyText) return
        val reg = Regex("""^\s*(https?://[^\s]+)(?:$|\s)""")
        element.getElementsMatchingOwnText(reg.toPattern()).forEach { el ->
            if (el.tagName() != "a" && el.parents().find { it.tagName() == "a" } == null) // Ensure we don't linkify what is already a link.
            el.textNodes().forEach { node ->
                val text = node.wholeText
                reg.find(node.wholeText)?.let { result ->
                    val group = result.groups[1]!!
                    if (URLUtil.isValidUrl(group.value)) {
                        node.text(text.removeRange(group.range))
                        val anchor = Element(Tag.valueOf("a"), node.baseUri())
                        anchor.attr("href", group.value)
                        anchor.text(group.value)
                        node.before(anchor)
                    }
                }
            }
        }
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

        when (contentElement.tagName()) {
            "img" -> {
                // Fix images that use data- and lazy-src attributes to load
                contentElement.attr("src", getImageUrl(contentElement))
                // Some websites use srcset to "hide" images from scrapers, and sometimes those images are links to actual chapter.
                // Example: lazytranslations use a 1x1 gif image to hide them.
                contentElement.removeAttr("srcset")
            }
            "div", "span" -> {
                // Clean up "Advertisements" divs that contain nothing else.
                if (contentElement.childrenSize() == 0 && contentElement.ownText().equals("Advertisements", true)) {
                    contentElement.remove()
                    return
                }
            }
        }

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

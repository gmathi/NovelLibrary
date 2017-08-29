package io.github.gmathi.novellibrary.cleaner

import android.graphics.Bitmap
import android.net.Uri
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.getFileName
import io.github.gmathi.novellibrary.util.writableFileName
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File
import java.io.FileOutputStream


open class HtmlHelper protected constructor() {

    private val TAG = "HtmlHelper"

    companion object {
        fun getInstance(host: String): HtmlHelper {
            when {

                host.contains(HostNames.ROYAL_ROAD) -> return RoyalRoadHelper()
                host.contains(HostNames.WUXIA_WORLD) -> return WuxiaWorldHelper()
                host.contains(HostNames.CIRCUS_TRANSLATIONS) -> return CircusTranslationsHelper()
                host.contains(HostNames.QIDIAN) -> return QidianHelper()
                host.contains(HostNames.GOOGLE_DOCS) -> return GoogleDocsCleaner()

            // 'entry-content' - 'div' tag cleaner
                host.contains(HostNames.KOBATOCHAN) -> return EntryContentTagCleaner()
                host.contains(HostNames.MOON_BUNNY_CAFE) -> return EntryContentTagCleaner()
                host.contains(HostNames.LIGHT_NOVEL_TRANSLATIONS) -> return EntryContentTagCleaner()
                host.contains(HostNames.SOUSETSUKA) -> return EntryContentTagCleaner()
                host.contains(HostNames.FANTASY_BOOKS) -> return EntryContentTagCleaner()

            // "WordPress" Sites cleaner
                host.contains(HostNames.BLUE_SILVER_TRANSLATIONS) -> return BlueSilverTranslationsHelper()
                host.contains(HostNames.WORD_PRESS) -> return WordPressHelper()
                host.contains(HostNames.PRINCE_REVOLUTION) -> return WordPressHelper()


            // "Tumblr" Sites Cleaner
                host.contains(HostNames.TUMBLR) -> return TumblrCleaner()

            //Generic Class Cleaners
                host.contains(HostNames.GRAVITY_TALES) -> return GeneralClassTagHelper(HostNames.GRAVITY_TALES, "article", "hentry")
                host.contains(HostNames.VOLARE_NOVELS) -> return GeneralClassTagHelper(HostNames.VOLARE_NOVELS, "article", "hentry")
                host.contains(HostNames.SKY_WOOD_TRANSLATIONS) -> return GeneralClassTagHelper(HostNames.SKY_WOOD_TRANSLATIONS, "div", "hentry")

            //Generic Id Cleaners
                host.contains(HostNames.LIBER_SPARK) -> return GeneralIdTagHelper("div", "chapter_body")

            }
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
            if (uri.scheme == null || uri.host == null) throw Exception("Invalid URI: " + uri.toString())
            val fileName = uri.getFileName()
            file = File(dir, fileName)
            doc = NovelApi().getDocumentWithUserAgentIgnoreContentType(uri.toString())
        } catch (e: Exception) {
            Utils.warning(TAG, "Uri: $uri", e)
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
            Utils.warning(TAG, "convertDocToFile: ${file.name}", e)
            return null
        }
        return file
    }

    open fun downloadImages(doc: Document, novelDir: File) {
        val elements = doc.getElementsByTag("img").filter { element -> element.hasAttr("src") }
        for (element in elements) {
            val imageFile = downloadImage(element, novelDir)
            if (imageFile != null) {
                element.removeAttr("src")
                element.attr("src", "./${imageFile.name}")
            }
        }
    }

    open fun downloadImage(element: Element, dir: File): File? {
        val uri = Uri.parse(element.absUrl("src"))
        val file: File
        try {
            if (uri.scheme == null || uri.host == null) throw Exception("Invalid URI: " + uri.toString())
            val fileName = uri.lastPathSegment.writableFileName()
            file = File(dir, fileName)
            val response = Jsoup.connect(uri.toString()).userAgent(HostNames.USER_AGENT).ignoreContentType(true).execute()
            val bytes = response.bodyAsBytes()
            val bitmap = Utils.getImage(bytes)
            val os = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
        } catch (e: Exception) {
            Utils.debug(TAG, "Exception Downloading Image: $uri")
            return null
        }
        return file
    }

    fun getTitle(doc: Document): String? {
        return doc.head().getElementsByTag("title").text()
    }

    open fun toggleTheme(isDark: Boolean, doc: Document): Document {
        return doc
    }

    fun toggleThemeDefault(isDark: Boolean, doc: Document): Document {
//        val fontName = "lobster_regular.ttf"
        val fontName = "source_sans_pro_regular.ttf"
        val fontFamily = fontName.split(".")[0]
        val nightModeTextBrightness = 8
        if (isDark) {
            doc.head().getElementById("darkTheme")?.remove()
            doc.head().append("" +
                "<style id=\"darkTheme\">" +
                "@font-face { font-family: $fontFamily; src: url(\"file:///android_asset/fonts/$fontName\") } \n" +
                "body { background-color:#000000; color:rgba(255, 255, 255, 0.$nightModeTextBrightness); font-family: '$fontFamily'; line-height: 1.5; padding:20px;} " +
                "</style> ")
        } else {
            doc.head().getElementById("darkTheme")?.remove()
            doc.head().append("" +
                "<style id=\"darkTheme\">" +
                "@font-face { font-family: $fontFamily; src: url(\"file:///android_asset/fonts/$fontName\") } \n" +
                "body { background-color:rgba(255, 255, 255, 0.$nightModeTextBrightness); color:#000000; font-family: '$fontFamily';; line-height: 1.5; padding:20px;} " +
                "</style> ")
        }

        return doc
    }

    open fun getLinkedChapters(doc: Document): ArrayList<String> {
        return ArrayList()
    }

    fun cleanClassAndIds(contentElement: Element?) {
        contentElement?.classNames()?.forEach { contentElement.removeClass(it) }
        contentElement?.removeAttr("style")
        if (contentElement != null && contentElement.hasAttr("id"))
            contentElement.removeAttr("id")
    }

    fun cleanCSSFromChildren(contentElement: Element?) {
        cleanClassAndIds(contentElement)
        contentElement?.children()?.forEach {
            cleanClassAndIds(it)
            cleanCSSFromChildren(it)
        }
    }
}
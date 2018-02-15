package io.github.gmathi.novellibrary.cleaner

import android.net.Uri
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.network.HostNames
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File

class GoogleDocsCleaner : HtmlHelper() {

    override fun additionalProcessing(doc: Document) {
        removeCSS(doc)
        doc.getElementsByTag("link")?.remove()
        doc.getElementsByTag("style")?.remove()
        cleanAttributes(doc.body())
    }

    private fun cleanAttributes(contentElement: Element?) {
        if (contentElement?.tagName() != "a" || contentElement.tagName() != "img")
            contentElement?.clearAttributes()
        contentElement?.children()?.forEach {
            cleanAttributes(it)
        }
    }


    override fun downloadImage(element: Element, dir: File): File? {
        val uri = Uri.parse(element.attr("src"))
        return if (uri.toString().contains("uploads/avatars")) null
        else super.downloadImage(element, dir)
    }

    override fun toggleTheme(isDark: Boolean, doc: Document): Document {

            var fontName = "source_sans_pro_regular.ttf"
            var fontUrl =  "/android_asset/fonts/$fontName"

            val fontFile = File(dataCenter.fontPath)
            if (fontFile.exists()) {
                fontName = fontFile.name
                fontUrl = fontFile.path
            }

            val fontFamily = fontName.substring(0, fontName.lastIndexOf("."))
            val nightModeTextBrightness = 87
            doc.head().getElementById("darkTheme")?.remove()
            doc.head().append("""
            <style id="darkTheme">
                @font-face {
                    font-family: $fontFamily;
                    src: url("file://$fontUrl");
                }
                body {
                    ${if (isDark) "background-color" else "color"}: #000;
                    ${if (isDark) "color" else "background-color"}: rgba(255, 255, 255, .$nightModeTextBrightness);
                    font-family: '$fontFamily';
                    line-height: 1.5;
                    padding: 20px;
                }
                a {
                    color: rgba(${if (isDark) "135, 206, 250" else "0, 0, 238"}, .$nightModeTextBrightness);
                }
            </style>
            """.trimIndent())

            return doc
    }
}
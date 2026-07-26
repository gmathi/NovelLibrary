package io.github.gmathi.novellibrary.cleaner

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HtmlCleanerTest {

    private class TestHtmlCleaner : HtmlCleaner()

    // Mirrors the markup freewebnovel.com started serving in 2026: the site styles the
    // <html> element directly, which used to leak past cleaning and paint a light frame
    // around the injected dark theme's body background.
    private val pageWithStyledRoot = """
        <!DOCTYPE html>
        <html lang="en-US" class="reader-background-document" style="--reader-document-bg:#F4F4F4;background-color:#F4F4F4">
        <head>
            <style>html.reader-background-document,html.reader-background-document body{background-color:var(--reader-document-bg)!important}</style>
            <title>Chapter 1</title>
        </head>
        <body class="site-theme" style="background-color:#FFFFFF">
            <div id="article"><div class="txt"><p>Chapter text.</p></div></div>
        </body>
        </html>
    """.trimIndent()

    @Test
    fun `removeCSS strips style and class attributes from the html element`() {
        val doc = Jsoup.parse(pageWithStyledRoot)

        TestHtmlCleaner().removeCSS(doc)

        val htmlElement = doc.selectFirst("html")!!
        assertFalse("html element should not keep its inline style", htmlElement.hasAttr("style"))
        assertFalse("html element should not keep its class list", htmlElement.hasAttr("class"))
    }

    @Test
    fun `removeCSS strips style attribute from the body element`() {
        val doc = Jsoup.parse(pageWithStyledRoot)

        TestHtmlCleaner().removeCSS(doc)

        assertFalse("body element should not keep its inline style", doc.body().hasAttr("style"))
    }

    @Test
    fun `removeCSS keeps content untouched`() {
        val doc = Jsoup.parse(pageWithStyledRoot)

        TestHtmlCleaner().removeCSS(doc)

        assertEquals("Chapter text.", doc.select("#article p").text())
    }

    // The ad-slot placeholders freewebnovel.com injects inside its #article content
    // container. None of them are story content, but reader mode carries them through.
    private val pageWithAdSlotImages = """
        <!DOCTYPE html>
        <html>
        <body>
            <div id="article"><div class="txt">
                <p>Chapter text.</p>
                <img src="/static/freewebnovel/images/slot-state/wait-06.webp" width="300" height="250" alt="">
                <p>More chapter text.</p>
                <img src="/static/freewebnovel/images/slot-state/wait-03.webp" width="300" height="250" alt="">
            </div></div>
        </body>
        </html>
    """.trimIndent()

    @Test
    fun `removeImages strips img elements`() {
        val doc = Jsoup.parse(pageWithAdSlotImages)

        TestHtmlCleaner().removeImages(doc)

        assertEquals(0, doc.select("img").size)
    }

    @Test
    fun `removeImages keeps chapter text untouched`() {
        val doc = Jsoup.parse(pageWithAdSlotImages)

        TestHtmlCleaner().removeImages(doc)

        assertEquals("Chapter text. More chapter text.", doc.select("#article p").text())
    }

    @Test
    fun `removeImages strips picture elements`() {
        val doc = Jsoup.parse("<body><p>Text.</p><picture><source srcset='a.webp'><img src='a.png'></picture></body>")

        TestHtmlCleaner().removeImages(doc)

        assertEquals(0, doc.select("picture").size)
        assertEquals(0, doc.select("source").size)
    }

    @Test
    fun `removeImages strips svg elements`() {
        val doc = Jsoup.parse("<body><p>Text.</p><svg viewBox='0 0 10 10'><circle r='5'/></svg></body>")

        TestHtmlCleaner().removeImages(doc)

        assertEquals(0, doc.select("svg").size)
    }

    @Test
    fun `removeImages drops a figure left with no text`() {
        val doc = Jsoup.parse("<body><p>Text.</p><figure><img src='art.png'></figure></body>")

        TestHtmlCleaner().removeImages(doc)

        assertEquals(0, doc.select("figure").size)
    }

    @Test
    fun `removeImages keeps a figure that still has a caption`() {
        val doc = Jsoup.parse("<body><figure><img src='art.png'><figcaption>Sunny's dream.</figcaption></figure></body>")

        TestHtmlCleaner().removeImages(doc)

        assertEquals(1, doc.select("figure").size)
        assertEquals("Sunny's dream.", doc.select("figcaption").text())
    }
}

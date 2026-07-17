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
}

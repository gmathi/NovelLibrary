package io.github.gmathi.novellibrary.cleaner;

import org.jsoup.nodes.Document


class GravityTalesHelper : HtmlHelper() {

//    private val  ROYAL_ROAD_SITE_THEME_ID: String = "siteThemeRoyalRoad"

//    override fun downloadCSS(doc: Document, downloadDir: File) {
//        doc.head().append("<link id=\"${ROYAL_ROAD_SITE_THEME_ID}\" href=\"/Content/Themes/Bootstrap/Site-dark.css\" rel=\"stylesheet\">")
//        //doc.head().getElementsByTag("link").firstOrNull { it.hasAttr("href") && it.attr("href") == "/Content/Themes/Bootstrap/Site.css" }?.remove()
//        super.downloadCSS(doc, downloadDir)
//        doc.head().getElementsByTag("link").firstOrNull { it.hasAttr("href") && it.attr("href") == "../Site.css" }
//    }

    override fun cleanDoc(doc: Document) {
        var contentElement = doc.body().getElementById("chapterContent")
        //contentElement?.prepend("<h4>${getTitle(doc)}</h4><br>")
        do {
            contentElement?.siblingElements()?.remove()
            contentElement = contentElement?.parent()
        } while (contentElement?.tagName() != "body")
    }


}

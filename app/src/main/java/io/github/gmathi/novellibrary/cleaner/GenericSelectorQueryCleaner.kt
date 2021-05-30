package io.github.gmathi.novellibrary.cleaner

import io.github.gmathi.novellibrary.model.other.SelectorQuery
import io.github.gmathi.novellibrary.model.other.SubqueryRole
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

class GenericSelectorQueryCleaner(
    private val url: String, private val query: SelectorQuery,
    override var keepContentStyle: Boolean = query.keepContentStyle, override var keepContentIds: Boolean = query.keepContentIds,
    override var keepContentClasses: Boolean = query.keepContentClasses
) : HtmlCleaner() {

    override fun additionalProcessing(doc: Document) {

        val body = doc.body()
        val contentElement = body.select(query.selector)
        val subQueries = query.subqueries
        val constructedContent = Elements()
        var hasHeader = false
        if (subQueries.isEmpty()) {
            // Legacy cleaner behavior
            removeCSS(doc)
            websiteSpecificFixes(contentElement)

            contentElement.forEach { element -> element.children()?.forEach { cleanCSSFromChildren(it) } }

            doc.getElementsByClass("post-navigation")?.remove()

            constructedContent.addAll(contentElement)
        } else {
            // Comprehensive subqueries
            val subContent = subQueries.map { if (it.selector.isEmpty()) Elements() else body.select(it.selector) }
            removeCSS(doc)
            subContent.forEachIndexed { index, elements ->
                val q = subQueries[index]

                if (!q.multiple && elements.isNotEmpty()) {
                    val first = elements.first()
                    elements.clear()
                    elements.add(first)
                }

                when (q.role) {
                    SubqueryRole.RContent -> {
                        if (elements.isEmpty()) elements.addAll(contentElement)
                        websiteSpecificFixes(elements)
                    }
                    SubqueryRole.RHeader ->
                        hasHeader = elements.isNotEmpty()
//                    SubqueryRole.RFooter -> {}
                    SubqueryRole.RShare -> {
                        elements.remove()
                        return@forEachIndexed
                    }
                    SubqueryRole.RComments ->
                        if (!dataCenter.showChapterComments) {
                            elements.remove()
                            return@forEachIndexed
                        }
                    SubqueryRole.RMeta -> {
                        elements.remove()
                        return@forEachIndexed
                    }
                    SubqueryRole.RNavigation ->
                        if (!dataCenter.enableDirectionalLinks) {
                            elements.remove()
                            return@forEachIndexed
                        }
                    SubqueryRole.RBlacklist -> {
                        elements.remove()
                        return@forEachIndexed
                    }
                    SubqueryRole.RWhitelist -> {
                        constructedContent.addAll(elements)
                        return@forEachIndexed
                    }
//                    SubqueryRole.RPage -> {}
                    else -> {}
                }
                elements.forEach { cleanCSSFromChildren(it) }
                constructedContent.addAll(elements)
            }
        }

        if (!dataCenter.enableDirectionalLinks) removeDirectionalLinks(constructedContent)

        if (!hasHeader && query.appendTitleHeader)
            constructedContent.first().prepend("<h4>${getTitle(doc)}</h4><br>")

        if (!dataCenter.showChapterComments) {
            doc.getElementsByClass("comments-container")?.remove()
            doc.getElementsByClass("respond-container")?.remove()
        }

        body.children().remove()
        body.classNames().forEach { body.removeClass(it) }
        body.append(constructedContent.outerHtml())
        if (query.customCSS.isNotEmpty())
            body.append("<style>${query.customCSS}</style>");

    }

    fun websiteSpecificFixes(contentElement: Elements) {
        //Fix for volarenovels.com
        // TODO: Extract into comprehensive query with Blacklist subquery role
        if (url.contains("volarenovels.com")) {
            val elements = contentElement.select("[id*=announcement]")
            contentElement.removeAll(elements)
        }
    }

    override fun getLinkedChapters(doc: Document): ArrayList<String> {
        return getLinkedChapters(doc.location(), doc.body().select(query.selector).firstOrNull())
    }

    private fun removeDirectionalLinks(contentElement: Elements) {
        contentElement.forEach { element ->
            element.getElementsByTag("a")?.filter {
                it.text().equals("Previous Chapter", ignoreCase = true)
                        || it.text().equals("Next Chapter", ignoreCase = true)
                        || it.text().equals("Project Page", ignoreCase = true)
                        || it.text().equals("Index", ignoreCase = true)
                        || it.text().equals("[Previous Chapter]", ignoreCase = true)
                        || it.text().equals("[Next Chapter]", ignoreCase = true)
                        || it.text().equals("[Table of Contents]", ignoreCase = true)
                        || it.text().equals("Next", ignoreCase = true)
                        || it.text().equals("TOC", ignoreCase = true)
                        || it.text().equals("Previous", ignoreCase = true)

            }?.forEach { it?.remove() }
        }
    }
}

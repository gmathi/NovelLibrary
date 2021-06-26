package io.github.gmathi.novellibrary.cleaner

import io.github.gmathi.novellibrary.model.other.SelectorQuery
import io.github.gmathi.novellibrary.model.other.SelectorSubQuery
import io.github.gmathi.novellibrary.model.other.SubQueryProcessingCommand
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
        val subQueries = query.subQueries
        val constructedContent = Elements()
        var hasHeader = false

        if (subQueries.isEmpty()) {
            /**
             *  Legacy Cleaning
             */

            // Remove the complete CSS links / Style elements in the doc
            removeCSS(doc)
            // Any website specific tweaks that need to be done.
            websiteSpecificFixes(contentElement)

            // Remove CSS Class/Ids from the child elements
            contentElement.forEach { element -> element.children()?.forEach { cleanCSSFromChildren(it) } }
            doc.getElementsByClass("post-navigation")?.remove()

            // Mark as a content.
            contentElement.attr("data-role", SubqueryRole.RContent.toString())

            //Add that back to the constructed elements
            constructedContent.addAll(contentElement)

        } else {
            /**
             * Comprehensive SubQueries
             */

            val subContent = subQueries.map { if (it.selector.isEmpty()) Elements() else body.select(it.selector) }
            removeCSS(doc)
            subContent.forEachIndexed { index, elements ->
                val subQuery = subQueries[index]

                if (!subQuery.multiple && elements.isNotEmpty()) {
                    val first = elements.first()
                    elements.clear()
                    elements.add(first)
                }

                when (subQuery.role) {
                    SubqueryRole.RContent -> {
                        if (elements.isEmpty()) elements.addAll(contentElement)
                        websiteSpecificFixes(elements)
                    }
                    SubqueryRole.RHeader ->
                        hasHeader = elements.isNotEmpty()
//                    SubqueryRole.RFooter -> {}
                    SubqueryRole.RShare -> {
                        applyCommands(subQuery, elements)
                        elements.remove()
                        return@forEachIndexed
                    }
                    SubqueryRole.RComments ->
                        if (!dataCenter.showChapterComments) {
                            applyCommands(subQuery, elements)
                            elements.remove()
                            return@forEachIndexed
                        }
                    SubqueryRole.RMeta -> {
                        applyCommands(subQuery, elements)
                        elements.remove()
                        return@forEachIndexed
                    }
                    SubqueryRole.RNavigation ->
                        if (!dataCenter.enableDirectionalLinks) {
                            applyCommands(subQuery, elements)
                            elements.remove()
                            return@forEachIndexed
                        }
                    SubqueryRole.RBlacklist -> {
                        applyCommands(subQuery, elements)
                        elements.remove()
                        return@forEachIndexed
                    }
                    SubqueryRole.RWhitelist -> {
                        applyCommands(subQuery, elements)
                        elements.attr("data-role", subQuery.role.toString())
                        constructedContent.addAll(elements)
                        return@forEachIndexed
                    }
                    SubqueryRole.RProcess -> {
                        applyCommands(subQuery, elements)
                        return@forEachIndexed
                    }
//                    SubqueryRole.RPage -> {}
                    else -> {
                    }
                }
                applyCommands(subQuery, elements)
                elements.attr("data-role", subQuery.role.toString())
                elements.forEach { element ->
                    element.children()?.forEach { cleanCSSFromChildren(it) }
                }
                constructedContent.addAll(elements)
            }
        }

        if (!dataCenter.enableDirectionalLinks) removeDirectionalLinks(constructedContent)

        if (!hasHeader && query.appendTitleHeader)
            constructedContent.first().prepend("<h4 data-role=\"${SubqueryRole.RHeader}\">${getTitle(doc)}</h4><br>")

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

    private fun applyCommands(subQuery: SelectorSubQuery, elements: Elements) {
        var els = elements
        subQuery.extraProcessing.forEach { (command, value) ->
            when (command) {
                SubQueryProcessingCommand.AddAttribute -> {
                    val split = value.split("=".toRegex(), 2)
                    elements.attr(split[0], split[1])
                }
                SubQueryProcessingCommand.AddId -> els.attr("id", value)
                SubQueryProcessingCommand.AddClass -> value.split(",").forEach { els.addClass(it.trim()) }
                SubQueryProcessingCommand.DisableTTS -> {
                    els.attr("tts-disable", "true")
                    if (value.isNotEmpty()) els.attr("tts-substitute", value)
                }
                SubQueryProcessingCommand.FilterNotRegex -> {
                    val reg = value.toRegex()
                    els = els.filterTo(Elements()) { !it.text().matches(reg) }
                }
                SubQueryProcessingCommand.FilterNotString ->
                    els = els.filterTo(Elements()) { !it.text().contains(value) }
                SubQueryProcessingCommand.FilterOnlyRegex -> {
                    val reg = value.toRegex()
                    els = els.filterTo(Elements()) { it.text().matches(reg) }
                }
                SubQueryProcessingCommand.FilterOnlyString ->
                    els = els.filterTo(Elements()) { it.text().contains(value) }
                SubQueryProcessingCommand.MarkBufferLink ->
                    els.forEach {
                        if (it.hasAttr("href"))
                            els.attr("data-role", "RBuffer")
                    }
                SubQueryProcessingCommand.RemoveAttributes -> {
                    if (value.isEmpty()) els.forEach { it.clearAttributes() }
                    else {
                        val list = value.split(",").map { it.trim() }
                        els.forEach { it.attributes().removeAll { attr -> attr.key in list } }
                    }
                }
                SubQueryProcessingCommand.RemoveClasses ->
                    if (value.isEmpty()) els.removeAttr("class")
                    else value.split(",").forEach { els.removeClass(it.trim()) }
                SubQueryProcessingCommand.RemoveId ->
                    if (value.isEmpty()) els.removeAttr("id")
                    else {
                        val list = value.split(",").map { it.trim() }
                        els.forEach { if (it.attr("id") in list) it.removeAttr("id") }
                    }
                SubQueryProcessingCommand.Unwrap -> {
                    els = els.unwrap()
                    if (value.isNotEmpty()) els = els.wrap("<$value></$value>") // TODO: Sanitize?
                }
                SubQueryProcessingCommand.Wrap -> els = els.wrap("<$value></$value>")
                SubQueryProcessingCommand.ChangeTag -> els.tagName(value)

            }
        }
        if (els != elements) {
            elements.clear()
            elements.addAll(els)
        }
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
        if (query.subQueries.isNotEmpty()) {
            return getLinkedChapters(doc.location(), doc.body().select(query.subQueries.first { it.role == SubqueryRole.RContent }.selector).firstOrNull())
        }
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

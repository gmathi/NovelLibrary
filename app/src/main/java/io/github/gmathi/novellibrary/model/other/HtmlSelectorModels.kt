package io.github.gmathi.novellibrary.model.other

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

/**
 * @param selector The CSS selector used to find the content unless Content subQuery is used.
 * @param appendTitleHeader Whether to append the document title to the cleaned up page unless Header is present and found.
 * @param host An optional host restriction for the selector.
 * @param subQueries A list of comprehensive subQueries to construct cleaned up content.
 * @param keepContentStyle Whether to keep style attribute on the tags during cleanup. (Does not remove the text coloration styles)
 * @param keepContentIds Whether to keep id attribute on the tags during cleanup.
 * @param keepContentClasses Whether to keep class attribute on the tags during cleanup.
 * @param customCSS An additional CSS injected to the page with that selector.
 */
data class SelectorQuery(val selector: String, val appendTitleHeader: Boolean = true, val host: String? = null,
                         val subQueries: List<SelectorSubQuery> = emptyList(),
                         val keepContentStyle: Boolean = false, val keepContentIds: Boolean = true, val keepContentClasses: Boolean = false,
                         val customCSS:String = ""
)

/**
 * @param selector The CSS selector used for that subQuery. An empty string would result in result list.
 * @param role The subQuery role. Depending on it, content is processed differently.
 * @param optional Allows query to be missing from the page. If mandatory query is not found, while SelectorQuery is discarded and not used for the website.
 * @param multiple Whether only first found query result is injected in reader mode or all of them.
 * @param extraProcessing An optional list of extra processing commands for this subQuery.
 * @param customSelector Built-in selector exclusive callback to select the contents of the sub-query.
 * For websites that don't have easily distinctive elements and need some custom selector implementation.
 */
data class SelectorSubQuery(val selector: String, val role: SubqueryRole,
                            val optional: Boolean = true, val multiple: Boolean = true,
                            val extraProcessing: List<SubQueryProcessingCommandInfo> = emptyList(),
                            val customSelector: ((doc: Element) -> Elements)? = null,
)

fun SelectorSubQuery.select(doc: Element): Elements {
    return customSelector?.invoke(doc) ?: if (selector.isEmpty()) Elements()
    else doc.select(selector)
}

data class SubQueryProcessingCommandInfo(val command: SubQueryProcessingCommand, val value: String = "")

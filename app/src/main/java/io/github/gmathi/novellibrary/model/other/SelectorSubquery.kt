package io.github.gmathi.novellibrary.model.other

import java.io.Serializable

/**
 * @param query The CSS selector used for that subquery.
 * @param role The subquery role. Depending on it, content is processed differently.
 * @param optional Allows query to be missing from the page. If mandatory query is not found, while SelectorQuery is discarded and not used for the website.
 * @param multiple Whether only first found query result is injected in reader mode or all of them.
 */
data class SelectorSubquery(val query: String, val role: SubqueryRole, val optional: Boolean = true, val multiple: Boolean = true) : Serializable {
}
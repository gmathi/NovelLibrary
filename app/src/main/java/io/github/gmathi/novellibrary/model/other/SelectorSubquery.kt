package io.github.gmathi.novellibrary.model.other

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

/**
 * @param query The CSS selector used for that subquery.
 * @param role The subquery role. Depending on it, content is processed differently.
 * @param optional Allows query to be missing from the page. If mandatory query is not found, while SelectorQuery is discarded and not used for the website.
 * @param multiple Whether only first found query result is injected in reader mode or all of them.
 */
data class SelectorSubquery(val query: String, val role: SubqueryRole, val optional: Boolean = true, val multiple: Boolean = true) : Serializable {
}

// The subquery role.
enum class SubqueryRole {
    RContent, // Chapter contents.
    RHeader, // Chapter header. If present and found, `appendTitleHeader` is ignored.
    RFooter, // Chapter footer. May contain stuff like pagination for the chapter that cannot be detected explicitly.
    RShare, // Chapter social media embeds (share/like/subscribe/etc).
    RComments, // The comments area.
    RMeta, // Chapter metadata (author, when posted, etc)
    RNavigation, // The chapter navigation (next/prev/TOC)
    RPage, // In-chapter page navigation. Also can be used to detect buffer pages if applicable.
    RBlacklist, // Simply stuff that have to be removed regardless.
}
package io.github.gmathi.novellibrary.model.other

import java.io.Serializable

data class SelectorQuery(val query: String, val appendTitleHeader: Boolean = true, val host: String? = null,
                         val subqueries: List<SelectorSubquery> = emptyList(),
                         val keepContentStyle: Boolean = false, val keepContentIds: Boolean = true, val keepContentClasses: Boolean = false,
                         val customCSS:String = ""
) : Serializable {

    //region equals(), hashcode(), toString()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SelectorQuery

        return query == other.query && appendTitleHeader == other.appendTitleHeader &&
                host == other.host && customCSS == other.customCSS &&
                keepContentStyle == other.keepContentStyle && keepContentIds == other.keepContentIds &&
                keepContentClasses == other.keepContentClasses && subqueries.count() == other.subqueries.count() &&
                subqueries.containsAll(other.subqueries)
    }

    override fun hashCode(): Int {
        var result = query.hashCode()
        result = 31 * result + appendTitleHeader.hashCode() + host.hashCode() + subqueries.hashCode()
        return result
    }

    override fun toString(): String {
        return "SelectorQuery(query='$query', appendTitleHeader=$appendTitleHeader, host=$host, subqueries=${subqueries.count()})"
    }
    //endregion

}
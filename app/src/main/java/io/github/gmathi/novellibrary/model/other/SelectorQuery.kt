package io.github.gmathi.novellibrary.model.other

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

data class SelectorQuery(val query: String, val appendTitleHeader: Boolean = true, val host: String? = null,
                         val subqueries: List<SelectorSubquery> = emptyList()) : Serializable {

    //region equals(), hashcode(), toString()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SelectorQuery

        if (query != other.query) return false
        if (appendTitleHeader != other.appendTitleHeader) return false
        if (host != other.host) return false
        if (subqueries.count() != other.subqueries.count() || !subqueries.containsAll(other.subqueries)) return false

        return true
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
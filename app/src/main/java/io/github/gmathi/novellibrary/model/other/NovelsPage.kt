package io.github.gmathi.novellibrary.model.other

import io.github.gmathi.novellibrary.model.database.Novel

data class NovelsPage(val novels: List<Novel>, val hasNextPage: Boolean) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NovelsPage

        if (novels != other.novels) return false
        if (hasNextPage != other.hasNextPage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = novels.hashCode()
        result = 31 * result + hasNextPage.hashCode()
        return result
    }

    override fun toString(): String {
        return "NovelsPage(novels=$novels, hasNextPage=$hasNextPage)"
    }


}
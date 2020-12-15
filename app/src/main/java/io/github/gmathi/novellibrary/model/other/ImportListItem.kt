package io.github.gmathi.novellibrary.model.other


class ImportListItem {

    var novelUrl: String? = null
    var novelName: String? = null
    var novelImageUrl: String? = null
    var currentlyReading: String? = null
    var currentlyReadingChapterName: String? = null
    var isAlreadyInLibrary: Boolean = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImportListItem

        if (novelUrl != other.novelUrl) return false
        if (novelName != other.novelName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = novelUrl?.hashCode() ?: 0
        result = 31 * result + (novelName?.hashCode() ?: 0)
        return result
    }


}

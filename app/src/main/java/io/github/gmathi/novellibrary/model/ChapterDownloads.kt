package io.github.gmathi.novellibrary.model

class ChapterDownloads {

    var novelId: Long = -1L
    var webPageId: Long = -1L
    var status: Long = 0
    var metaData: HashMap<String, String?> = HashMap()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChapterDownloads

        if (novelId != other.novelId) return false
        if (webPageId != other.webPageId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = novelId.hashCode()
        result = 31 * result + webPageId.hashCode()
        return result
    }


}
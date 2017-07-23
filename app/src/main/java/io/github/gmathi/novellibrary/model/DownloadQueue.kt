package io.github.gmathi.novellibrary.model


class DownloadQueue {

    var novelId: Long = 0
    var status: Long = 0
    var metaData: HashMap<String, String?> = HashMap()

    override fun equals(other: Any?): Boolean {
        return other != null && other is DownloadQueue && other.novelId == this.novelId
    }

}

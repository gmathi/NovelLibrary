package io.github.gmathi.novellibrary.model


data class Download(val webPageId: Long, var novelName: String, var chapter: String) {

    companion object {
        val STATUS_IN_QUEUE = 0
        val STATUS_PAUSED = 1
        val STATUS_RUNNING = 2
    }

    var status: Int = 0
    var metaData: String? = null
    var orderId: Int = 0

    fun equals(other: Download): Boolean = this.webPageId == other.webPageId
}
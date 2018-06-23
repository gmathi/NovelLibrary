package io.github.gmathi.novellibrary.model


data class Download(val webPageUrl: String, var novelName: String, var chapter: String) {

    companion object {
        const val STATUS_IN_QUEUE = 0
        const val STATUS_PAUSED = 1
        const val STATUS_RUNNING = 2
    }

    var status: Int = 0
    var metaData: HashMap<String, String>? = null
    var orderId: Int = 0

    fun equals(other: Download): Boolean = (this.webPageUrl == other.webPageUrl)

}
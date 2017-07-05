package com.mgn.bingenovelreader.model


class DownloadQueue {

    var novelId: Long = 0
    var status: Long = 0
    var totalChapters: Long = -1
    var currentChapter: Long = -1
    var chapterUrlsCached: Long = 0

    override fun equals(other: Any?): Boolean {
        return other != null && other is DownloadQueue && other.novelId == this.novelId
    }
}

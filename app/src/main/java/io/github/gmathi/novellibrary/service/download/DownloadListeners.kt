package io.github.gmathi.novellibrary.service.download

import io.github.gmathi.novellibrary.model.other.DownloadNovelEvent
import io.github.gmathi.novellibrary.model.other.DownloadWebPageEvent

interface DownloadListener {
    fun handleEvent(downloadNovelEvent: DownloadNovelEvent) {}
    fun handleEvent(downloadWebPageEvent: DownloadWebPageEvent) {}
}



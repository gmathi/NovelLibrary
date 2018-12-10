package io.github.gmathi.novellibrary.service.download

import io.github.gmathi.novellibrary.model.DownloadNovelEvent
import io.github.gmathi.novellibrary.model.DownloadWebPageEvent

interface DownloadListener {

    fun handleEvent(downloadNovelEvent: DownloadNovelEvent) { }
    fun handleEvent(downloadWebPageEvent: DownloadWebPageEvent) { }
}



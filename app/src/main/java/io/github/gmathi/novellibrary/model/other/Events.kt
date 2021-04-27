package io.github.gmathi.novellibrary.model.other

import io.github.gmathi.novellibrary.model.database.Download
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.util.Constants.ALL_TRANSLATOR_SOURCES

enum class EventType {
    UPDATE,
    DELETE,
    INSERT,
    COMPLETE,
    PAUSED,
    RUNNING,
    DOWNLOAD
}

class NovelEvent(var type: EventType, var novelId: Long = -1L, var webPage: WebPage? = null)

class DownloadWebPageEvent(var type: EventType, var webPageUrl: String? = null, var download: Download)

class DownloadNovelEvent(var type: EventType, var novelId: Long)

class ServiceEvent(var type: EventType)

class DownloadActionEvent(var novelName: String, var action: String)

class ChapterEvent(val novel: Novel)

class SyncEvent(val novel: Novel, val newChapCount: Int)

class NovelSectionEvent(val novelSectionId: Long)

class ChapterActionModeEvent(val translatorSourceName: String = ALL_TRANSLATOR_SOURCES, val eventType: EventType, val url: String? = null,)

class ReaderSettingsEvent(val setting: String) {
    companion object {
        const val READER_MODE = "readerMode"
        const val NIGHT_MODE = "nightMode"
        const val JAVA_SCRIPT = "javaScript"
        const val FONT = "font"
        const val TEXT_SIZE = "textSize"
    }
}

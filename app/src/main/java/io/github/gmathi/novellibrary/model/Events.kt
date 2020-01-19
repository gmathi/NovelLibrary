package io.github.gmathi.novellibrary.model


enum class EventType {
    UPDATE,
    DELETE,
    INSERT,
    COMPLETE,
    PAUSED,
    RUNNING
}

class NovelEvent(var type: EventType, var novelId: Long = -1L, var webPage: WebPage? = null)

class DownloadWebPageEvent(var type: EventType, var webPageUrl: String? = null, var download: Download)

class DownloadNovelEvent(var type: EventType, var novelName: String)

class ServiceEvent(var type: EventType)

class DownloadActionEvent(var novelName: String, var action: String)

class ChapterEvent(val novel: Novel)

class SyncEvent(val novel: Novel, val newChapCount: Int)

class NovelSectionEvent(val novelSectionId: Long)

class ChapterActionModeEvent(val sourceId: Long = -1L, val eventType: EventType)

class ReaderSettingsEvent(val setting: String) {
    companion object {
        const val READER_MODE = "readerMode"
        const val NIGHT_MODE = "nightMode"
        const val JAVA_SCRIPT = "javaScript"
        const val FONT = "font"
        const val TEXT_SIZE = "textSize"
    }
}

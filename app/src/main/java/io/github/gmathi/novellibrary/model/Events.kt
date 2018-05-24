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

class DownloadWebPageEvent(var type: EventType, var webPageId: Long = -1L, var download: Download)

class DownloadNovelEvent(var type: EventType, var novelName: String)

class ServiceEvent(var type: EventType)

class DownloadActionEvent(var novelName: String, var action: String)

class ChapterEvent(val novel: Novel)

class SyncEvent(val novel: Novel, val newChapCount: Int)

class NovelSectionEvent(val novelSectionId: Long)

class ReaderSettingsEvent(val setting: String) {
    companion object {
        const val NIGHT_MODE = "nightMode"
        const val TEXT_SIZE = "textSize"
        const val READER_MODE = "readerMode"
        const val JAVA_SCRIPT = "javaScript"
        const val FONT = "font"
    }
}

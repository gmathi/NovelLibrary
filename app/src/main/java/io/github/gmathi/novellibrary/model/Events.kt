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

class DownloadEvent(var type: EventType, var webPageId: Long = -1L, var download: Download)

class ServiceEvent(var type: EventType)

class ChapterEvent(val novel: Novel)

class SyncEvent(val novel: Novel, val newChapCount: Int)

class NightModeChangeEvent
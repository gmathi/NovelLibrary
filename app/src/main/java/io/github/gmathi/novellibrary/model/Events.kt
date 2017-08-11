package io.github.gmathi.novellibrary.model


enum class EventType {
    UPDATE,
    DELETE,
    INSERT,
    COMPLETE,
}

class NovelEvent(var type: EventType, var novelId: Long = -1L, var webPage: WebPage? = null)

class ChapterEvent(val novel: Novel)

class SyncEvent(val novel: Novel, val newChapCount: Int)
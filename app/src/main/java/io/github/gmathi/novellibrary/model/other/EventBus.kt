package io.github.gmathi.novellibrary.model.other

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Modern EventBus implementation using SharedFlow and Coroutines
 * Replaces GreenRobot EventBus with type-safe, lifecycle-aware events
 */
object ModernEventBus {
    
    // Chapter events
    private val _chapterEvents = MutableSharedFlow<ChapterActionModeEvent>()
    val chapterEvents: SharedFlow<ChapterActionModeEvent> = _chapterEvents.asSharedFlow()
    
    // Novel section events
    private val _novelSectionEvents = MutableSharedFlow<NovelSectionEvent>()
    val novelSectionEvents: SharedFlow<NovelSectionEvent> = _novelSectionEvents.asSharedFlow()
    
    // Reader settings events
    private val _readerSettingsEvents = MutableSharedFlow<ReaderSettingsEvent>()
    val readerSettingsEvents: SharedFlow<ReaderSettingsEvent> = _readerSettingsEvents.asSharedFlow()
    
    // Download events
    private val _downloadEvents = MutableSharedFlow<DownloadWebPageEvent>()
    val downloadEvents: SharedFlow<DownloadWebPageEvent> = _downloadEvents.asSharedFlow()
    
    // Generic events for any other event types
    private val _genericEvents = MutableSharedFlow<Any>()
    val genericEvents: SharedFlow<Any> = _genericEvents.asSharedFlow()
    
    /**
     * Emit chapter events
     */
    suspend fun emitChapterEvent(event: ChapterActionModeEvent) {
        _chapterEvents.emit(event)
    }
    
    /**
     * Emit novel section events
     */
    suspend fun emitNovelSectionEvent(event: NovelSectionEvent) {
        _novelSectionEvents.emit(event)
    }
    
    /**
     * Emit reader settings events
     */
    suspend fun emitReaderSettingsEvent(event: ReaderSettingsEvent) {
        _readerSettingsEvents.emit(event)
    }
    
    /**
     * Emit download events
     */
    suspend fun emitDownloadEvent(event: DownloadWebPageEvent) {
        _downloadEvents.emit(event)
    }
    
    /**
     * Emit generic events
     */
    suspend fun emitGenericEvent(event: Any) {
        _genericEvents.emit(event)
    }
    
    /**
     * Convenience method for non-suspending contexts
     */
    fun postChapterEvent(event: ChapterActionModeEvent) {
        CoroutineScope(Dispatchers.Main.immediate).launch {
            emitChapterEvent(event)
        }
    }
    
    fun postNovelSectionEvent(event: NovelSectionEvent) {
        CoroutineScope(Dispatchers.Main.immediate).launch {
            emitNovelSectionEvent(event)
        }
    }
    
    fun postReaderSettingsEvent(event: ReaderSettingsEvent) {
        CoroutineScope(Dispatchers.Main.immediate).launch {
            emitReaderSettingsEvent(event)
        }
    }
    
    fun postDownloadEvent(event: DownloadWebPageEvent) {
        CoroutineScope(Dispatchers.Main.immediate).launch {
            emitDownloadEvent(event)
        }
    }
    
    fun postGenericEvent(event: Any) {
        CoroutineScope(Dispatchers.Main.immediate).launch {
            emitGenericEvent(event)
        }
    }
} 
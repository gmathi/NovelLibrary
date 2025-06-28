package io.github.gmathi.novellibrary.util.event

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import io.github.gmathi.novellibrary.model.other.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import uy.kohesive.injekt.injectLazy

/**
 * Modern Event Bus implementation using SharedFlow
 * Replaces EventBus with better performance, type safety, and lifecycle awareness
 */
object ModernEventBus {
    
    companion object {
        const val TAG = "ModernEventBus"
    }

    // SharedFlow instances for different event types
    private val _downloadNovelEvents = MutableSharedFlow<DownloadNovelEvent>(replay = 0)
    val downloadNovelEvents: SharedFlow<DownloadNovelEvent> = _downloadNovelEvents.asSharedFlow()

    private val _downloadWebPageEvents = MutableSharedFlow<DownloadWebPageEvent>(replay = 0)
    val downloadWebPageEvents: SharedFlow<DownloadWebPageEvent> = _downloadWebPageEvents.asSharedFlow()

    private val _chapterActionModeEvents = MutableSharedFlow<ChapterActionModeEvent>(replay = 0)
    val chapterActionModeEvents: SharedFlow<ChapterActionModeEvent> = _chapterActionModeEvents.asSharedFlow()

    private val _novelSectionEvents = MutableSharedFlow<NovelSectionEvent>(replay = 0)
    val novelSectionEvents: SharedFlow<NovelSectionEvent> = _novelSectionEvents.asSharedFlow()

    private val _readerSettingsEvents = MutableSharedFlow<ReaderSettingsEvent>(replay = 0)
    val readerSettingsEvents: SharedFlow<ReaderSettingsEvent> = _readerSettingsEvents.asSharedFlow()

    private val _novelEvents = MutableSharedFlow<NovelEvent>(replay = 0)
    val novelEvents: SharedFlow<NovelEvent> = _novelEvents.asSharedFlow()

    private val _syncEvents = MutableSharedFlow<SyncEvent>(replay = 0)
    val syncEvents: SharedFlow<SyncEvent> = _syncEvents.asSharedFlow()

    private val _serviceEvents = MutableSharedFlow<ServiceEvent>(replay = 0)
    val serviceEvents: SharedFlow<ServiceEvent> = _serviceEvents.asSharedFlow()

    private val _downloadActionEvents = MutableSharedFlow<DownloadActionEvent>(replay = 0)
    val downloadActionEvents: SharedFlow<DownloadActionEvent> = _downloadActionEvents.asSharedFlow()

    private val _chapterEvents = MutableSharedFlow<ChapterEvent>(replay = 0)
    val chapterEvents: SharedFlow<ChapterEvent> = _chapterEvents.asSharedFlow()

    /**
     * Post events to the appropriate SharedFlow
     */
    suspend fun post(event: DownloadNovelEvent) {
        _downloadNovelEvents.emit(event)
    }

    suspend fun post(event: DownloadWebPageEvent) {
        _downloadWebPageEvents.emit(event)
    }

    suspend fun post(event: ChapterActionModeEvent) {
        _chapterActionModeEvents.emit(event)
    }

    suspend fun post(event: NovelSectionEvent) {
        _novelSectionEvents.emit(event)
    }

    suspend fun post(event: ReaderSettingsEvent) {
        _readerSettingsEvents.emit(event)
    }

    suspend fun post(event: NovelEvent) {
        _novelEvents.emit(event)
    }

    suspend fun post(event: SyncEvent) {
        _syncEvents.emit(event)
    }

    suspend fun post(event: ServiceEvent) {
        _serviceEvents.emit(event)
    }

    suspend fun post(event: DownloadActionEvent) {
        _downloadActionEvents.emit(event)
    }

    suspend fun post(event: ChapterEvent) {
        _chapterEvents.emit(event)
    }

    /**
     * Convenience methods for posting events from non-suspend contexts
     */
    fun postAsync(event: DownloadNovelEvent) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate).launch {
            post(event)
        }
    }

    fun postAsync(event: DownloadWebPageEvent) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate).launch {
            post(event)
        }
    }

    fun postAsync(event: ChapterActionModeEvent) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate).launch {
            post(event)
        }
    }

    fun postAsync(event: NovelSectionEvent) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate).launch {
            post(event)
        }
    }

    fun postAsync(event: ReaderSettingsEvent) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate).launch {
            post(event)
        }
    }

    fun postAsync(event: NovelEvent) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate).launch {
            post(event)
        }
    }

    fun postAsync(event: SyncEvent) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate).launch {
            post(event)
        }
    }

    fun postAsync(event: ServiceEvent) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate).launch {
            post(event)
        }
    }

    fun postAsync(event: DownloadActionEvent) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate).launch {
            post(event)
        }
    }

    fun postAsync(event: ChapterEvent) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate).launch {
            post(event)
        }
    }
}

/**
 * Extension functions for easy event subscription in LifecycleOwners
 */
fun LifecycleOwner.subscribeToDownloadNovelEvents(
    onEvent: (DownloadNovelEvent) -> Unit
) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            ModernEventBus.downloadNovelEvents.collect { event ->
                onEvent(event)
            }
        }
    }
}

fun LifecycleOwner.subscribeToDownloadWebPageEvents(
    onEvent: (DownloadWebPageEvent) -> Unit
) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            ModernEventBus.downloadWebPageEvents.collect { event ->
                onEvent(event)
            }
        }
    }
}

fun LifecycleOwner.subscribeToChapterActionModeEvents(
    onEvent: (ChapterActionModeEvent) -> Unit
) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            ModernEventBus.chapterActionModeEvents.collect { event ->
                onEvent(event)
            }
        }
    }
}

fun LifecycleOwner.subscribeToNovelSectionEvents(
    onEvent: (NovelSectionEvent) -> Unit
) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            ModernEventBus.novelSectionEvents.collect { event ->
                onEvent(event)
            }
        }
    }
}

fun LifecycleOwner.subscribeToReaderSettingsEvents(
    onEvent: (ReaderSettingsEvent) -> Unit
) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            ModernEventBus.readerSettingsEvents.collect { event ->
                onEvent(event)
            }
        }
    }
}

fun LifecycleOwner.subscribeToNovelEvents(
    onEvent: (NovelEvent) -> Unit
) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            ModernEventBus.novelEvents.collect { event ->
                onEvent(event)
            }
        }
    }
}

fun LifecycleOwner.subscribeToSyncEvents(
    onEvent: (SyncEvent) -> Unit
) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            ModernEventBus.syncEvents.collect { event ->
                onEvent(event)
            }
        }
    }
}

fun LifecycleOwner.subscribeToServiceEvents(
    onEvent: (ServiceEvent) -> Unit
) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            ModernEventBus.serviceEvents.collect { event ->
                onEvent(event)
            }
        }
    }
}

fun LifecycleOwner.subscribeToDownloadActionEvents(
    onEvent: (DownloadActionEvent) -> Unit
) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            ModernEventBus.downloadActionEvents.collect { event ->
                onEvent(event)
            }
        }
    }
}

fun LifecycleOwner.subscribeToChapterEvents(
    onEvent: (ChapterEvent) -> Unit
) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            ModernEventBus.chapterEvents.collect { event ->
                onEvent(event)
            }
        }
    }
}

/**
 * Extension functions for ViewModels to subscribe to events
 */
fun androidx.lifecycle.ViewModel.subscribeToDownloadNovelEvents(
    onEvent: (DownloadNovelEvent) -> Unit
) {
    viewModelScope.launch {
        ModernEventBus.downloadNovelEvents.collect { event ->
            onEvent(event)
        }
    }
}

fun androidx.lifecycle.ViewModel.subscribeToDownloadWebPageEvents(
    onEvent: (DownloadWebPageEvent) -> Unit
) {
    viewModelScope.launch {
        ModernEventBus.downloadWebPageEvents.collect { event ->
            onEvent(event)
        }
    }
}

fun androidx.lifecycle.ViewModel.subscribeToChapterActionModeEvents(
    onEvent: (ChapterActionModeEvent) -> Unit
) {
    viewModelScope.launch {
        ModernEventBus.chapterActionModeEvents.collect { event ->
            onEvent(event)
        }
    }
}

/**
 * Migration helper to maintain compatibility with existing EventBus code
 */
object EventBusMigration {
    
    /**
     * Migrate from EventBus to ModernEventBus
     * This can be used to gradually migrate existing code
     */
    fun migrateEventBusPost(event: Any) {
        when (event) {
            is DownloadNovelEvent -> ModernEventBus.postAsync(event)
            is DownloadWebPageEvent -> ModernEventBus.postAsync(event)
            is ChapterActionModeEvent -> ModernEventBus.postAsync(event)
            is NovelSectionEvent -> ModernEventBus.postAsync(event)
            is ReaderSettingsEvent -> ModernEventBus.postAsync(event)
            is NovelEvent -> ModernEventBus.postAsync(event)
            is SyncEvent -> ModernEventBus.postAsync(event)
            is ServiceEvent -> ModernEventBus.postAsync(event)
            is DownloadActionEvent -> ModernEventBus.postAsync(event)
            is ChapterEvent -> ModernEventBus.postAsync(event)
            else -> {
                // Log unknown event type
                io.github.gmathi.novellibrary.util.Logs.warn(
                    ModernEventBus.TAG, 
                    "Unknown event type: ${event::class.simpleName}"
                )
            }
        }
    }
} 
package io.github.gmathi.novellibrary.viewmodel

import android.content.Context
import androidx.lifecycle.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import io.github.gmathi.novellibrary.data.repository.ChaptersRepository
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.model.other.ChapterActionModeEvent
import io.github.gmathi.novellibrary.model.other.EventType
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Constants.ALL_TRANSLATOR_SOURCES
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.system.DataAccessor
import io.github.gmathi.novellibrary.util.event.ModernEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.injectLazy
import java.lang.ref.WeakReference

class ChaptersFragmentViewModel(private val state: SavedStateHandle) : ViewModel(), LifecycleObserver, DataAccessor {

    companion object {
        const val TAG = "ChaptersFragmentViewModel"
        const val KEY_NOVEL = "novelKey"
        const val KEY_TRANSLATOR_SOURCE_NAME = "translatorSourceNameKey"
    }

    // Objects to be part of the savedState
    val novel: Novel
        get() = state.get(KEY_NOVEL) as? Novel ?: throw Error("Invalid Novel")

    val translatorSourceName: String
        get() = state.get(KEY_TRANSLATOR_SOURCE_NAME) as? String ?: ALL_TRANSLATOR_SOURCES

    private fun setNovel(novel: Novel) {
        state.set(KEY_NOVEL, novel)
    }

    private fun setTranslatorSourceName(translatorSourceName: String) {
        state.set(KEY_TRANSLATOR_SOURCE_NAME, translatorSourceName)
    }

    override lateinit var firebaseAnalytics: FirebaseAnalytics
    override lateinit var dataCenter: DataCenter
    override lateinit var sourceManager: SourceManager
    override lateinit var networkHelper: NetworkHelper
    override lateinit var dbHelper: io.github.gmathi.novellibrary.database.DBHelper

    private val chaptersRepository: ChaptersRepository by injectLazy()

    // UI State
    private val _uiState = MutableLiveData<ChaptersFragmentUiState>()
    val uiState: LiveData<ChaptersFragmentUiState> = _uiState

    // Chapters data
    private val _chapters = MutableLiveData<List<WebPage>>()
    val chapters: LiveData<List<WebPage>> = _chapters

    // Chapter settings
    private val _chapterSettings = MutableLiveData<List<WebPageSettings>>()
    val chapterSettings: LiveData<List<WebPageSettings>> = _chapterSettings

    // Action mode state
    private val _actionModeState = MutableLiveData<ActionModeState>()
    val actionModeState: LiveData<ActionModeState> = _actionModeState

    // Selection state
    private val _selectedChapters = MutableLiveData<Set<WebPage>>()
    val selectedChapters: LiveData<Set<WebPage>> = _selectedChapters

    init {
        setupEventSubscriptions()
    }

    private fun setupEventSubscriptions() {
        // Subscribe to chapter action mode events
        subscribeToChapterActionModeEvents { event ->
            Logs.info(TAG, "Received ChapterActionModeEvent: ${event.eventType} for source: ${event.translatorSourceName}")
            
            when (event.eventType) {
                EventType.UPDATE -> {
                    // Check if this event is for our current source
                    if (event.translatorSourceName == translatorSourceName || event.translatorSourceName == ALL_TRANSLATOR_SOURCES) {
                        // Refresh chapters data
                        loadChapters()
                    }
                }
                EventType.COMPLETE -> {
                    // Action mode completed, clear selection
                    clearSelection()
                }
                else -> {
                    // Handle other event types if needed
                }
            }
        }
    }

    /**
     * Initialize the ViewModel with novel and translator source
     */
    fun init(novel: Novel, translatorSourceName: String, context: Context) {
        setNovel(novel)
        setTranslatorSourceName(translatorSourceName)
        
        // Initialize dependencies
        firebaseAnalytics = Firebase.analytics
        dataCenter = DataCenter(context)
        sourceManager = SourceManager(context)
        networkHelper = NetworkHelper(context)
        dbHelper = io.github.gmathi.novellibrary.database.DBHelper(context)
        
        // Load initial data
        loadChapters()
    }

    /**
     * Load chapters for the current novel and translator source
     */
    fun loadChapters() {
        viewModelScope.launch {
            try {
                _uiState.value = ChaptersFragmentUiState.Loading
                
                val chapters = if (translatorSourceName == ALL_TRANSLATOR_SOURCES) {
                    chaptersRepository.getChaptersForNovel(novel.id)
                } else {
                    chaptersRepository.getChaptersForNovelAndSource(novel.id, translatorSourceName)
                }
                
                val settings = chaptersRepository.getChapterSettingsForNovel(novel.id)
                
                _chapters.value = chapters
                _chapterSettings.value = settings
                
                if (chapters.isEmpty()) {
                    _uiState.value = ChaptersFragmentUiState.Empty
                } else {
                    _uiState.value = ChaptersFragmentUiState.Success
                }
                
            } catch (e: Exception) {
                Logs.error(TAG, "Error loading chapters", e)
                _uiState.value = ChaptersFragmentUiState.Error(e.message ?: "Failed to load chapters")
            }
        }
    }

    /**
     * Refresh chapters data
     */
    fun refreshChapters() {
        loadChapters()
    }

    /**
     * Toggle chapter selection
     */
    fun toggleChapterSelection(webPage: WebPage) {
        val currentSelection = _selectedChapters.value?.toMutableSet() ?: mutableSetOf()
        
        if (currentSelection.contains(webPage)) {
            currentSelection.remove(webPage)
        } else {
            currentSelection.add(webPage)
        }
        
        _selectedChapters.value = currentSelection
        updateActionModeState()
    }

    /**
     * Select all chapters
     */
    fun selectAllChapters() {
        val allChapters = _chapters.value ?: return
        _selectedChapters.value = allChapters.toSet()
        updateActionModeState()
    }

    /**
     * Clear chapter selection
     */
    fun clearSelection() {
        _selectedChapters.value = emptySet()
        updateActionModeState()
    }

    /**
     * Update action mode state based on selection
     */
    private fun updateActionModeState() {
        val selectedCount = _selectedChapters.value?.size ?: 0
        
        _actionModeState.value = if (selectedCount > 0) {
            ActionModeState.Active(selectedCount)
        } else {
            ActionModeState.Inactive
        }
    }

    /**
     * Mark chapters as read
     */
    fun markChaptersAsRead(webPages: List<WebPage>, markRead: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.value = ChaptersFragmentUiState.Loading
                
                chaptersRepository.markChaptersAsRead(webPages, markRead)
                
                // Refresh data
                loadChapters()
                
                _uiState.value = ChaptersFragmentUiState.Success
                
            } catch (e: Exception) {
                Logs.error(TAG, "Error marking chapters as read", e)
                _uiState.value = ChaptersFragmentUiState.Error(e.message ?: "Failed to update chapters")
            }
        }
    }

    /**
     * Mark chapters as favorite
     */
    fun markChaptersAsFavorite(webPages: List<WebPage>, markFavorite: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.value = ChaptersFragmentUiState.Loading
                
                chaptersRepository.markChaptersAsFavorite(webPages, markFavorite)
                
                // Refresh data
                loadChapters()
                
                _uiState.value = ChaptersFragmentUiState.Success
                
            } catch (e: Exception) {
                Logs.error(TAG, "Error marking chapters as favorite", e)
                _uiState.value = ChaptersFragmentUiState.Error(e.message ?: "Failed to update chapters")
            }
        }
    }

    /**
     * Add chapters to download queue
     */
    fun addChaptersToDownloadQueue(webPages: List<WebPage>) {
        viewModelScope.launch {
            try {
                _uiState.value = ChaptersFragmentUiState.Loading
                
                chaptersRepository.addChaptersToDownloadQueue(webPages, novel)
                
                // Clear selection after adding to download queue
                clearSelection()
                
                _uiState.value = ChaptersFragmentUiState.Success
                
            } catch (e: Exception) {
                Logs.error(TAG, "Error adding chapters to download queue", e)
                _uiState.value = ChaptersFragmentUiState.Error(e.message ?: "Failed to add chapters to download queue")
            }
        }
    }

    /**
     * Delete downloaded chapters
     */
    fun deleteDownloadedChapters(webPages: List<WebPage>) {
        viewModelScope.launch {
            try {
                _uiState.value = ChaptersFragmentUiState.Loading
                
                chaptersRepository.deleteDownloadedChapters(webPages)
                
                // Refresh data
                loadChapters()
                
                _uiState.value = ChaptersFragmentUiState.Success
                
            } catch (e: Exception) {
                Logs.error(TAG, "Error deleting downloaded chapters", e)
                _uiState.value = ChaptersFragmentUiState.Error(e.message ?: "Failed to delete chapters")
            }
        }
    }

    /**
     * Get chapter settings for a specific chapter
     */
    fun getChapterSettings(webPageUrl: String): WebPageSettings? {
        return _chapterSettings.value?.find { it.url == webPageUrl }
    }

    /**
     * Check if a chapter is selected
     */
    fun isChapterSelected(webPage: WebPage): Boolean {
        return _selectedChapters.value?.contains(webPage) ?: false
    }

    /**
     * Get selected chapters count
     */
    fun getSelectedChaptersCount(): Int {
        return _selectedChapters.value?.size ?: 0
    }

    /**
     * Get selected chapters
     */
    fun getSelectedChapters(): List<WebPage> {
        return _selectedChapters.value?.toList() ?: emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        // Cleanup if needed
    }

    // UI State sealed classes
    sealed class ChaptersFragmentUiState {
        object Idle : ChaptersFragmentUiState()
        object Loading : ChaptersFragmentUiState()
        object Success : ChaptersFragmentUiState()
        object Empty : ChaptersFragmentUiState()
        data class Error(val message: String) : ChaptersFragmentUiState()
    }

    sealed class ActionModeState {
        object Inactive : ActionModeState()
        data class Active(val selectedCount: Int) : ActionModeState()
    }
} 
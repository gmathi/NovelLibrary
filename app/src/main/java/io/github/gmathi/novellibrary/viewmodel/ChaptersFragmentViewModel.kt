package io.github.gmathi.novellibrary.viewmodel

import android.content.Context
import androidx.lifecycle.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import io.github.gmathi.novellibrary.data.repository.ChaptersRepository
import io.github.gmathi.novellibrary.database.DBHelper
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
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

    override val dataCenter: DataCenter by injectLazy()
    override val sourceManager: SourceManager by injectLazy()
    override val networkHelper: NetworkHelper by injectLazy()
    override val dbHelper: DBHelper by injectLazy()

    // Repository for data operations
    private val repository: ChaptersRepository by injectLazy()

    private lateinit var _ctx: WeakReference<Context>
    override fun getContext(): Context? = _ctx.get()

    // Reference to parent ViewModel
    private var parentViewModel: ChaptersViewModel? = null

    // LiveData for UI state management
    private val _uiState = MutableLiveData<ChaptersUiState>()
    val uiState: LiveData<ChaptersUiState> = _uiState

    private val _chapters = MutableLiveData<List<WebPage>>()
    val chapters: LiveData<List<WebPage>> = _chapters

    private val _chapterSettings = MutableLiveData<List<WebPageSettings>>()
    val chapterSettings: LiveData<List<WebPageSettings>> = _chapterSettings

    private val _selectedChapters = MutableLiveData<Set<WebPage>>()
    val selectedChapters: LiveData<Set<WebPage>> = _selectedChapters

    private val _scrollToPosition = MutableLiveData<Int>()
    val scrollToPosition: LiveData<Int> = _scrollToPosition

    private val _actionModeProgress = MutableLiveData<String>()
    val actionModeProgress: LiveData<String> = _actionModeProgress

    // Internal state
    private var lastKnownRecyclerState: android.os.Parcelable? = null
    private var selectedChaptersSet = mutableSetOf<WebPage>()

    fun init(novel: Novel, translatorSourceName: String, lifecycleOwner: LifecycleOwner, context: Context, parentViewModel: ChaptersViewModel? = null) {
        setNovel(novel)
        setTranslatorSourceName(translatorSourceName)
        this._ctx = WeakReference(context)
        this.parentViewModel = parentViewModel
        lifecycleOwner.lifecycle.addObserver(this)
        firebaseAnalytics = Firebase.analytics
        
        // Register for EventBus
        EventBus.getDefault().register(this)
        
        // Initialize UI state
        _uiState.value = ChaptersUiState.Loading
        loadChapters()
    }

    fun loadChapters(forceUpdate: Boolean = false) {
        viewModelScope.launch {
            try {
                _uiState.value = ChaptersUiState.Loading
                
                // Get chapters from parent ViewModel or database
                val chaptersList = getChaptersFromParent() ?: getChaptersFromDatabase()
                
                if (chaptersList.isNullOrEmpty()) {
                    if (!repository.isNetworkConnected()) {
                        _uiState.value = ChaptersUiState.NoInternet
                        return@launch
                    }
                    _uiState.value = ChaptersUiState.Empty
                    return@launch
                }

                // Filter chapters by translator source if needed
                val filteredChapters = if (translatorSourceName == ALL_TRANSLATOR_SOURCES) {
                    chaptersList
                } else {
                    chaptersList.filter { it.translatorSourceName == translatorSourceName }
                }

                // Apply chapter order
                val orderedChapters = if (novel.metadata["chapterOrder"] == "des") {
                    filteredChapters.reversed()
                } else {
                    filteredChapters
                }

                _chapters.value = orderedChapters
                _uiState.value = ChaptersUiState.Success

                // Auto-scroll logic
                handleAutoScroll()

            } catch (e: Exception) {
                Logs.error(TAG, "Error loading chapters", e)
                _uiState.value = ChaptersUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    private suspend fun getChaptersFromParent(): List<WebPage>? = withContext(Dispatchers.IO) {
        // Get chapters from parent ViewModel if available
        parentViewModel?.chapters?.let { chapters ->
            parentViewModel?.chapterSettings?.let { settings ->
                _chapterSettings.postValue(settings)
            }
            return@withContext chapters
        }
        return@withContext null
    }

    private suspend fun getChaptersFromDatabase(): List<WebPage> = withContext(Dispatchers.IO) {
        if (novel.id != -1L) {
            val (chapters, settings) = repository.getChaptersFromDatabase(novel.id)
            _chapterSettings.postValue(settings)
            return@withContext chapters
        }
        return@withContext emptyList()
    }

    private fun handleAutoScroll() {
        if (novel.currentChapterUrl != null) {
            val chaptersList = _chapters.value ?: return
            val index = chaptersList.indexOfFirst { it.url == novel.currentChapterUrl }
            if (index != -1) {
                _scrollToPosition.value = index
            }
        }
    }

    fun scrollToFirstUnread() {
        val chaptersList = _chapters.value ?: return
        val settingsList = _chapterSettings.value ?: return
        
        if (novel.currentChapterUrl != null) {
            val index = if (novel.metadata["chapterOrder"] == "des") {
                chaptersList.indexOfLast { chapter -> 
                    settingsList.firstOrNull { it.url == chapter.url && !it.isRead } != null 
                }
            } else {
                chaptersList.indexOfFirst { chapter -> 
                    settingsList.firstOrNull { it.url == chapter.url && !it.isRead } != null 
                }
            }
            if (index != -1) {
                _scrollToPosition.value = index
            }
        }
    }

    fun onChapterClicked(webPage: WebPage) {
        // Handle chapter click - this would typically navigate to reader
        // The actual navigation logic should be handled by the Fragment
    }

    fun onChapterLongClicked(webPage: WebPage): Boolean {
        toggleChapterSelection(webPage)
        return true
    }

    fun toggleChapterSelection(webPage: WebPage) {
        if (selectedChaptersSet.contains(webPage)) {
            selectedChaptersSet.remove(webPage)
        } else {
            selectedChaptersSet.add(webPage)
        }
        _selectedChapters.value = selectedChaptersSet.toSet()
    }

    fun clearSelection() {
        selectedChaptersSet.clear()
        _selectedChapters.value = emptySet()
    }

    fun isChapterSelected(webPage: WebPage): Boolean {
        return selectedChaptersSet.contains(webPage)
    }

    fun getChapterSettings(webPage: WebPage): WebPageSettings? {
        return _chapterSettings.value?.firstOrNull { it.url == webPage.url }
    }

    fun saveRecyclerState(state: android.os.Parcelable?) {
        lastKnownRecyclerState = state
    }

    fun getRecyclerState(): android.os.Parcelable? {
        return lastKnownRecyclerState
    }

    fun updateParentViewModel(parentViewModel: ChaptersViewModel) {
        this.parentViewModel = parentViewModel
        // Reload chapters when parent ViewModel is updated
        loadChapters()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onChapterActionModeEvent(event: ChapterActionModeEvent) {
        when (event.eventType) {
            EventType.COMPLETE -> {
                loadChapters()
            }
            EventType.UPDATE -> {
                if (event.translatorSourceName == translatorSourceName || translatorSourceName == ALL_TRANSLATOR_SOURCES) {
                    loadChapters()
                }
            }
            EventType.DOWNLOAD -> {
                // Update specific chapter if needed
                val chaptersList = _chapters.value?.toMutableList() ?: return
                val index = chaptersList.indexOfFirst { it.url == event.url }
                if (index != -1) {
                    // Trigger item update
                    _chapters.value = chaptersList
                }
            }
            EventType.DELETE -> {
                // Handle delete event
                loadChapters()
            }
            EventType.INSERT -> {
                // Handle insert event
                loadChapters()
            }
            EventType.PAUSED -> {
                // Handle paused event
                // No specific action needed for paused downloads
            }
            EventType.RUNNING -> {
                // Handle running event
                // No specific action needed for running downloads
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        // Save any necessary state
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        loadChapters()
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    // UI State sealed class
    sealed class ChaptersUiState {
        object Loading : ChaptersUiState()
        object Success : ChaptersUiState()
        object Empty : ChaptersUiState()
        object NoInternet : ChaptersUiState()
        data class Error(val message: String) : ChaptersUiState()
    }
} 
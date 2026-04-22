package io.github.gmathi.novellibrary.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.error.Exceptions
import io.github.gmathi.novellibrary.util.logging.Logs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.injectLazy

/**
 * UI state for the Novel Details screen.
 */
sealed interface NovelDetailsUiState {
    /** Initial loading — no data yet. */
    data object Loading : NovelDetailsUiState

    /** Novel details loaded successfully. */
    data class Success(val novel: Novel) : NovelDetailsUiState

    /** No internet and novel is not in the library. */
    data object NoInternet : NovelDetailsUiState

    /** Network or source error. */
    data class Error(val message: String, val exception: Exception? = null) : NovelDetailsUiState

    /** The novel source is missing — unrecoverable. */
    data object MissingSource : NovelDetailsUiState

    /** Cloudflare challenge detected — user action required. */
    data object CloudflareChallenge : NovelDetailsUiState
}

/**
 * One-shot events that the Activity should handle once (navigation, toasts, etc.).
 */
sealed interface NovelDetailsEvent {
    data class ShowToast(val messageResId: Int) : NovelDetailsEvent
    data class CopyErrorToClipboard(val exception: Exception) : NovelDetailsEvent
    data object NovelDeleted : NovelDetailsEvent
    data object NovelAddedToLibrary : NovelDetailsEvent
}

class NovelDetailsViewModel(private val state: SavedStateHandle) : ViewModel() {

    companion object {
        private const val TAG = "NovelDetailsViewModel"
        private const val KEY_NOVEL = "novelKey"
        private const val MAX_CLOUDFLARE_RETRIES = 2
    }

    // --- Injected dependencies (matches existing project pattern) ---
    private val dbHelper: DBHelper by injectLazy()
    private val sourceManager: SourceManager by injectLazy()
    private val networkHelper: NetworkHelper by injectLazy()
    private val firebaseAnalytics: FirebaseAnalytics by injectLazy()
    private val gson: Gson by injectLazy()

    // --- State ---
    private val _uiState = MutableStateFlow<NovelDetailsUiState>(NovelDetailsUiState.Loading)
    val uiState: StateFlow<NovelDetailsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<NovelDetailsEvent>(extraBufferCapacity = 5)
    val events: SharedFlow<NovelDetailsEvent> = _events.asSharedFlow()

    /** Tracks consecutive Cloudflare errors (not user retries). Reset on success or explicit retry. */
    private var cloudflareErrorCount = 0

    /** Active load coroutine — cancelled when a new load starts to avoid races. */
    private var loadJob: Job? = null

    /** The current novel. Persisted across process death via SavedStateHandle. */
    val novel: Novel
        get() = state.get<Novel>(KEY_NOVEL) ?: throw IllegalStateException("Novel not set")

    private fun setNovel(novel: Novel) {
        state[KEY_NOVEL] = novel
    }

    val isInLibrary: Boolean get() = novel.id != -1L

    // ------------------------------------------------------------------
    // Initialisation
    // ------------------------------------------------------------------

    /**
     * Called once from Activity.onCreate with the novel from the intent.
     * Checks the DB for an existing ID and decides whether to load from
     * network or show cached data.
     */
    fun init(intentNovel: Novel) {
        if (state.contains(KEY_NOVEL)) return // already initialised (config change)
        setNovel(intentNovel)

        viewModelScope.launch {
            // Check if novel already exists in library and preserve its DB id
            val dbNovel = withContext(Dispatchers.IO) { dbHelper.getNovelByUrl(intentNovel.url) }
            if (dbNovel != null) {
                val current = novel
                current.id = dbNovel.id
                setNovel(current)
            }

            if (novel.id != -1L && !networkHelper.isConnectedToNetwork()) {
                // Offline but novel is in library — show cached data
                _uiState.value = NovelDetailsUiState.Success(novel)
            } else {
                loadNovelDetails()
            }
        }
    }

    // ------------------------------------------------------------------
    // Data loading
    // ------------------------------------------------------------------

    /** Public entry point for pull-to-refresh and retry. */
    fun refresh() {
        cloudflareErrorCount = 0
        loadNovelDetails()
    }

    /** Called after the user resolves a Cloudflare challenge. */
    fun onCloudflareResolved() {
        cloudflareErrorCount = 0
        loadNovelDetails()
    }

    private fun loadNovelDetails() {
        loadJob?.cancel()
        _uiState.value = NovelDetailsUiState.Loading

        if (!networkHelper.isConnectedToNetwork()) {
            if (novel.id == -1L) {
                _uiState.value = NovelDetailsUiState.NoInternet
            } else {
                _uiState.value = NovelDetailsUiState.Success(novel)
                _events.tryEmit(NovelDetailsEvent.ShowToast(R.string.no_internet))
            }
            return
        }

        loadJob = viewModelScope.launch {
            try {
                val source = sourceManager.get(novel.sourceId)
                    ?: throw Exception(Exceptions.MISSING_SOURCE_ID)

                val totalStartTime = System.currentTimeMillis()
                val fetchedNovel = withContext(Dispatchers.IO) { source.getNovelDetails(novel) }
                val totalElapsed = System.currentTimeMillis() - totalStartTime
                Logs.info(TAG, "⏱ [NovelDetails] Total load time for '${fetchedNovel.name}': ${totalElapsed}ms")

                setNovel(fetchedNovel)

                // Persist updates if novel is in library
                if (fetchedNovel.id != -1L) {
                    withContext(Dispatchers.IO) { dbHelper.updateNovel(fetchedNovel) }
                }

                addNovelToHistory(fetchedNovel)

                cloudflareErrorCount = 0
                _uiState.value = NovelDetailsUiState.Success(fetchedNovel)

            } catch (e: Exception) {
                handleLoadError(e)
            }
        }
    }

    private fun handleLoadError(e: Exception) {
        val message = e.message ?: ""

        when {
            message.contains(Exceptions.MISSING_SOURCE_ID) -> {
                _uiState.value = NovelDetailsUiState.MissingSource
            }

            (message.contains("cloudflare", ignoreCase = true) ||
                    message.contains("HTTP error 503")) && cloudflareErrorCount < MAX_CLOUDFLARE_RETRIES -> {
                cloudflareErrorCount++
                _uiState.value = NovelDetailsUiState.CloudflareChallenge
            }

            else -> {
                _events.tryEmit(NovelDetailsEvent.CopyErrorToClipboard(e))
                if (novel.id == -1L) {
                    _uiState.value = NovelDetailsUiState.Error(message, e)
                } else {
                    // Novel is in library — show cached data with a toast
                    _uiState.value = NovelDetailsUiState.Success(novel)
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Library actions
    // ------------------------------------------------------------------

    /**
     * Adds the novel to the library.
     * The actual DB insert + Firebase logging is delegated to the Activity's
     * DataAccessor.addNewNovel() because it needs a Context for broadcasts.
     * This method just signals the event; the Activity calls back with
     * [onNovelAddedToLibrary] once the insert is done.
     */
    fun addToLibrary() {
        if (novel.id != -1L) return // already in library
        _events.tryEmit(NovelDetailsEvent.NovelAddedToLibrary)
    }

    /** Called by the Activity after it has performed the actual DB insert. */
    fun onNovelAddedToLibrary(updatedNovel: Novel) {
        setNovel(updatedNovel)
    }

    /**
     * Signals the Activity to perform the delete (needs Context for broadcasts).
     */
    fun requestDeleteNovel() {
        _events.tryEmit(NovelDetailsEvent.NovelDeleted)
    }

    /**
     * Called by the Activity after it has performed the actual delete.
     * Receives the novel id that was deleted so we don't rely on mutation order.
     */
    fun onNovelDeleted(deletedNovelId: Long) {
        val current = novel
        if (current.id == deletedNovelId) {
            current.id = -1L
            setNovel(current)
        }
    }

    /**
     * Refreshes the novel from the database (e.g. after returning from chapters screen).
     * Runs on IO to avoid main-thread DB access.
     */
    fun refreshFromDatabase() {
        viewModelScope.launch {
            val dbNovel = withContext(Dispatchers.IO) { dbHelper.getNovelByUrl(novel.url) }
            if (dbNovel != null) {
                setNovel(dbNovel)
            }
        }
    }

    // ------------------------------------------------------------------
    // History
    // ------------------------------------------------------------------

    private fun addNovelToHistory(novel: Novel) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var history = dbHelper.getLargePreference(Constants.LargePreferenceKeys.RVN_HISTORY) ?: "[]"
                var historyList: ArrayList<Novel> = gson.fromJson(history, object : TypeToken<ArrayList<Novel>>() {}.type)
                historyList.removeAll { novel.name == it.name }
                if (historyList.size > 99) historyList = ArrayList(historyList.take(99))
                historyList.add(novel)
                history = gson.toJson(historyList)
                dbHelper.createOrUpdateLargePreference(Constants.LargePreferenceKeys.RVN_HISTORY, history)
            } catch (e: Exception) {
                Logs.warning(TAG, "Failed to update novel history, clearing corrupt data", e)
                dbHelper.deleteLargePreference(Constants.LargePreferenceKeys.RVN_HISTORY)
            }
        }
    }
}

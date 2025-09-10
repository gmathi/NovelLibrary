package io.github.gmathi.novellibrary.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.model.NovelDetailsUiState
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Exceptions
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.system.DataAccessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel for NovelDetailsFragment that manages novel details state and operations.
 * Follows the established architecture patterns with Hilt injection and UiState management.
 */
@HiltViewModel
class NovelDetailsViewModel @Inject constructor(
    private val _dbHelper: DBHelper,
    private val _dataCenter: DataCenter,
    private val _networkHelper: NetworkHelper,
    private val _sourceManager: SourceManager,
    private val _firebaseAnalytics: FirebaseAnalytics
) : BaseViewModel(), DataAccessor {
    
    override var dbHelper: DBHelper = _dbHelper
    override var dataCenter: DataCenter = _dataCenter
    override var networkHelper: NetworkHelper = _networkHelper
    override var sourceManager: SourceManager = _sourceManager
    override var firebaseAnalytics: FirebaseAnalytics = _firebaseAnalytics

    companion object {
        private const val TAG = "NovelDetailsViewModel"
    }

    // UI State for novel details
    private val _uiState = MutableLiveData<NovelDetailsUiState>()
    val uiState: LiveData<NovelDetailsUiState> = _uiState

    // Current novel being displayed
    private var currentNovel: Novel? = null
    private var retryCounter = 0

    override fun getTag(): String = TAG
    
    override fun getContext(): android.content.Context? = null // ViewModels don't have context

    /**
     * Load novel details by novel ID
     */
    fun loadNovelDetails(novelId: Long) {
        launchSafely {
            try {
                val novel = withContext(Dispatchers.IO) {
                    dbHelper.getNovel(novelId)
                }
                
                if (novel != null) {
                    currentNovel = novel
                    
                    // If no network, show cached data
                    if (!networkHelper.isConnectedToNetwork()) {
                        _uiState.value = NovelDetailsUiState.Success(
                            novel = novel,
                            isInLibrary = true
                        )
                        return@launchSafely
                    }
                    
                    // Otherwise fetch from network
                    fetchNovelDetails(novel)
                } else {
                    _uiState.value = NovelDetailsUiState.Error(
                        message = "Novel not found",
                        canRetry = false
                    )
                }
            } catch (e: Exception) {
                Logs.error(TAG, "Error loading novel by ID: $novelId", e)
                _uiState.value = NovelDetailsUiState.Error(
                    message = "Failed to load novel",
                    throwable = e,
                    canRetry = true
                )
            }
        }
    }

    /**
     * Load novel details from network or database (legacy method for Novel object)
     */
    fun loadNovelDetails(novel: Novel) {
        currentNovel = novel
        
        // Check if novel exists in database
        val dbNovel = dbHelper.getNovelByUrl(novel.url)
        if (dbNovel != null) {
            novel.id = dbNovel.id
        }

        // If novel is in library and no network, show cached data
        if (novel.id != -1L && !networkHelper.isConnectedToNetwork()) {
            _uiState.value = NovelDetailsUiState.Success(
                novel = novel,
                isInLibrary = true
            )
            return
        }

        // Otherwise fetch from network
        fetchNovelDetails(novel)
    }

    /**
     * Refresh novel details from network
     */
    fun refreshNovelDetails() {
        currentNovel?.let { novel ->
            _uiState.value = NovelDetailsUiState.Success(
                novel = novel,
                isInLibrary = novel.id != -1L,
                isRefreshing = true
            )
            fetchNovelDetails(novel)
        }
    }

    /**
     * Add novel to library
     */
    fun addToLibrary(novel: Novel) {
        launchSafely {
            try {
                withContext(Dispatchers.IO) {
                    val novelId = dbHelper.insertNovel(novel)
                    novel.id = novelId
                }
                
                // Update UI state to reflect library addition
                _uiState.value = NovelDetailsUiState.Success(
                    novel = novel,
                    isInLibrary = true
                )
                
                Logs.debug(TAG, "Novel added to library: ${novel.name}")
            } catch (e: Exception) {
                Logs.error(TAG, "Error adding novel to library", e)
                handleError(e)
            }
        }
    }

    /**
     * Remove novel from library
     */
    fun removeFromLibrary(novel: Novel) {
        launchSafely {
            try {
                withContext(Dispatchers.IO) {
                    dbHelper.deleteNovel(novel.id)
                    novel.id = -1L
                }
                
                // Update UI state to reflect library removal
                _uiState.value = NovelDetailsUiState.Success(
                    novel = novel,
                    isInLibrary = false
                )
                
                Logs.debug(TAG, "Novel removed from library: ${novel.name}")
            } catch (e: Exception) {
                Logs.error(TAG, "Error removing novel from library", e)
                handleError(e)
            }
        }
    }

    /**
     * Add novel to recently viewed history
     */
    private fun addToHistory(novel: Novel) {
        launchSafely {
            try {
                withContext(Dispatchers.IO) {
                    var history = dbHelper.getLargePreference(Constants.LargePreferenceKeys.RVN_HISTORY) ?: "[]"
                    var historyList: ArrayList<Novel> = Gson().fromJson(history, object : TypeToken<ArrayList<Novel>>() {}.type)
                    
                    // Remove existing entry if present
                    historyList.removeAll { novel.name == it.name }
                    
                    // Limit history size
                    if (historyList.size > 99) {
                        historyList = ArrayList(historyList.take(99))
                    }
                    
                    // Add current novel to history
                    historyList.add(novel)
                    history = Gson().toJson(historyList)
                    dbHelper.createOrUpdateLargePreference(Constants.LargePreferenceKeys.RVN_HISTORY, history)
                }
                
                Logs.debug(TAG, "Novel added to history: ${novel.name}")
            } catch (e: Exception) {
                Logs.warning(TAG, "Error adding novel to history", e)
                // Clear corrupted history
                withContext(Dispatchers.IO) {
                    dbHelper.deleteLargePreference(Constants.LargePreferenceKeys.RVN_HISTORY)
                }
            }
        }
    }

    /**
     * Fetch novel details from network
     */
    private fun fetchNovelDetails(novel: Novel) {
        _uiState.value = NovelDetailsUiState.Loading
        
        // Check network connectivity
        if (!networkHelper.isConnectedToNetwork()) {
            if (novel.id == -1L) {
                _uiState.value = NovelDetailsUiState.Error(
                    message = "No internet connection",
                    canRetry = true
                )
            } else {
                // Show cached data with error message
                _uiState.value = NovelDetailsUiState.Success(
                    novel = novel,
                    isInLibrary = true
                )
            }
            return
        }

        launchSafely {
            try {
                val source = sourceManager.get(novel.sourceId) 
                    ?: throw Exception(Exceptions.MISSING_SOURCE_ID)
                
                val updatedNovel = withContext(Dispatchers.IO) { 
                    source.getNovelDetails(novel) 
                }
                
                // Update novel in database if it exists
                if (updatedNovel.id != -1L) {
                    withContext(Dispatchers.IO) { 
                        dbHelper.updateNovel(updatedNovel) 
                    }
                }
                
                // Add to history
                addToHistory(updatedNovel)
                
                // Update UI state
                _uiState.value = NovelDetailsUiState.Success(
                    novel = updatedNovel,
                    isInLibrary = updatedNovel.id != -1L,
                    isRefreshing = false
                )
                
                retryCounter = 0
                Logs.debug(TAG, "Novel details loaded successfully: ${updatedNovel.name}")
                
            } catch (e: Exception) {
                handleNetworkError(e, novel)
            }
        }
    }

    /**
     * Handle network-specific errors
     */
    private fun handleNetworkError(e: Exception, novel: Novel) {
        when {
            e.message?.contains(Exceptions.MISSING_SOURCE_ID) == true -> {
                _uiState.value = NovelDetailsUiState.Error(
                    message = "Missing Novel Source Id.\nPlease re-add the novel.",
                    throwable = e,
                    canRetry = false
                )
            }
            
            e.message?.contains("cloudflare", ignoreCase = true) == true || 
            e.message?.contains("HTTP error 503") == true -> {
                if (retryCounter < 2) {
                    retryCounter++
                    Logs.warning(TAG, "Cloudflare issue, retrying... ($retryCounter/2)")
                    // Auto-retry for Cloudflare issues
                    fetchNovelDetails(novel)
                } else {
                    _uiState.value = NovelDetailsUiState.Error(
                        message = "Cloudflare protection detected. Please try again later.",
                        throwable = e,
                        canRetry = true
                    )
                }
            }
            
            novel.id == -1L -> {
                _uiState.value = NovelDetailsUiState.Error(
                    message = "Failed to load novel details",
                    throwable = e,
                    canRetry = true
                )
            }
            
            else -> {
                // Show cached data for library novels
                _uiState.value = NovelDetailsUiState.Success(
                    novel = novel,
                    isInLibrary = true
                )
            }
        }
        
        Logs.error(TAG, "Error loading novel details", e)
    }

    /**
     * Retry loading novel details
     */
    fun retryLoading() {
        currentNovel?.let { novel ->
            retryCounter = 0
            fetchNovelDetails(novel)
        }
    }
}
package io.github.gmathi.novellibrary.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.getAllNovels
import io.github.gmathi.novellibrary.database.getNovel

import io.github.gmathi.novellibrary.database.updateNovelMetaData
import io.github.gmathi.novellibrary.model.UiState
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.FAC
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Example ViewModel demonstrating best practices for Hilt dependency injection.
 * 
 * This ViewModel showcases:
 * - Proper Hilt annotation usage
 * - Constructor injection of dependencies
 * - State management with UiState
 * - SavedStateHandle usage
 * - Error handling patterns
 * - Firebase Analytics integration
 * - Coroutine usage with BaseViewModel
 * 
 * Use this as a reference when creating new ViewModels in the application.
 */
@HiltViewModel
class ExampleViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val dbHelper: DBHelper,
    private val dataCenter: DataCenter,
    private val networkHelper: NetworkHelper,
    private val firebaseAnalytics: FirebaseAnalytics
) : BaseViewModel() {

    companion object {
        private const val TAG = "ExampleViewModel"
        private const val KEY_SELECTED_NOVEL_ID = "selected_novel_id"
        private const val KEY_SEARCH_QUERY = "search_query"
    }

    // State management using UiState pattern
    private val _novels = MutableLiveData<UiState<List<Novel>>>()
    val novels: LiveData<UiState<List<Novel>>> = _novels

    private val _selectedNovel = MutableLiveData<UiState<Novel>>()
    val selectedNovel: LiveData<UiState<Novel>> = _selectedNovel

    // StateFlow for reactive properties
    private val _searchResults = MutableStateFlow<List<Novel>>(emptyList())
    val searchResults: StateFlow<List<Novel>> = _searchResults.asStateFlow()

    // SavedStateHandle backed properties
    val searchQuery: LiveData<String> = savedStateHandle.getLiveData(KEY_SEARCH_QUERY, "")
    
    private var selectedNovelId: Long
        get() = savedStateHandle.get<Long>(KEY_SELECTED_NOVEL_ID) ?: -1L
        set(value) = savedStateHandle.set(KEY_SELECTED_NOVEL_ID, value)

    override fun getTag(): String = TAG

    /**
     * Load all novels from the database.
     * Demonstrates proper error handling and state management.
     */
    fun loadNovels() {
        executeWithLoading {
            try {
                _novels.value = UiState.Loading
                
                val novelList = dbHelper.getAllNovels()
                _novels.value = UiState.Success(novelList)
                
                // Log successful load
                firebaseAnalytics.logEvent(FAC.Event.NOVELS_LOADED) {
                    param("count", novelList.size.toLong())
                    param("has_network", if (networkHelper.isConnectedToNetwork()) "true" else "false")
                }
                
            } catch (e: Exception) {
                val errorMessage = "Failed to load novels: ${e.message}"
                _novels.value = UiState.Error(errorMessage, e)
                
                // Log error
                firebaseAnalytics.logEvent(FAC.Event.ERROR_OCCURRED) {
                    param("error_type", "load_novels_failed")
                    param("error_message", e.message ?: "Unknown error")
                }
            }
        }
    }

    /**
     * Select a novel by ID and load its details.
     * Demonstrates SavedStateHandle usage and dependent data loading.
     */
    fun selectNovel(novelId: Long) {
        selectedNovelId = novelId
        
        launchSafely {
            try {
                _selectedNovel.value = UiState.Loading
                
                val novel = dbHelper.getNovel(novelId)
                if (novel != null) {
                    _selectedNovel.value = UiState.Success(novel)
                    
                    // Log novel selection
                    firebaseAnalytics.logEvent(FAC.Event.NOVEL_SELECTED) {
                        param("novel_id", novelId)
                        param("novel_name", novel.name)
                    }
                } else {
                    _selectedNovel.value = UiState.Error("Novel not found")
                }
                
            } catch (e: Exception) {
                _selectedNovel.value = UiState.Error("Failed to load novel: ${e.message}", e)
            }
        }
    }

    /**
     * Search for novels by query.
     * Demonstrates StateFlow usage and search functionality.
     */
    fun searchNovels(query: String) {
        savedStateHandle.set(KEY_SEARCH_QUERY, query)
        
        launchSafely {
            try {
                if (query.isBlank()) {
                    _searchResults.value = emptyList()
                    return@launchSafely
                }
                
                val allNovels = dbHelper.getAllNovels()
                val results = allNovels.filter { 
                    it.name.contains(query, ignoreCase = true) 
                }
                _searchResults.value = results
                
                // Log search
                firebaseAnalytics.logEvent(FAC.Event.SEARCH_PERFORMED) {
                    param("query", query)
                    param("results_count", results.size.toLong())
                }
                
            } catch (e: Exception) {
                handleError(e)
                _searchResults.value = emptyList()
            }
        }
    }

    /**
     * Refresh novels from network if available.
     * Demonstrates network checking and refresh patterns.
     */
    fun refreshNovels() {
        if (!networkHelper.isConnectedToNetwork()) {
            setError("No internet connection available")
            return
        }
        
        executeWithLoading {
            try {
                // Simulate network refresh
                // In real implementation, this would call a repository method
                val refreshedNovels = dbHelper.getAllNovels()
                _novels.value = UiState.Success(refreshedNovels)
                
                firebaseAnalytics.logEvent(FAC.Event.NOVELS_REFRESHED) {
                    param("count", refreshedNovels.size.toLong())
                }
                
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Toggle novel favorite status.
     * Demonstrates database updates and state synchronization.
     */
    fun toggleNovelFavorite(novel: Novel) {
        launchSafely {
            try {
                val updatedNovel = novel.apply {
                    // Update the metadata directly
                    val currentFavorite = metadata["is_favorite"]?.toBoolean() ?: false
                    metadata["is_favorite"] = (!currentFavorite).toString()
                }
                
                dbHelper.updateNovelMetaData(updatedNovel)
                
                // Refresh the selected novel if it's the one being updated
                if (selectedNovelId == novel.id) {
                    _selectedNovel.value = UiState.Success(updatedNovel)
                }
                
                // Log favorite toggle
                firebaseAnalytics.logEvent(FAC.Event.NOVEL_FAVORITE_TOGGLED) {
                    param("novel_id", novel.id)
                    param("is_favorite", if (updatedNovel.metadata["is_favorite"]?.toBoolean() == true) "true" else "false")
                }
                
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Clear search results and query.
     * Demonstrates state clearing patterns.
     */
    fun clearSearch() {
        savedStateHandle.set(KEY_SEARCH_QUERY, "")
        _searchResults.value = emptyList()
    }

    /**
     * Get user preferences for display.
     * Demonstrates DataCenter usage.
     */
    fun getUserPreferences(): Map<String, Any> {
        return mapOf(
            "theme" to if (dataCenter.isDarkTheme) "dark" else "light",
            "language" to dataCenter.language,
            "auto_backup" to (dataCenter.backupFrequency > 0),
            "sync_enabled" to dataCenter.getSyncEnabled("default")
        )
    }

    /**
     * Override error handling to provide custom behavior.
     */
    override fun handleError(throwable: Throwable) {
        super.handleError(throwable)
        
        // Custom error handling logic
        when (throwable) {
            is NetworkException -> {
                setError("Network error: Please check your connection")
            }
            is DatabaseException -> {
                setError("Database error: Please try again")
            }
            else -> {
                setError("An unexpected error occurred")
            }
        }
    }

    /**
     * Clean up resources when ViewModel is cleared.
     */
    override fun onCleared() {
        super.onCleared()
        // Perform any necessary cleanup
        // Note: Hilt handles dependency cleanup automatically
    }
}

// Custom exception classes for demonstration
class NetworkException(message: String) : Exception(message)
class DatabaseException(message: String) : Exception(message)
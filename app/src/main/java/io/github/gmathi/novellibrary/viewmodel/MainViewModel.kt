package io.github.gmathi.novellibrary.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.UiState
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MainViewModel handles the main activity state and global app state management.
 * 
 * Responsibilities:
 * - Manage drawer state
 * - Handle global navigation state
 * - Manage app-wide settings and preferences
 * - Handle deep link processing state
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val dataCenter: DataCenter,
    private val dbHelper: DBHelper
) : BaseViewModel() {

    // UI State for main activity
    private val _uiState = MutableLiveData<UiState<MainUiState>>()
    val uiState: LiveData<UiState<MainUiState>> = _uiState

    // Drawer state management
    private val _isDrawerOpen = MutableLiveData<Boolean>()
    val isDrawerOpen: LiveData<Boolean> = _isDrawerOpen

    // Current destination tracking
    private val _currentDestination = MutableLiveData<Int>()
    val currentDestination: LiveData<Int> = _currentDestination

    init {
        loadInitialState()
    }

    /**
     * Load initial state for the main activity
     */
    private fun loadInitialState() {
        executeWithLoading {
            try {
                val initialState = MainUiState(
                    isFirstLaunch = dataCenter.appVersionCode == 0, // Use version code to determine first launch
                    isDarkTheme = dataCenter.isDarkTheme,
                    currentLanguage = dataCenter.language
                )
                _uiState.value = UiState.Success(initialState)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load initial state")
            }
        }
    }

    /**
     * Update drawer state
     */
    fun setDrawerOpen(isOpen: Boolean) {
        _isDrawerOpen.value = isOpen
    }

    /**
     * Update current destination
     */
    fun setCurrentDestination(destinationId: Int) {
        _currentDestination.value = destinationId
    }

    /**
     * Handle first launch completion
     */
    fun completeFirstLaunch() {
        launchSafely {
            // Set app version code to indicate app has been launched
            if (dataCenter.appVersionCode == 0) {
                dataCenter.appVersionCode = 1 // Set to a non-zero value
            }
            loadInitialState() // Refresh state
        }
    }

    /**
     * Toggle theme
     */
    fun toggleTheme() {
        launchSafely {
            dataCenter.isDarkTheme = !dataCenter.isDarkTheme
            loadInitialState() // Refresh state
        }
    }

    override fun getTag(): String = "MainViewModel"
}

/**
 * UI State data class for MainActivity
 */
data class MainUiState(
    val isFirstLaunch: Boolean = false,
    val isDarkTheme: Boolean = false,
    val currentLanguage: String = "en"
)
package io.github.gmathi.novellibrary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.domain.usecase.ClearRecentlyViewedNovelsUseCase
import io.github.gmathi.novellibrary.domain.usecase.GetRecentlyUpdatedNovelsUseCase
import io.github.gmathi.novellibrary.domain.usecase.GetRecentlyViewedNovelsUseCase
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.other.RecentlyUpdatedItem
import io.github.gmathi.novellibrary.network.NetworkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.injectLazy

sealed class RecentlyUpdatedUiState {
    object Loading : RecentlyUpdatedUiState()
    data class Success(val items: List<RecentlyUpdatedItem>) : RecentlyUpdatedUiState()
    data class Error(val message: String) : RecentlyUpdatedUiState()
    object NoInternet : RecentlyUpdatedUiState()
}

sealed class RecentlyViewedUiState {
    object Loading : RecentlyViewedUiState()
    data class Success(val novels: List<Novel>) : RecentlyViewedUiState()
    object Empty : RecentlyViewedUiState()
}

class RecentNovelsViewModel : ViewModel() {

    private val networkHelper: NetworkHelper by injectLazy()
    private val dbHelper: DBHelper by injectLazy()
    private val gson: Gson by injectLazy()

    private val getRecentlyUpdatedNovelsUseCase = GetRecentlyUpdatedNovelsUseCase()
    private val getRecentlyViewedNovelsUseCase = GetRecentlyViewedNovelsUseCase(dbHelper, gson)
    private val clearRecentlyViewedNovelsUseCase = ClearRecentlyViewedNovelsUseCase(dbHelper)

    private val _recentlyUpdatedState = MutableStateFlow<RecentlyUpdatedUiState>(RecentlyUpdatedUiState.Loading)
    val recentlyUpdatedState: StateFlow<RecentlyUpdatedUiState> = _recentlyUpdatedState.asStateFlow()

    private val _recentlyViewedState = MutableStateFlow<RecentlyViewedUiState>(RecentlyViewedUiState.Loading)
    val recentlyViewedState: StateFlow<RecentlyViewedUiState> = _recentlyViewedState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun loadRecentlyUpdatedNovels(isManualRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isManualRefresh) {
                _isRefreshing.value = true
            } else {
                _recentlyUpdatedState.value = RecentlyUpdatedUiState.Loading
            }

            if (!networkHelper.isConnectedToNetwork()) {
                _recentlyUpdatedState.value = RecentlyUpdatedUiState.NoInternet
                _isRefreshing.value = false
                return@launch
            }

            val result = withContext(Dispatchers.IO) {
                getRecentlyUpdatedNovelsUseCase()
            }

            _recentlyUpdatedState.value = result.fold(
                onSuccess = { RecentlyUpdatedUiState.Success(it) },
                onFailure = { RecentlyUpdatedUiState.Error(it.localizedMessage ?: "Unknown error") }
            )
            _isRefreshing.value = false
        }
    }

    fun loadRecentlyViewedNovels() {
        viewModelScope.launch {
            _recentlyViewedState.value = RecentlyViewedUiState.Loading

            val novels = withContext(Dispatchers.IO) {
                getRecentlyViewedNovelsUseCase()
            }

            _recentlyViewedState.value = if (novels.isEmpty()) {
                RecentlyViewedUiState.Empty
            } else {
                RecentlyViewedUiState.Success(novels)
            }
        }
    }

    fun clearRecentlyViewedNovels() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                clearRecentlyViewedNovelsUseCase()
            }
            loadRecentlyViewedNovels()
        }
    }

    fun retryRecentlyUpdated() {
        loadRecentlyUpdatedNovels(isManualRefresh = true)
    }
}

package io.github.gmathi.novellibrary.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.network.HostNames
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI state for the Metadata screen.
 */
sealed interface MetadataUiState {
    data object Loading : MetadataUiState
    data class Success(val entries: List<Map.Entry<String, String?>>) : MetadataUiState
    data object Empty : MetadataUiState
}

/**
 * One-shot events the Activity should handle once.
 */
sealed interface MetadataEvent {
    data class NavigateToSearch(val title: String, val url: String) : MetadataEvent
    data class OpenInBrowser(val url: String) : MetadataEvent
}

class MetadataViewModel(private val state: SavedStateHandle) : ViewModel() {

    companion object {
        private const val KEY_NOVEL = "novel"
        private const val KEY_INITIALIZED = "initialized"
    }

    private val _uiState = MutableStateFlow<MetadataUiState>(MetadataUiState.Loading)
    val uiState: StateFlow<MetadataUiState> = _uiState.asStateFlow()

    private val _events = MutableStateFlow<MetadataEvent?>(null)
    val events: StateFlow<MetadataEvent?> = _events.asStateFlow()

    val novel: Novel
        get() = state.get<Novel>(KEY_NOVEL) ?: throw IllegalStateException("Novel not set")

    /**
     * Called once from Activity.onCreate with the novel from the intent.
     */
    fun init(intentNovel: Novel) {
        if (state.get<Boolean>(KEY_INITIALIZED) == true) return // already initialised (config change)
        state[KEY_NOVEL] = intentNovel
        state[KEY_INITIALIZED] = true
        loadMetadata()
    }

    private fun loadMetadata() {
        val entries = ArrayList(novel.metadata.entries)
        _uiState.value = if (entries.isEmpty()) {
            MetadataUiState.Empty
        } else {
            MetadataUiState.Success(entries)
        }
    }

    fun onLinkClicked(title: String, url: String) {
        if (url.contains(HostNames.NOVEL_UPDATES)) {
            _events.value = MetadataEvent.NavigateToSearch(title, url)
        } else {
            _events.value = MetadataEvent.OpenInBrowser(url)
        }
    }

    fun onEventConsumed() {
        _events.value = null
    }
}

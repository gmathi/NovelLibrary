package io.github.gmathi.novellibrary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.getNovelByUrl
import io.github.gmathi.novellibrary.database.insertNovel
import io.github.gmathi.novellibrary.database.cleanupNovelData
import io.github.gmathi.novellibrary.domain.usecase.AddNovelToHistoryUseCase
import io.github.gmathi.novellibrary.domain.usecase.GetNovelDetailsUseCase
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.error.Exceptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.injectLazy

sealed class NovelDetailsUiState {
    object Loading : NovelDetailsUiState()
    data class Content(val novel: Novel) : NovelDetailsUiState()
    data class Error(val message: String, val canRetry: Boolean = true) : NovelDetailsUiState()
    object NoInternet : NovelDetailsUiState()
    object CloudflareRequired : NovelDetailsUiState()
    object MissingSource : NovelDetailsUiState()
}

sealed class NovelDetailsEvent {
    object NavigateBack : NovelDetailsEvent()
    data class NavigateToChapters(val novel: Novel) : NovelDetailsEvent()
    data class NavigateToMetadata(val novel: Novel) : NovelDetailsEvent()
    data class NavigateToImagePreview(val imageUrl: String?, val imageFilePath: String?) : NovelDetailsEvent()
    data class NavigateToSearchResults(val title: String, val url: String) : NovelDetailsEvent()
    data class OpenInBrowser(val url: String) : NovelDetailsEvent()
    data class ShareUrl(val url: String) : NovelDetailsEvent()
    data class ShowMessage(val message: String) : NovelDetailsEvent()
    object LaunchCloudflareResolver : NovelDetailsEvent()
}

class NovelDetailsViewModel : ViewModel() {

    private val networkHelper: NetworkHelper by injectLazy()
    private val dbHelper: DBHelper by injectLazy()
    private val sourceManager: SourceManager by injectLazy()
    private val gson: Gson by injectLazy()

    private val getNovelDetailsUseCase by lazy { GetNovelDetailsUseCase(sourceManager, dbHelper) }
    private val addNovelToHistoryUseCase by lazy { AddNovelToHistoryUseCase(dbHelper, gson) }

    private val _uiState = MutableStateFlow<NovelDetailsUiState>(NovelDetailsUiState.Loading)
    val uiState: StateFlow<NovelDetailsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<NovelDetailsEvent>()
    val events: SharedFlow<NovelDetailsEvent> = _events.asSharedFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var novel: Novel? = null
    private var retryCounter = 0

    fun init(initialNovel: Novel) {
        if (novel != null) return
        novel = initialNovel
        // Resolve local DB id
        dbHelper.getNovelByUrl(initialNovel.url)?.let { novel!!.id = it.id }

        if (novel!!.id != -1L && !networkHelper.isConnectedToNetwork()) {
            _uiState.value = NovelDetailsUiState.Content(novel!!)
        } else {
            loadDetails()
        }
    }

    fun loadDetails(isManualRefresh: Boolean = false) {
        val currentNovel = novel ?: return

        if (isManualRefresh) {
            _isRefreshing.value = true
        } else {
            _uiState.value = NovelDetailsUiState.Loading
        }

        if (!networkHelper.isConnectedToNetwork()) {
            _isRefreshing.value = false
            if (currentNovel.id == -1L) {
                _uiState.value = NovelDetailsUiState.NoInternet
            } else {
                _uiState.value = NovelDetailsUiState.Content(currentNovel)
                viewModelScope.launch { _events.emit(NovelDetailsEvent.ShowMessage("No internet connection")) }
            }
            return
        }

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { getNovelDetailsUseCase(currentNovel) }

            result.fold(
                onSuccess = { updatedNovel ->
                    novel = updatedNovel
                    withContext(Dispatchers.IO) { addNovelToHistoryUseCase(updatedNovel) }
                    retryCounter = 0
                    _uiState.value = NovelDetailsUiState.Content(updatedNovel)
                },
                onFailure = { error ->
                    when {
                        error.message?.contains(Exceptions.MISSING_SOURCE_ID) == true -> {
                            _uiState.value = NovelDetailsUiState.MissingSource
                        }
                        (error.message?.contains("cloudflare", ignoreCase = true) == true ||
                            error.message?.contains("HTTP error 503") == true) && retryCounter < 2 -> {
                            retryCounter++
                            _uiState.value = NovelDetailsUiState.CloudflareRequired
                            _events.emit(NovelDetailsEvent.LaunchCloudflareResolver)
                        }
                        else -> {
                            if (currentNovel.id == -1L) {
                                _uiState.value = NovelDetailsUiState.Error(
                                    error.localizedMessage ?: "Failed to load novel details"
                                )
                            } else {
                                _uiState.value = NovelDetailsUiState.Content(currentNovel)
                                _events.emit(NovelDetailsEvent.ShowMessage(error.localizedMessage ?: "Failed to refresh"))
                            }
                        }
                    }
                }
            )
            _isRefreshing.value = false
        }
    }

    fun addToLibrary() {
        val currentNovel = novel ?: return
        if (currentNovel.id != -1L) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                currentNovel.id = dbHelper.insertNovel(currentNovel)
            }
            novel = currentNovel
            _uiState.value = NovelDetailsUiState.Content(currentNovel)
        }
    }

    fun deleteFromLibrary() {
        val currentNovel = novel ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dbHelper.cleanupNovelData(currentNovel)
            }
            currentNovel.id = -1L
            novel = currentNovel
            _uiState.value = NovelDetailsUiState.Content(currentNovel)
        }
    }

    fun onCloudflareResolved() {
        retryCounter = 0
        loadDetails()
    }

    fun onDeleteAndFinish() {
        viewModelScope.launch {
            val currentNovel = novel ?: return@launch
            withContext(Dispatchers.IO) { dbHelper.cleanupNovelData(currentNovel) }
            _events.emit(NovelDetailsEvent.NavigateBack)
        }
    }

    fun onChaptersClick() {
        novel?.let { viewModelScope.launch { _events.emit(NovelDetailsEvent.NavigateToChapters(it)) } }
    }

    fun onMetadataClick() {
        novel?.let { viewModelScope.launch { _events.emit(NovelDetailsEvent.NavigateToMetadata(it)) } }
    }

    fun onImageClick() {
        val n = novel ?: return
        viewModelScope.launch { _events.emit(NovelDetailsEvent.NavigateToImagePreview(n.imageUrl, n.imageFilePath)) }
    }

    fun onOpenInBrowser() {
        novel?.let { viewModelScope.launch { _events.emit(NovelDetailsEvent.OpenInBrowser(it.url)) } }
    }

    fun onShareUrl() {
        novel?.let { viewModelScope.launch { _events.emit(NovelDetailsEvent.ShareUrl(it.url)) } }
    }

    fun onAuthorLinkClick(title: String, url: String) {
        viewModelScope.launch { _events.emit(NovelDetailsEvent.NavigateToSearchResults(title, url)) }
    }

    fun onNovelAddedFromChapters(url: String) {
        dbHelper.getNovelByUrl(url)?.let { dbNovel ->
            novel = dbNovel
            _uiState.value = NovelDetailsUiState.Content(dbNovel)
        }
    }
}

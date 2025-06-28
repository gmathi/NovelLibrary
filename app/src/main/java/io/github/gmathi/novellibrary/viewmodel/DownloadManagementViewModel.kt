package io.github.gmathi.novellibrary.viewmodel

import android.content.Context
import androidx.lifecycle.*
import io.github.gmathi.novellibrary.data.repository.DownloadManagementRepository
import io.github.gmathi.novellibrary.model.database.Download
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.other.DownloadNovelEvent
import io.github.gmathi.novellibrary.model.other.DownloadWebPageEvent
import io.github.gmathi.novellibrary.model.other.EventType
import io.github.gmathi.novellibrary.service.download.DownloadListener
import io.github.gmathi.novellibrary.service.download.DownloadNovelService
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.event.ModernEventBus
import io.github.gmathi.novellibrary.util.system.startDownloadNovelService
import kotlinx.coroutines.launch
import uy.kohesive.injekt.injectLazy
import java.util.*

class DownloadManagementViewModel : ViewModel(), DownloadListener {
    
    companion object {
        const val TAG = "DownloadManagementViewModel"
    }

    private val downloadRepository: DownloadManagementRepository by injectLazy()

    // UI State
    private val _uiState = MutableLiveData<DownloadManagementUiState>()
    val uiState: LiveData<DownloadManagementUiState> = _uiState

    // Download operations state
    private val _downloadOperationsState = MutableLiveData<DownloadOperationsState>()
    val downloadOperationsState: LiveData<DownloadOperationsState> = _downloadOperationsState

    // Download list
    private val _downloads = MutableLiveData<List<Download>>()
    val downloads: LiveData<List<Download>> = _downloads

    // Download statistics
    private val _downloadStatistics = MutableLiveData<DownloadStatistics>()
    val downloadStatistics: LiveData<DownloadStatistics> = _downloadStatistics

    // Novel download progress
    private val _novelDownloadProgress = MutableLiveData<Map<Long, NovelDownloadProgress>>()
    val novelDownloadProgress: LiveData<Map<Long, NovelDownloadProgress>> = _novelDownloadProgress

    // Download service state
    private val _downloadServiceState = MutableLiveData<DownloadServiceState>()
    val downloadServiceState: LiveData<DownloadServiceState> = _downloadServiceState

    init {
        setupEventSubscriptions()
        loadInitialData()
    }

    private fun setupEventSubscriptions() {
        // Subscribe to download novel events
        subscribeToDownloadNovelEvents { event ->
            Logs.info(TAG, "Received DownloadNovelEvent: ${event.type} for novel ID: ${event.novelId}")
            
            // Update progress for the specific novel
            getDownloadProgressForNovel(event.novelId)
            
            // Reload data if needed
            when (event.type) {
                EventType.INSERT, EventType.DELETE -> {
                    loadDownloads()
                    loadDownloadStatistics()
                }
                else -> {
                    // Just update progress
                }
            }
        }

        // Subscribe to download web page events
        subscribeToDownloadWebPageEvents { event ->
            Logs.info(TAG, "Received DownloadWebPageEvent: ${event.type} for novel ID: ${event.download.novelId}")
            
            // Update progress for the specific novel
            getDownloadProgressForNovel(event.download.novelId)
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                _uiState.value = DownloadManagementUiState.Loading
                
                // Load downloads and statistics in parallel
                val downloadsDeferred = kotlinx.coroutines.async { downloadRepository.getAllDownloads() }
                val statisticsDeferred = kotlinx.coroutines.async { downloadRepository.getDownloadStatistics() }
                
                val downloads = downloadsDeferred.await()
                val statistics = statisticsDeferred.await()
                
                _downloads.value = downloads
                _downloadStatistics.value = statistics
                
                // Calculate novel download progress
                updateNovelDownloadProgress(downloads)
                
                _uiState.value = DownloadManagementUiState.Success
                
            } catch (e: Exception) {
                Logs.error(TAG, "Error loading initial data", e)
                _uiState.value = DownloadManagementUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Load all downloads
     */
    fun loadDownloads() {
        viewModelScope.launch {
            try {
                val downloads = downloadRepository.getAllDownloads()
                _downloads.value = downloads
                updateNovelDownloadProgress(downloads)
            } catch (e: Exception) {
                Logs.error(TAG, "Error loading downloads", e)
                _uiState.value = DownloadManagementUiState.Error(e.message ?: "Failed to load downloads")
            }
        }
    }

    /**
     * Load download statistics
     */
    fun loadDownloadStatistics() {
        viewModelScope.launch {
            try {
                val statistics = downloadRepository.getDownloadStatistics()
                _downloadStatistics.value = statistics
            } catch (e: Exception) {
                Logs.error(TAG, "Error loading download statistics", e)
            }
        }
    }

    /**
     * Get download progress for a specific novel
     */
    fun getDownloadProgressForNovel(novelId: Long) {
        viewModelScope.launch {
            try {
                val downloads = downloadRepository.getDownloadsForNovel(novelId)
                val progress = calculateNovelDownloadProgress(novelId, downloads)
                
                val currentProgress = _novelDownloadProgress.value?.toMutableMap() ?: mutableMapOf()
                currentProgress[novelId] = progress
                _novelDownloadProgress.value = currentProgress
                
            } catch (e: Exception) {
                Logs.error(TAG, "Error getting download progress for novel $novelId", e)
            }
        }
    }

    /**
     * Start download for a novel
     */
    fun startDownload(novelId: Long, context: Context) {
        viewModelScope.launch {
            try {
                _downloadOperationsState.value = DownloadOperationsState.Loading
                
                // Check if service is running
                if (Utils.isServiceRunning(context, DownloadNovelService.QUALIFIED_NAME)) {
                    // Service is running, just start the download
                    Logs.info(TAG, "Download service is running, starting download for novel $novelId")
                } else {
                    // Start the download service
                    startDownloadNovelService(novelId)
                    Logs.info(TAG, "Started download service for novel $novelId")
                }
                
                _downloadOperationsState.value = DownloadOperationsState.Success("Download started")
                
            } catch (e: Exception) {
                Logs.error(TAG, "Error starting download for novel $novelId", e)
                _downloadOperationsState.value = DownloadOperationsState.Error(e.message ?: "Failed to start download")
            }
        }
    }

    /**
     * Pause download for a novel
     */
    fun pauseDownload(novelId: Long, context: Context) {
        viewModelScope.launch {
            try {
                _downloadOperationsState.value = DownloadOperationsState.Loading
                
                if (Utils.isServiceRunning(context, DownloadNovelService.QUALIFIED_NAME)) {
                    // Pause the download
                    Logs.info(TAG, "Pausing download for novel $novelId")
                    // TODO: Implement pause functionality through service
                }
                
                _downloadOperationsState.value = DownloadOperationsState.Success("Download paused")
                
            } catch (e: Exception) {
                Logs.error(TAG, "Error pausing download for novel $novelId", e)
                _downloadOperationsState.value = DownloadOperationsState.Error(e.message ?: "Failed to pause download")
            }
        }
    }

    /**
     * Remove download for a novel
     */
    fun removeDownload(novelId: Long, context: Context) {
        viewModelScope.launch {
            try {
                _downloadOperationsState.value = DownloadOperationsState.Loading
                
                // Remove downloads from database
                downloadRepository.removeDownloadsForNovel(novelId)
                
                // Update UI
                loadDownloads()
                loadDownloadStatistics()
                
                // Remove from progress map
                val currentProgress = _novelDownloadProgress.value?.toMutableMap() ?: mutableMapOf()
                currentProgress.remove(novelId)
                _novelDownloadProgress.value = currentProgress
                
                _downloadOperationsState.value = DownloadOperationsState.Success("Download removed")
                
            } catch (e: Exception) {
                Logs.error(TAG, "Error removing download for novel $novelId", e)
                _downloadOperationsState.value = DownloadOperationsState.Error(e.message ?: "Failed to remove download")
            }
        }
    }

    /**
     * Clear all downloads
     */
    fun clearAllDownloads(context: Context) {
        viewModelScope.launch {
            try {
                _downloadOperationsState.value = DownloadOperationsState.Loading
                
                // Clear all downloads
                downloadRepository.clearAllDownloads()
                
                // Update UI
                _downloads.value = emptyList()
                _downloadStatistics.value = DownloadStatistics(0, 0, 0, 0)
                _novelDownloadProgress.value = emptyMap()
                
                _downloadOperationsState.value = DownloadOperationsState.Success("All downloads cleared")
                
            } catch (e: Exception) {
                Logs.error(TAG, "Error clearing all downloads", e)
                _downloadOperationsState.value = DownloadOperationsState.Error(e.message ?: "Failed to clear downloads")
            }
        }
    }

    /**
     * Update novel download progress
     */
    private fun updateNovelDownloadProgress(downloads: List<Download>) {
        val progressMap = mutableMapOf<Long, NovelDownloadProgress>()
        
        downloads.groupBy { it.novelId }.forEach { (novelId, novelDownloads) ->
            val progress = calculateNovelDownloadProgress(novelId, novelDownloads)
            progressMap[novelId] = progress
        }
        
        _novelDownloadProgress.value = progressMap
    }

    /**
     * Calculate download progress for a novel
     */
    private fun calculateNovelDownloadProgress(novelId: Long, downloads: List<Download>): NovelDownloadProgress {
        val totalDownloads = downloads.size
        val completedDownloads = downloads.count { it.status == Download.STATUS_COMPLETED }
        val runningDownloads = downloads.count { it.status == Download.STATUS_RUNNING }
        val queuedDownloads = downloads.count { it.status == Download.STATUS_IN_QUEUE }
        val pausedDownloads = downloads.count { it.status == Download.STATUS_PAUSED }
        
        val progress = if (totalDownloads > 0) {
            (completedDownloads.toFloat() / totalDownloads * 100).toInt()
        } else {
            0
        }
        
        return NovelDownloadProgress(
            novelId = novelId,
            totalDownloads = totalDownloads,
            completedDownloads = completedDownloads,
            runningDownloads = runningDownloads,
            queuedDownloads = queuedDownloads,
            pausedDownloads = pausedDownloads,
            progress = progress
        )
    }

    /**
     * Reset UI state
     */
    fun resetUiState() {
        _uiState.value = DownloadManagementUiState.Idle
        _downloadOperationsState.value = DownloadOperationsState.Idle
    }

    /**
     * Reset download operations state
     */
    fun resetDownloadOperationsState() {
        _downloadOperationsState.value = DownloadOperationsState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        // Cleanup if needed
    }

    // UI State sealed classes
    sealed class DownloadManagementUiState {
        object Idle : DownloadManagementUiState()
        object Loading : DownloadManagementUiState()
        object Success : DownloadManagementUiState()
        data class Error(val message: String) : DownloadManagementUiState()
    }

    sealed class DownloadOperationsState {
        object Idle : DownloadOperationsState()
        object Loading : DownloadOperationsState()
        data class Success(val message: String) : DownloadOperationsState()
        data class Error(val message: String) : DownloadOperationsState()
    }

    data class DownloadStatistics(
        val totalDownloads: Int,
        val completedDownloads: Int,
        val runningDownloads: Int,
        val queuedDownloads: Int
    )

    data class NovelDownloadProgress(
        val novelId: Long,
        val totalDownloads: Int,
        val completedDownloads: Int,
        val runningDownloads: Int,
        val queuedDownloads: Int,
        val pausedDownloads: Int,
        val progress: Int
    )

    sealed class DownloadServiceState {
        object Idle : DownloadServiceState()
        object Running : DownloadServiceState()
        object Paused : DownloadServiceState()
        object Stopped : DownloadServiceState()
        data class Error(val message: String) : DownloadServiceState()
    }
} 
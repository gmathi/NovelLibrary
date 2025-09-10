package io.github.gmathi.novellibrary.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.getAllWebPages
import io.github.gmathi.novellibrary.database.getNovel
import io.github.gmathi.novellibrary.model.ReaderUiState
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.system.DataAccessor
import io.github.gmathi.novellibrary.util.system.updateNovelBookmark
import io.github.gmathi.novellibrary.util.system.updateNovelLastRead
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Reader screen that manages novel reading state and navigation
 */
@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val _dbHelper: DBHelper,
    private val _dataCenter: DataCenter,
    private val _firebaseAnalytics: FirebaseAnalytics,
    private val _sourceManager: SourceManager,
    private val _networkHelper: NetworkHelper,
    @ApplicationContext private val context: Context
) : BaseViewModel(), DataAccessor {

    companion object {
        private const val TAG = "ReaderViewModel"
    }

    private val _uiState = MutableLiveData<ReaderUiState>()
    val uiState: LiveData<ReaderUiState> = _uiState

    private var novel: Novel? = null
    private var webPages: List<WebPage> = emptyList()
    private var translatorSourceName: String? = null

    override fun getTag(): String = TAG

    override fun getContext(): Context = context

    override var dbHelper: DBHelper = _dbHelper
    override var dataCenter: DataCenter = _dataCenter
    override var firebaseAnalytics: FirebaseAnalytics = _firebaseAnalytics
    override var sourceManager: SourceManager = _sourceManager
    override var networkHelper: NetworkHelper = _networkHelper

    /**
     * Initialize the reader with novel ID and chapter ID
     */
    fun initializeReader(novelId: Long, chapterId: Long = -1L, translatorSourceName: String?) {
        this.translatorSourceName = translatorSourceName
        loadNovelAndWebPages(novelId, chapterId)
    }

    /**
     * Load novel and web pages for the reader
     */
    private fun loadNovelAndWebPages(novelId: Long, chapterId: Long = -1L) {
        executeWithLoading {
            try {
                // First load the novel from database
                val loadedNovel = dbHelper.getNovel(novelId)
                if (loadedNovel == null) {
                    _uiState.value = ReaderUiState.Error("Novel not found")
                    return@executeWithLoading
                }
                
                novel = loadedNovel
                Logs.debug(TAG, "Loading web pages for novel: ${loadedNovel.name}")
                
                var pages = dbHelper.getAllWebPages(loadedNovel.id, translatorSourceName)
                
                // Apply Japanese swipe order if enabled
                if (dataCenter.japSwipe) {
                    pages = pages.reversed()
                }
                
                webPages = pages
                
                if (pages.isEmpty()) {
                    _uiState.value = ReaderUiState.Error("No chapters found for this novel")
                    return@executeWithLoading
                }

                // Find the current page index based on chapter ID or bookmark
                val currentPageIndex = findCurrentPageIndex(loadedNovel, pages, chapterId)
                
                _uiState.value = ReaderUiState.Success(
                    novel = loadedNovel,
                    webPages = pages,
                    currentPageIndex = currentPageIndex
                )
                
                Logs.debug(TAG, "Loaded ${pages.size} web pages, current index: $currentPageIndex")
                
            } catch (e: Exception) {
                Logs.error(TAG, "Error loading novel and web pages", e)
                handleError(e)
            }
        }
    }

    /**
     * Find the current page index based on chapter ID or the novel's bookmark
     */
    private fun findCurrentPageIndex(novel: Novel, pages: List<WebPage>, chapterId: Long = -1L): Int {
        // First try to find by chapter ID if provided
        if (chapterId != -1L) {
            val index = pages.indexOfFirst { it.orderId.toLong() == chapterId }
            if (index != -1) {
                return index
            }
        }
        
        // Fall back to bookmark URL
        novel.currentChapterUrl?.let { bookmarkUrl ->
            val index = pages.indexOfFirst { it.url == bookmarkUrl }
            if (index != -1) {
                return index
            }
        }
        return 0
    }

    /**
     * Update the bookmark for the current page
     */
    fun updateBookmark(webPage: WebPage) {
        val currentNovel = novel ?: return
        
        launchSafely {
            try {
                updateNovelBookmark(currentNovel, webPage)
                Logs.debug(TAG, "Updated bookmark for: ${webPage.url}")
            } catch (e: Exception) {
                Logs.error(TAG, "Error updating bookmark", e)
            }
        }
    }

    /**
     * Update the last read time for the novel
     */
    fun updateLastRead() {
        val currentNovel = novel ?: return
        
        launchSafely {
            try {
                updateNovelLastRead(currentNovel)
                Logs.debug(TAG, "Updated last read time for novel: ${currentNovel.name}")
            } catch (e: Exception) {
                Logs.error(TAG, "Error updating last read time", e)
            }
        }
    }

    /**
     * Navigate to a specific page by URL
     */
    fun navigateToPage(url: String): Boolean {
        val pages = webPages
        val index = pages.indexOfFirst { it.url == url }
        
        return if (index != -1) {
            val currentState = _uiState.value as? ReaderUiState.Success ?: return false
            _uiState.value = currentState.copy(currentPageIndex = index)
            updateBookmark(pages[index])
            true
        } else {
            false
        }
    }

    /**
     * Refresh the current content
     */
    fun refresh() {
        val currentNovel = novel ?: return
        val currentState = _uiState.value as? ReaderUiState.Success ?: return
        _uiState.value = currentState.copy(isRefreshing = true)
        
        // Reload web pages
        loadNovelAndWebPages(currentNovel.id)
    }

    /**
     * Get the current novel
     */
    fun getCurrentNovel(): Novel? = novel

    /**
     * Get the current web pages
     */
    fun getWebPages(): List<WebPage> = webPages

    /**
     * Get the translator source name
     */
    fun getTranslatorSourceName(): String? = translatorSourceName

    /**
     * Check if volume scroll is enabled
     */
    fun isVolumeScrollEnabled(): Boolean = dataCenter.enableVolumeScroll

    /**
     * Get volume scroll length
     */
    fun getVolumeScrollLength(): Int = dataCenter.volumeScrollLength

    /**
     * Check if reader mode button should be visible
     */
    fun isReaderModeButtonVisible(): Boolean = dataCenter.isReaderModeButtonVisible

    /**
     * Check if immersive mode is enabled
     */
    fun isImmersiveModeEnabled(): Boolean = dataCenter.enableImmersiveMode

    /**
     * Check if screen should stay on
     */
    fun shouldKeepScreenOn(): Boolean = dataCenter.keepScreenOn

    /**
     * Navigate to next chapter
     */
    fun navigateToNextChapter(): Boolean {
        val currentState = _uiState.value as? ReaderUiState.Success ?: return false
        val currentIndex = currentState.currentPageIndex
        
        return if (currentIndex < currentState.webPages.size - 1) {
            val nextIndex = currentIndex + 1
            _uiState.value = currentState.copy(currentPageIndex = nextIndex)
            updateBookmark(currentState.webPages[nextIndex])
            true
        } else {
            false
        }
    }

    /**
     * Navigate to previous chapter
     */
    fun navigateToPreviousChapter(): Boolean {
        val currentState = _uiState.value as? ReaderUiState.Success ?: return false
        val currentIndex = currentState.currentPageIndex
        
        return if (currentIndex > 0) {
            val previousIndex = currentIndex - 1
            _uiState.value = currentState.copy(currentPageIndex = previousIndex)
            updateBookmark(currentState.webPages[previousIndex])
            true
        } else {
            false
        }
    }

    /**
     * Navigate to specific chapter by index
     */
    fun navigateToChapterByIndex(chapterIndex: Int): Boolean {
        val currentState = _uiState.value as? ReaderUiState.Success ?: return false
        
        return if (chapterIndex in 0 until currentState.webPages.size) {
            _uiState.value = currentState.copy(currentPageIndex = chapterIndex)
            updateBookmark(currentState.webPages[chapterIndex])
            true
        } else {
            false
        }
    }

    /**
     * Get current chapter index
     */
    fun getCurrentChapterIndex(): Int {
        val currentState = _uiState.value as? ReaderUiState.Success ?: return -1
        return currentState.currentPageIndex
    }

    /**
     * Get current chapter
     */
    fun getCurrentChapter(): WebPage? {
        val currentState = _uiState.value as? ReaderUiState.Success ?: return null
        val currentIndex = currentState.currentPageIndex
        return if (currentIndex in 0 until currentState.webPages.size) {
            currentState.webPages[currentIndex]
        } else {
            null
        }
    }

    /**
     * Get total chapter count
     */
    fun getTotalChapterCount(): Int {
        val currentState = _uiState.value as? ReaderUiState.Success ?: return 0
        return currentState.webPages.size
    }
}
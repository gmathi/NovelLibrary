package io.github.gmathi.novellibrary.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.model.ChaptersUiState
import io.github.gmathi.novellibrary.model.UiState
import io.github.gmathi.novellibrary.model.database.Download
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.*
import io.github.gmathi.novellibrary.util.Exceptions.MISSING_SOURCE_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ChaptersMainViewModel @Inject constructor(
    private val dbHelper: DBHelper,
    private val dataCenter: DataCenter,
    private val networkHelper: NetworkHelper,
    private val sourceManager: SourceManager,
    private val firebaseAnalytics: FirebaseAnalytics
) : BaseViewModel() {

    companion object {
        private const val TAG = "ChaptersMainViewModel"
    }

    private val _uiState = MutableLiveData<ChaptersUiState>()
    val uiState: LiveData<ChaptersUiState> = _uiState

    private val _actionModeState = MutableLiveData<UiState<String>>()
    val actionModeState: LiveData<UiState<String>> = _actionModeState

    private var currentNovel: Novel? = null
    private var chapters: List<WebPage>? = null
    private var chapterSettings: List<WebPageSettings>? = null
    private var showSources: Boolean = false

    // Expose chapter settings for child fragments to access
    val currentChapterSettings: List<WebPageSettings>?
        get() = chapterSettings

    override fun getTag(): String = TAG

    /**
     * Initialize the ViewModel with a novel.
     */
    fun initialize(novel: Novel) {
        currentNovel = novel
        showSources = novel.metadata[Constants.MetaDataKeys.SHOW_SOURCES]?.toBoolean() ?: false
        loadChapters()
    }

    /**
     * Load chapters for the current novel.
     */
    fun loadChapters(forceRefresh: Boolean = false) {
        val novel = currentNovel ?: return
        
        executeWithLoading {
            _uiState.value = ChaptersUiState.Loading
            
            try {
                // Update novel data if it's in library
                if (novel.id != -1L) {
                    val updatedNovel = withContext(Dispatchers.IO) {
                        dbHelper.getNovel(novel.id)?.also { 
                            dbHelper.updateNewReleasesCount(novel.id, 0L)
                        }
                    }
                    if (updatedNovel != null) {
                        currentNovel = updatedNovel
                    }
                }

                // Load chapters
                loadChaptersData(forceRefresh)

                val currentChapters = chapters
                val currentSettings = chapterSettings

                when {
                    !networkHelper.isConnectedToNetwork() && currentChapters == null -> {
                        _uiState.value = ChaptersUiState.NoInternet
                    }
                    currentChapters == null -> {
                        _uiState.value = ChaptersUiState.NetworkError
                    }
                    currentChapters.isEmpty() -> {
                        _uiState.value = ChaptersUiState.Empty()
                    }
                    else -> {
                        // Add chapters to database if novel is in library
                        if (novel.id != -1L && (forceRefresh || currentSettings.isNullOrEmpty())) {
                            addChaptersToDatabase(forceRefresh)
                        }

                        val translatorSources = if (showSources) {
                            currentChapters.distinctBy { it.translatorSourceName }
                                .mapNotNull { it.translatorSourceName }
                        } else {
                            listOf(Constants.ALL_TRANSLATOR_SOURCES)
                        }

                        _uiState.value = ChaptersUiState.Success(
                            novel = currentNovel!!,
                            chapters = currentChapters,
                            chapterSettings = chapterSettings ?: emptyList(),
                            showSources = showSources,
                            translatorSources = translatorSources
                        )
                    }
                }
            } catch (e: Exception) {
                Logs.error(TAG, "Error loading chapters", e)
                _uiState.value = ChaptersUiState.Error(
                    message = e.message ?: "Failed to load chapters",
                    throwable = e
                )
            }
        }
    }

    /**
     * Toggle between showing sources and showing all chapters.
     */
    fun toggleSources() {
        val novel = currentNovel ?: return
        
        showSources = !showSources
        novel.metadata[Constants.MetaDataKeys.SHOW_SOURCES] = showSources.toString()
        
        launchSafely {
            withContext(Dispatchers.IO) {
                dbHelper.updateNovelMetaData(novel)
            }
            
            // Update UI state with new source configuration
            val currentChapters = chapters
            if (currentChapters != null) {
                val translatorSources = if (showSources) {
                    currentChapters.distinctBy { it.translatorSourceName }
                        .mapNotNull { it.translatorSourceName }
                } else {
                    listOf(Constants.ALL_TRANSLATOR_SOURCES)
                }

                _uiState.value = ChaptersUiState.Success(
                    novel = novel,
                    chapters = currentChapters,
                    chapterSettings = chapterSettings ?: emptyList(),
                    showSources = showSources,
                    translatorSources = translatorSources
                )
            }
        }
    }

    /**
     * Add novel to library.
     */
    fun addNovelToLibrary() {
        val novel = currentNovel ?: return
        if (novel.id != -1L) return

        launchSafely {
            withContext(Dispatchers.IO) {
                novel.id = dbHelper.insertNovel(novel)
                firebaseAnalytics.logEvent(FAC.Event.ADD_NOVEL) {
                    param(FAC.Param.NOVEL_NAME, novel.name)
                    param(FAC.Param.NOVEL_URL, novel.url)
                }
            }
            
            // Update chapters with novel ID
            chapters?.forEach { it.novelId = novel.id }
            
            // Add chapters to database
            addChaptersToDatabase(true)
        }
    }

    /**
     * Update chapters with specific actions.
     */
    fun updateChapters(webPages: List<WebPage>, action: ChapterAction, callback: (() -> Unit)? = null) {
        launchSafely {
            _actionModeState.value = UiState.Loading
            
            withContext(Dispatchers.IO) {
                when (action) {
                    ChapterAction.ADD_DOWNLOADS -> addChaptersToDownloadQueue(webPages)
                    ChapterAction.DELETE_DOWNLOADS -> deleteDownloadedChapters(webPages)
                    ChapterAction.MARK_READ -> updateReadStatus(webPages, true)
                    ChapterAction.MARK_UNREAD -> updateReadStatus(webPages, false)
                    ChapterAction.MARK_FAVORITE -> updateFavoriteStatus(webPages, true)
                    ChapterAction.REMOVE_FAVORITE -> updateFavoriteStatus(webPages, false)
                }
            }
            
            _actionModeState.value = UiState.Success("Action completed")
            callback?.invoke()
        }
    }

    private suspend fun loadChaptersData(forceRefresh: Boolean) = withContext(Dispatchers.IO) {
        val novel = currentNovel ?: return@withContext
        
        // Reset data if force refresh
        if (forceRefresh) {
            chapters = null
            chapterSettings = null
        }

        try {
            // Try to load from cache first if not force refresh
            if (!forceRefresh && novel.id != -1L) {
                val cachedChapters = dbHelper.getAllWebPages(novel.id)
                val cachedSettings = dbHelper.getAllWebPageSettings(novel.id)

                if (cachedChapters.isNotEmpty() && cachedChapters.size >= novel.chaptersCount.toInt()) {
                    chapters = cachedChapters
                    chapterSettings = cachedSettings
                    return@withContext
                } else {
                    // Update last updated date
                    novel.metadata[Constants.MetaDataKeys.LAST_UPDATED_DATE] = Utils.getCurrentFormattedDate()
                    dbHelper.updateNovelMetaData(novel)
                }
            }

            // Fetch from network
            val source = sourceManager.get(novel.sourceId) ?: throw Exception(MISSING_SOURCE_ID)
            val fetchedChapters = source.getChapterList(novel)
            
            if (novel.id != -1L) {
                fetchedChapters.forEach { it.novelId = novel.id }
            }
            
            chapters = fetchedChapters
        } catch (e: Exception) {
            Logs.error(TAG, "Error loading chapters data", e)
            throw e
        }
    }

    private suspend fun addChaptersToDatabase(forceUpdate: Boolean) = withContext(Dispatchers.IO) {
        val novel = currentNovel ?: return@withContext
        val chaptersList = chapters ?: return@withContext

        dbHelper.writableDatabase.runTransaction { writableDatabase ->
            if (forceUpdate) {
                // Delete existing data
                dbHelper.deleteWebPages(novel.id, writableDatabase)
                chaptersList.forEach { dbHelper.deleteWebPage(it.url, writableDatabase) }
            }

            val chaptersCount = chaptersList.size
            dbHelper.updateChaptersCount(novel.id, chaptersCount.toLong(), writableDatabase)

            val chaptersHash = chaptersList.sumOf { it.hashCode() }
            novel.metadata[Constants.MetaDataKeys.HASH_CODE] = chaptersHash.toString()
            dbHelper.updateNovelMetaData(novel)

            chaptersList.forEach { chapter ->
                dbHelper.createWebPage(chapter, writableDatabase)
                dbHelper.createWebPageSettings(WebPageSettings(chapter.url, novel.id), writableDatabase)
            }
        }

        // Reload data from database
        chapters = dbHelper.getAllWebPages(novel.id)
        chapterSettings = dbHelper.getAllWebPageSettings(novel.id)
    }

    private suspend fun addChaptersToDownloadQueue(webPages: List<WebPage>) = withContext(Dispatchers.IO) {
        val novel = currentNovel ?: return@withContext
        
        dbHelper.writableDatabase.runTransaction { writableDatabase ->
            webPages.forEach { webPage ->
                val download = Download(webPage.url, novel.name, novel.id, webPage.chapterName)
                download.orderId = webPage.orderId.toInt()
                dbHelper.createDownload(download, writableDatabase)
            }
        }
    }

    private suspend fun deleteDownloadedChapters(webPages: List<WebPage>) = withContext(Dispatchers.IO) {
        val settings = chapterSettings ?: return@withContext
        
        webPages.forEach { webPage ->
            val webPageSettings = settings.firstOrNull { it.url == webPage.url }
            webPageSettings?.filePath?.let { filePath ->
                val file = File(filePath)
                file.delete()
                webPageSettings.filePath = null
                
                try {
                    if (webPageSettings.metadata.contains(Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES)) {
                        val linkedPages = webPageSettings.getLinkedPagesCompat()
                        linkedPages.forEach { linkedPage ->
                            val linkedWebPageSettings = dbHelper.getWebPageSettings(linkedPage.href)
                            if (linkedWebPageSettings?.filePath != null) {
                                val linkedFile = File(linkedWebPageSettings.filePath!!)
                                linkedFile.delete()
                                dbHelper.deleteWebPageSettings(linkedWebPageSettings.url)
                            }
                        }
                        webPageSettings.metadata[Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES] = "[]"
                    }
                } catch (e: Exception) {
                    Logs.error(TAG, "Error deleting linked pages for: $webPage", e)
                }
                
                dbHelper.updateWebPageSettings(webPageSettings)
            }
        }
    }

    private suspend fun updateReadStatus(webPages: List<WebPage>, markRead: Boolean) = withContext(Dispatchers.IO) {
        val novel = currentNovel ?: return@withContext
        val settings = chapterSettings?.toMutableList() ?: return@withContext
        
        dbHelper.writableDatabase.runTransaction { writableDatabase ->
            webPages.forEach { webPage ->
                val webPageSettings = settings.firstOrNull { it.url == webPage.url }
                if (webPageSettings != null) {
                    // Update the read status
                    webPageSettings.isRead = markRead
                    if (!markRead) {
                        // Remove scroll position when marking as unread
                        webPageSettings.metadata.remove(Constants.MetaDataKeys.SCROLL_POSITION)
                    }
                    dbHelper.updateWebPageSettingsReadStatus(webPageSettings, markRead, writableDatabase)
                }
            }
        }
        
        // Reload settings to ensure consistency
        chapterSettings = dbHelper.getAllWebPageSettings(novel.id)
    }

    private suspend fun updateFavoriteStatus(webPages: List<WebPage>, favoriteStatus: Boolean) = withContext(Dispatchers.IO) {
        val novel = currentNovel ?: return@withContext
        val settings = chapterSettings?.toMutableList() ?: return@withContext
        
        dbHelper.writableDatabase.runTransaction { writableDatabase ->
            webPages.forEach { webPage ->
                val webPageSettings = settings.firstOrNull { it.url == webPage.url }
                if (webPageSettings != null) {
                    // Update favorite status in metadata
                    webPageSettings.metadata[Constants.MetaDataKeys.IS_FAVORITE] = favoriteStatus.toString()
                    dbHelper.updateWebPageSettings(webPageSettings, writableDatabase)
                }
            }
        }
        
        // Reload settings to ensure consistency
        chapterSettings = dbHelper.getAllWebPageSettings(novel.id)
    }

    /**
     * Get download progress information for chapters
     */
    fun getChapterDownloadProgress(novelId: Long): Map<String, Boolean> {
        val settings = chapterSettings ?: return emptyMap()
        return settings.associate { setting ->
            setting.url to (setting.filePath != null)
        }
    }

    /**
     * Check if a chapter is downloaded
     */
    fun isChapterDownloaded(chapterUrl: String): Boolean {
        return chapterSettings?.firstOrNull { it.url == chapterUrl }?.filePath != null
    }

    /**
     * Get chapter read status
     */
    fun getChapterReadStatus(chapterUrl: String): Boolean {
        return chapterSettings?.firstOrNull { it.url == chapterUrl }?.isRead ?: false
    }

    /**
     * Get chapter favorite status
     */
    fun isChapterFavorite(chapterUrl: String): Boolean {
        return chapterSettings?.firstOrNull { it.url == chapterUrl }
            ?.metadata?.get(Constants.MetaDataKeys.IS_FAVORITE)?.toBoolean() ?: false
    }

    /**
     * Actions that can be performed on chapters.
     */
    enum class ChapterAction {
        ADD_DOWNLOADS,
        DELETE_DOWNLOADS,
        MARK_READ,
        MARK_UNREAD,
        MARK_FAVORITE,
        REMOVE_FAVORITE
    }
}
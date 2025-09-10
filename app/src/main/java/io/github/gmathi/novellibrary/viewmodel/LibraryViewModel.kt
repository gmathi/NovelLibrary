package io.github.gmathi.novellibrary.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.model.UiState
import io.github.gmathi.novellibrary.model.LibraryUiState
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.NovelSection
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.model.other.NovelEvent
import io.github.gmathi.novellibrary.model.other.NovelSectionEvent
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.network.sync.NovelSync
import io.github.gmathi.novellibrary.source.NovelUpdatesSource
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import java.util.LinkedList
import javax.inject.Inject

/**
 * ViewModel for LibraryFragment that manages library novels state and operations.
 * Follows the established architecture patterns with Hilt injection and UiState management.
 */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val dbHelper: DBHelper,
    private val dataCenter: DataCenter,
    private val networkHelper: NetworkHelper,
    private val sourceManager: SourceManager
) : BaseViewModel() {

    companion object {
        private const val TAG = "LibraryViewModel"
    }

    // UI State for library novels using LibraryUiState
    private val _uiState = MutableLiveData<LibraryUiState>()
    val uiState: LiveData<LibraryUiState> = _uiState

    // Sync progress state
    private val _syncProgress = MutableLiveData<SyncProgress>()
    val syncProgress: LiveData<SyncProgress> = _syncProgress

    // Current novel section ID
    private var currentNovelSectionId: Long = -1L

    override fun getTag(): String = TAG

    /**
     * Load novels for the specified section
     */
    fun loadNovels(novelSectionId: Long) {
        currentNovelSectionId = novelSectionId
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Loading
            try {
                val novels = withContext(Dispatchers.IO) {
                    dbHelper.getAllNovels(novelSectionId)
                }
                _uiState.value = LibraryUiState.Success(novels)
            } catch (e: Exception) {
                Logs.error(TAG, "Error loading novels", e)
                _uiState.value = LibraryUiState.Error(e.message ?: "Failed to load novels")
            }
        }
    }

    /**
     * Refresh novels data
     */
    fun refreshNovels() {
        loadNovels(currentNovelSectionId)
    }

    /**
     * Update order IDs for novels based on current list order
     */
    fun updateOrderIds(novels: List<Novel>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                novels.forEachIndexed { index, novel ->
                    dbHelper.updateNovelOrderId(novel.id, index.toLong())
                }
            }
        }
    }

    /**
     * Sort novels alphabetically
     */
    fun sortNovelsAlphabetically(ascending: Boolean = true) {
        val currentState = _uiState.value
        if (currentState is LibraryUiState.Success) {
            val sortedNovels = if (ascending) {
                currentState.novels.sortedWith(compareBy { it.name })
            } else {
                currentState.novels.sortedWith(compareBy<Novel> { it.name }.reversed())
            }
            _uiState.value = currentState.copy(novels = sortedNovels)
            updateOrderIds(sortedNovels)
        }
    }

    /**
     * Delete a novel from the library
     */
    fun deleteNovel(novel: Novel) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dbHelper.deleteNovel(novel.id)
            }
            refreshNovels()
        }
    }

    /**
     * Delete multiple novels from the library
     */
    fun deleteNovels(novels: List<Novel>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                novels.forEach { novel ->
                    dbHelper.deleteNovel(novel.id)
                }
            }
            clearSelection()
            refreshNovels()
        }
    }

    /**
     * Reset a novel (re-download chapters)
     */
    fun resetNovel(novel: Novel) {
        viewModelScope.launch {
            if (!networkHelper.isConnectedToNetwork()) {
                setError("You need to be connected to Internet to Hard Reset.")
                return@launch
            }
            
            withContext(Dispatchers.IO) {
                try {
                    // Use the extension function from NovelHelper
                    dbHelper.resetNovel(novel, sourceManager)
                } catch (e: Exception) {
                    Logs.error(TAG, "Error resetting novel: $novel", e)
                    throw e
                }
            }
            refreshNovels()
        }
    }

    /**
     * Reset multiple novels
     */
    fun resetNovels(novels: List<Novel>) {
        viewModelScope.launch {
            if (!networkHelper.isConnectedToNetwork()) {
                setError("You need to be connected to Internet to Hard Reset.")
                return@launch
            }
            
            withContext(Dispatchers.IO) {
                novels.forEach { novel ->
                    try {
                        dbHelper.resetNovel(novel, sourceManager)
                    } catch (e: Exception) {
                        Logs.error(TAG, "Error resetting novel: $novel", e)
                    }
                }
            }
            clearSelection()
            refreshNovels()
        }
    }

    /**
     * Assign novel to a different section
     */
    fun assignNovelToSection(novel: Novel, novelSectionId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dbHelper.updateNovelSectionId(novel.id, novelSectionId)
                EventBus.getDefault().post(NovelSectionEvent(novelSectionId))
                
                // Handle sync if needed
                val novelSections = dbHelper.getAllNovelSections()
                NovelSync.getInstance(novel, dbHelper, dataCenter, networkHelper, sourceManager)?.applyAsync(viewModelScope) { novelSync ->
                    if (dataCenter.getSyncAddNovels(novelSync.host)) {
                        novelSync.updateNovel(novel, novelSections.firstOrNull { section -> section.id == novelSectionId })
                    }
                }
            }
            refreshNovels()
        }
    }

    /**
     * Assign multiple novels to a section
     */
    fun assignNovelsToSection(novels: List<Novel>, novelSectionId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                novels.forEach { novel ->
                    dbHelper.updateNovelSectionId(novel.id, novelSectionId)
                }
                EventBus.getDefault().post(NovelSectionEvent(novelSectionId))
            }
            clearSelection()
            refreshNovels()
        }
    }

    /**
     * Sync novels with remote sources
     */
    fun syncNovels(specificNovel: Novel? = null) {
        if (!networkHelper.isConnectedToNetwork()) {
            setError("No internet connection available")
            return
        }

        viewModelScope.launch {
            val novels = if (specificNovel == null) {
                withContext(Dispatchers.IO) { dbHelper.getAllNovels(currentNovelSectionId) }
            } else {
                listOf(specificNovel)
            }

            _syncProgress.value = SyncProgress.Started("Sync in progress - Please wait")

            withContext(Dispatchers.IO) {
                var counter = 0
                val waitList = LinkedList<kotlinx.coroutines.Deferred<Boolean>>()
                val totalChaptersMap: HashMap<Novel, ArrayList<WebPage>> = HashMap()

                _syncProgress.postValue(SyncProgress.Progress(0, novels.count(), "Fetching chapters..."))

                novels.forEach { novel ->
                    waitList.add(async {
                        try {
                            val newChaptersList = withContext(Dispatchers.IO) {
                                val source = sourceManager.get(novel.sourceId)
                                if (source is NovelUpdatesSource)
                                    source.getUnsortedChapterList(novel)
                                else
                                    source?.getChapterList(novel)
                            } ?: ArrayList()

                            var currentChaptersHashCode = (novel.metadata[Constants.MetaDataKeys.HASH_CODE] ?: "0").toInt()
                            if (currentChaptersHashCode == 0)
                                currentChaptersHashCode = dbHelper.getAllWebPages(novel.id).sumOf { webPage -> webPage.hashCode() }
                            val newChaptersHashCode = newChaptersList.sumOf { webPage -> webPage.hashCode() }

                            if (newChaptersList.isNotEmpty() && newChaptersHashCode != currentChaptersHashCode) {
                                novel.metadata[Constants.MetaDataKeys.HASH_CODE] = newChaptersHashCode.toString()
                                totalChaptersMap[novel] = ArrayList(newChaptersList)
                            }

                            val message = "Sync done fetching chapters for ${novel.name}, ${novels.count() - counter++} remaining"
                            _syncProgress.postValue(SyncProgress.Progress(counter, novels.count(), message))

                        } catch (e: Exception) {
                            Logs.error(TAG, "Novel: $novel", e)
                        }
                        true
                    })
                }

                waitList.awaitAll()

                // Update DB with new chapters
                waitList.clear()
                counter = 0
                _syncProgress.postValue(SyncProgress.Progress(0, totalChaptersMap.count(), "Updating chapters..."))

                totalChaptersMap.forEach { (novelToUpdate, chapters) ->
                    counter++
                    val message = "Sync fetching chapter list $counter/${totalChaptersMap.count()} - ${novelToUpdate.name}"
                    _syncProgress.postValue(SyncProgress.Progress(counter, totalChaptersMap.count(), message))

                    novelToUpdate.metadata[Constants.MetaDataKeys.LAST_UPDATED_DATE] = Utils.getCurrentFormattedDate()

                    var newChaptersCount = chapters.size - novelToUpdate.chaptersCount
                    if (newChaptersCount <= 0) {
                        newChaptersCount = 0
                    }
                    val newReleasesCount = novelToUpdate.newReleasesCount + newChaptersCount
                    novelToUpdate.chaptersCount = chapters.size.toLong()
                    novelToUpdate.newReleasesCount = newReleasesCount
                    dbHelper.updateNovelMetaData(novelToUpdate)
                    dbHelper.updateChaptersAndReleasesCount(novelToUpdate.id, chapters.size.toLong(), newReleasesCount)

                    try {
                        var finalChapters = chapters
                        if (novelToUpdate.sourceId == Constants.SourceId.NOVEL_UPDATES) {
                            finalChapters = ArrayList(sourceManager.get(novelToUpdate.sourceId)?.getChapterList(novelToUpdate) ?: emptyList())
                        }
                        
                        waitList.add(async {
                            val writableDatabase = dbHelper.writableDatabase
                            writableDatabase.beginTransaction()
                            try {
                                for (i in finalChapters.indices) {
                                    dbHelper.createWebPage(finalChapters[i], writableDatabase)
                                    dbHelper.createWebPageSettings(WebPageSettings(finalChapters[i].url, novelToUpdate.id), writableDatabase)
                                }
                                writableDatabase.setTransactionSuccessful()
                            } finally {
                                writableDatabase.endTransaction()
                            }
                            true
                        })

                    } catch (e: Exception) {
                        Logs.error(TAG, "Novel: $novelToUpdate", e)
                    }
                }

                waitList.awaitAll()
            }

            _syncProgress.value = SyncProgress.Completed
            refreshNovels()
        }
    }

    /**
     * Get all novel sections for assignment
     */
    fun getNovelSections(): List<NovelSection> {
        return try {
            dbHelper.getAllNovelSections()
        } catch (e: Exception) {
            Logs.error(TAG, "Error getting novel sections", e)
            emptyList()
        }
    }

    /**
     * Add novel to selection
     */
    fun addToSelection(novel: Novel) {
        val currentState = _uiState.value
        if (currentState is LibraryUiState.Success) {
            val newSelection = currentState.selectedNovels + novel
            _uiState.value = currentState.copy(selectedNovels = newSelection)
        }
    }

    /**
     * Remove novel from selection
     */
    fun removeFromSelection(novel: Novel) {
        val currentState = _uiState.value
        if (currentState is LibraryUiState.Success) {
            val newSelection = currentState.selectedNovels - novel
            _uiState.value = currentState.copy(selectedNovels = newSelection)
        }
    }

    /**
     * Add multiple novels to selection
     */
    fun addToSelection(novels: List<Novel>) {
        val currentState = _uiState.value
        if (currentState is LibraryUiState.Success) {
            val newSelection = currentState.selectedNovels + novels
            _uiState.value = currentState.copy(selectedNovels = newSelection)
        }
    }

    /**
     * Remove multiple novels from selection
     */
    fun removeFromSelection(novels: List<Novel>) {
        val currentState = _uiState.value
        if (currentState is LibraryUiState.Success) {
            val newSelection = currentState.selectedNovels - novels.toSet()
            _uiState.value = currentState.copy(selectedNovels = newSelection)
        }
    }

    /**
     * Select all novels
     */
    fun selectAll() {
        val currentState = _uiState.value
        if (currentState is LibraryUiState.Success) {
            _uiState.value = currentState.copy(selectedNovels = currentState.novels.toSet())
        }
    }

    /**
     * Clear selection
     */
    fun clearSelection() {
        val currentState = _uiState.value
        if (currentState is LibraryUiState.Success) {
            _uiState.value = currentState.copy(selectedNovels = emptySet())
        }
    }

    /**
     * Select interval between first and last selected novels
     */
    fun selectInterval() {
        val currentState = _uiState.value
        if (currentState is LibraryUiState.Success && currentState.selectedNovels.isNotEmpty()) {
            val novels = currentState.novels
            val indexes = currentState.selectedNovels.map { novels.indexOf(it) }.sorted()
            if (indexes.isNotEmpty()) {
                val subList = novels.subList(indexes.first(), indexes.last() + 1)
                val newSelection = currentState.selectedNovels + subList
                _uiState.value = currentState.copy(selectedNovels = newSelection)
            }
        }
    }

    /**
     * Move novel in the list (for drag and drop)
     */
    fun moveNovel(fromPosition: Int, toPosition: Int) {
        val currentState = _uiState.value
        if (currentState is LibraryUiState.Success) {
            val novels = currentState.novels.toMutableList()
            if (fromPosition < novels.size && toPosition < novels.size) {
                val novel = novels.removeAt(fromPosition)
                novels.add(toPosition, novel)
                _uiState.value = currentState.copy(novels = novels)
                updateOrderIds(novels)
            }
        }
    }

    /**
     * Update new releases count for a novel
     */
    fun updateNewReleasesCount(novelId: Long, count: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dbHelper.updateNewReleasesCount(novelId, count)
            }
        }
    }
}

/**
 * Sealed class representing sync progress states
 */
sealed class SyncProgress {
    data class Started(val message: String) : SyncProgress()
    data class Progress(val current: Int, val total: Int, val message: String) : SyncProgress()
    object Completed : SyncProgress()
}
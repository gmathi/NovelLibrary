package io.github.gmathi.novellibrary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.addTagToNovel
import io.github.gmathi.novellibrary.database.createNovelTag
import io.github.gmathi.novellibrary.database.deleteNovelTag
import io.github.gmathi.novellibrary.database.getAllNovelTagLinks
import io.github.gmathi.novellibrary.database.getAllNovelTags
import io.github.gmathi.novellibrary.database.getAllNovels
import io.github.gmathi.novellibrary.database.getGenres
import io.github.gmathi.novellibrary.database.getNovelCountPerTag
import io.github.gmathi.novellibrary.database.removeTagFromNovel
import io.github.gmathi.novellibrary.database.setNovelTags
import io.github.gmathi.novellibrary.database.updateNovelTagName
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.NovelTag
import io.github.gmathi.novellibrary.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale

/** Well-defined sort options for the new library. */
enum class LibrarySort(val label: String) {
    TITLE_ASC("Title (A → Z)"),
    TITLE_DESC("Title (Z → A)"),
    LAST_READ_NEWEST("Last read (newest first)"),
    LAST_READ_OLDEST("Last read (oldest first)"),
    LAST_UPDATED_NEWEST("Last updated (newest first)"),
    LAST_UPDATED_OLDEST("Last updated (oldest first)"),
    RECENTLY_ADDED_NEWEST("Recently added (newest first)"),
    RECENTLY_ADDED_OLDEST("Recently added (oldest first)"),
    RECENTLY_DOWNLOADED_NEWEST("Recently downloaded (newest first)"),
    RECENTLY_DOWNLOADED_OLDEST("Recently downloaded (oldest first)"),
    UNREAD_COUNT_DESC("Most unread chapters"),
    UNREAD_COUNT_ASC("Fewest unread chapters")
}

/** How selected tag filters combine. */
enum class TagFilterMode { ANY, ALL }

data class LibraryTag(
    val tag: NovelTag,
    val novelCount: Int
)

data class NewLibraryUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    /** True while a full-library sync or cover-refresh is running (hides the sync action). */
    val syncing: Boolean = false,
    /** Novels after filter + sort, ready to render. */
    val novels: List<Novel> = emptyList(),
    /** All tags with their novel counts, for the filter bar / assignment dialogs. */
    val tags: List<LibraryTag> = emptyList(),
    /** novelId -> ordered tag ids currently applied, for chips on each card. */
    val novelTagIds: Map<Long, List<Long>> = emptyMap(),
    val selectedTagIds: Set<Long> = emptySet(),
    val tagFilterMode: TagFilterMode = TagFilterMode.ANY,
    val sort: LibrarySort = LibrarySort.TITLE_ASC,
    val query: String = ""
) {
    val isEmpty: Boolean get() = !isLoading && novels.isEmpty()
}

class NewLibraryViewModel : ViewModel() {

    private val dbHelper: DBHelper by injectLazy()

    private val _uiState = MutableStateFlow(NewLibraryUiState())
    val uiState: StateFlow<NewLibraryUiState> = _uiState.asStateFlow()

    /** Full, unfiltered, unsorted novel set from the DB. */
    private var allNovels: List<Novel> = emptyList()
    private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

    /** Set by the host while a full-library sync / cover-refresh runs, so the top bar can react. */
    fun setSyncing(syncing: Boolean) {
        _uiState.value = _uiState.value.copy(syncing = syncing)
    }

    fun load(isRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = !isRefresh,
                isRefreshing = isRefresh
            )
            val (novels, tags, links, counts) = withContext(Dispatchers.IO) {
                val novels = dbHelper.getAllNovels().onEach { novel ->
                    if (novel.genres == null) novel.genres = dbHelper.getGenres(novel.id)
                }
                val tags = dbHelper.getAllNovelTags()
                val links = dbHelper.getAllNovelTagLinks()
                val counts = dbHelper.getNovelCountPerTag()
                LibraryData(novels, tags, links, counts)
            }
            allNovels = novels
            // Drop any selected filter tag that no longer exists.
            val liveTagIds = tags.map { it.id }.toSet()
            val selected = _uiState.value.selectedTagIds.intersect(liveTagIds)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                tags = tags.map { LibraryTag(it, counts[it.id] ?: 0) },
                novelTagIds = links,
                selectedTagIds = selected
            )
            recompute()
        }
    }

    private data class LibraryData(
        val novels: List<Novel>,
        val tags: List<NovelTag>,
        val links: Map<Long, List<Long>>,
        val counts: Map<Long, Int>
    )

    fun setSort(sort: LibrarySort) {
        _uiState.value = _uiState.value.copy(sort = sort)
        recompute()
    }

    fun setQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        recompute()
    }

    fun toggleTagFilter(tagId: Long) {
        val current = _uiState.value.selectedTagIds
        val next = if (current.contains(tagId)) current - tagId else current + tagId
        _uiState.value = _uiState.value.copy(selectedTagIds = next)
        recompute()
    }

    fun clearTagFilters() {
        _uiState.value = _uiState.value.copy(selectedTagIds = emptySet())
        recompute()
    }

    fun setTagFilterMode(mode: TagFilterMode) {
        _uiState.value = _uiState.value.copy(tagFilterMode = mode)
        recompute()
    }

    private fun recompute() {
        val state = _uiState.value
        val links = state.novelTagIds
        val selected = state.selectedTagIds

        var result = allNovels.asSequence()

        if (state.query.isNotBlank()) {
            result = result.filter { it.name.contains(state.query, ignoreCase = true) }
        }

        if (selected.isNotEmpty()) {
            result = result.filter { novel ->
                val novelTags = links[novel.id].orEmpty()
                when (state.tagFilterMode) {
                    TagFilterMode.ANY -> novelTags.any { it in selected }
                    TagFilterMode.ALL -> selected.all { it in novelTags }
                }
            }
        }

        val sorted = sortSequence(result.toList(), state.sort)
        _uiState.value = _uiState.value.copy(novels = sorted)
    }

    private fun sortSequence(novels: List<Novel>, sort: LibrarySort): List<Novel> = when (sort) {
        LibrarySort.TITLE_ASC -> novels.sortedBy { it.name.lowercase() }
        LibrarySort.TITLE_DESC -> novels.sortedByDescending { it.name.lowercase() }
        LibrarySort.RECENTLY_ADDED_NEWEST -> novels.sortedByDescending { it.id }
        LibrarySort.RECENTLY_ADDED_OLDEST -> novels.sortedBy { it.id }
        LibrarySort.LAST_READ_NEWEST -> byDate(novels, Constants.MetaDataKeys.LAST_READ_DATE, ascending = false)
        LibrarySort.LAST_READ_OLDEST -> byDate(novels, Constants.MetaDataKeys.LAST_READ_DATE, ascending = true)
        LibrarySort.LAST_UPDATED_NEWEST -> byDate(novels, Constants.MetaDataKeys.LAST_UPDATED_DATE, ascending = false)
        LibrarySort.LAST_UPDATED_OLDEST -> byDate(novels, Constants.MetaDataKeys.LAST_UPDATED_DATE, ascending = true)
        LibrarySort.RECENTLY_DOWNLOADED_NEWEST -> byDate(novels, Constants.MetaDataKeys.LAST_DOWNLOAD_DATE, ascending = false)
        LibrarySort.RECENTLY_DOWNLOADED_OLDEST -> byDate(novels, Constants.MetaDataKeys.LAST_DOWNLOAD_DATE, ascending = true)
        LibrarySort.UNREAD_COUNT_DESC -> novels.sortedByDescending { it.newReleasesCount }
        LibrarySort.UNREAD_COUNT_ASC -> novels.sortedBy { it.newReleasesCount }
    }

    /** Sort by a `d MMM yyyy` metadata date; novels without a parseable date always sort last. */
    private fun byDate(novels: List<Novel>, metadataKey: String, ascending: Boolean): List<Novel> {
        val sorted = novels.sortedBy { novel ->
            novel.metadata[metadataKey]?.let {
                try { dateFormat.parse(it)?.time } catch (e: Exception) { null }
            } ?: if (ascending) Long.MAX_VALUE else Long.MIN_VALUE
        }
        return if (ascending) sorted else sorted.reversed()
    }

    //region Tag mutations

    fun createTag(name: String, onDone: (Long) -> Unit = {}) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val id = withContext(Dispatchers.IO) { dbHelper.createNovelTag(trimmed) }
            load()
            onDone(id)
        }
    }

    fun renameTag(tagId: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { dbHelper.updateNovelTagName(tagId, trimmed) }
            load()
        }
    }

    fun deleteTag(tagId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { dbHelper.deleteNovelTag(tagId) }
            load()
        }
    }

    fun setNovelTags(novelId: Long, tagIds: Collection<Long>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { dbHelper.setNovelTags(novelId, tagIds) }
            load()
        }
    }

    fun addTagToNovel(novelId: Long, tagId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { dbHelper.addTagToNovel(novelId, tagId) }
            load()
        }
    }

    fun removeTagFromNovel(novelId: Long, tagId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { dbHelper.removeTagFromNovel(novelId, tagId) }
            load()
        }
    }

    /** Creates a tag (if needed) and applies it to the novel in one step. */
    fun createAndApplyTag(novelId: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val id = dbHelper.createNovelTag(trimmed)
                dbHelper.addTagToNovel(novelId, id)
            }
            load()
        }
    }

    //endregion

    /**
     * Reload after the host has deleted a novel. Deletion itself (downloaded files, broadcasts,
     * analytics) lives on the fragment via DataAccessor.deleteNovel, which needs an Android context,
     * so the VM only refreshes its view of the DB here.
     */
    fun refreshAfterExternalChange() {
        load()
    }
}

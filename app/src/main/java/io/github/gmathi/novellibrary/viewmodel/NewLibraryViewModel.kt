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
import io.github.gmathi.novellibrary.database.updateNovelTagColor
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

/** What to order the library by. Direction is chosen separately (see [SortDirection]). */
enum class SortKey(val label: String) {
    TITLE("Title"),
    LAST_READ("Last read"),
    LAST_UPDATED("Last updated"),
    RECENTLY_ADDED("Date added"),
    RECENTLY_DOWNLOADED("Date downloaded"),
    UNREAD_COUNT("Unread chapters")
}

/** Ascending / descending, applied on top of the chosen [SortKey]. */
enum class SortDirection(val label: String) {
    ASCENDING("Ascending"),
    DESCENDING("Descending")
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
    val sortKey: SortKey = SortKey.TITLE,
    val sortDirection: SortDirection = SortDirection.ASCENDING,
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

    fun setSortKey(key: SortKey) {
        _uiState.value = _uiState.value.copy(sortKey = key)
        recompute()
    }

    fun setSortDirection(direction: SortDirection) {
        _uiState.value = _uiState.value.copy(sortDirection = direction)
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

        val sorted = sortSequence(result.toList(), state.sortKey, state.sortDirection)
        _uiState.value = _uiState.value.copy(novels = sorted)
    }

    private fun sortSequence(novels: List<Novel>, key: SortKey, direction: SortDirection): List<Novel> {
        val ascending = direction == SortDirection.ASCENDING
        return when (key) {
            SortKey.TITLE ->
                if (ascending) novels.sortedBy { it.name.lowercase() }
                else novels.sortedByDescending { it.name.lowercase() }
            SortKey.RECENTLY_ADDED ->
                if (ascending) novels.sortedBy { it.id } else novels.sortedByDescending { it.id }
            SortKey.LAST_READ -> byDate(novels, Constants.MetaDataKeys.LAST_READ_DATE, ascending)
            SortKey.LAST_UPDATED -> byDate(novels, Constants.MetaDataKeys.LAST_UPDATED_DATE, ascending)
            SortKey.RECENTLY_DOWNLOADED -> byDate(novels, Constants.MetaDataKeys.LAST_DOWNLOAD_DATE, ascending)
            SortKey.UNREAD_COUNT ->
                if (ascending) novels.sortedBy { it.newReleasesCount }
                else novels.sortedByDescending { it.newReleasesCount }
        }
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

    /** Persist a per-tag ARGB color (null clears it back to the name-derived default). */
    fun updateTagColor(tagId: Long, color: Long?) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { dbHelper.updateNovelTagColor(tagId, color) }
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

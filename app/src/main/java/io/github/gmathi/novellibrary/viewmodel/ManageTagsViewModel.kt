package io.github.gmathi.novellibrary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.createNovelTag
import io.github.gmathi.novellibrary.database.deleteNovelTag
import io.github.gmathi.novellibrary.database.getAllNovelTags
import io.github.gmathi.novellibrary.database.getNovelCountPerTag
import io.github.gmathi.novellibrary.database.updateNovelTagColor
import io.github.gmathi.novellibrary.database.updateNovelTagName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.injectLazy

/**
 * Backs the settings "Library Tags" screen. Reads/writes the SAME tag model and persistence the
 * new library uses (via the DBHelper tag helpers), so edits here are immediately reflected in the
 * library and vice-versa.
 */
class ManageTagsViewModel : ViewModel() {

    private val dbHelper: DBHelper by injectLazy()

    private val _tags = MutableStateFlow<List<LibraryTag>>(emptyList())
    val tags: StateFlow<List<LibraryTag>> = _tags.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                val counts = dbHelper.getNovelCountPerTag()
                dbHelper.getAllNovelTags().map { LibraryTag(it, counts[it.id] ?: 0) }
            }
            _tags.value = list
        }
    }

    fun create(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { dbHelper.createNovelTag(trimmed) }
            load()
        }
    }

    fun rename(tagId: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { dbHelper.updateNovelTagName(tagId, trimmed) }
            load()
        }
    }

    fun setColor(tagId: Long, color: Long?) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { dbHelper.updateNovelTagColor(tagId, color) }
            load()
        }
    }

    fun delete(tagId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { dbHelper.deleteNovelTag(tagId) }
            load()
        }
    }
}

//TODO: DUPLICATE - REMOVAL NEEDED
// This entire file duplicates LibrarySearchActivity.kt (same package).
// Both implement Compose-based library search with nearly identical UI and logic.
// LibrarySearchActivity.kt is the canonical version (registered in AndroidManifest.xml).
// Remove this file and consolidate any unique features (e.g. novel section handling) into LibrarySearchActivity.kt.

package io.github.gmathi.novellibrary.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItems
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.compose.search.*
import io.github.gmathi.novellibrary.compose.theme.NovelLibraryTheme
import io.github.gmathi.novellibrary.database.getAllNovelSections
import io.github.gmathi.novellibrary.database.getAllNovels
import io.github.gmathi.novellibrary.database.updateNovelSectionId
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.other.NovelSectionEvent
import io.github.gmathi.novellibrary.network.sync.NovelSync
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.lang.addToLibrarySearchHistory
import io.github.gmathi.novellibrary.util.system.hideSoftKeyboard
import io.github.gmathi.novellibrary.util.system.startChaptersActivity
import io.github.gmathi.novellibrary.util.system.startNovelDetailsActivity
import io.github.gmathi.novellibrary.util.system.startReaderDBPagerActivity
import org.greenrobot.eventbus.EventBus

/**
 * Compose-based Library Search Activity
 * Replaces the old XML-based LibrarySearchActivity
 */
class LibrarySearchActivityCompose : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NovelLibraryTheme {
                LibrarySearchScreen(
                    onBack = { finish() },
                    onNovelClick = { novel -> startChaptersActivity(novel) },
                    onNovelDetails = { novel -> startNovelDetailsActivity(novel, false) },
                    onReadChapter = { novel -> startReader(novel) }
                )
            }
        }
    }

    private fun startReader(novel: Novel) {
        if (novel.currentChapterUrl != null) {
            startReaderDBPagerActivity(novel)
        } else {
            MaterialDialog(this).show {
                title(R.string.no_bookmark_found_dialog_title)
                message(text = getString(R.string.no_bookmark_found_dialog_description, novel.name))
                positiveButton(R.string.okay) { dialog ->
                    startChaptersActivity(novel, false)
                    dialog.dismiss()
                }
                negativeButton(R.string.cancel)
                lifecycleOwner(this@LibrarySearchActivityCompose)
            }
        }
    }
}

@Composable
fun LibrarySearchScreen(
    onBack: () -> Unit,
    onNovelClick: (Novel) -> Unit,
    onNovelDetails: (Novel) -> Unit,
    onReadChapter: (Novel) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? BaseActivity
    
    var searchResults by remember { mutableStateOf<List<Novel>>(emptyList()) }
    var allNovels by remember { mutableStateOf<List<Novel>>(emptyList()) }
    
    val searchState = rememberPersistentSearchState()
    val searchHistory = remember { 
        activity?.dataCenter?.loadLibrarySearchHistory() ?: emptyList()
    }
    val suggestionBuilder = remember(searchHistory) {
        HistorySearchSuggestionsBuilder(searchHistory)
    }

    // Load all novels
    LaunchedEffect(Unit) {
        activity?.let {
            allNovels = it.dbHelper.getAllNovels()
            searchResults = allNovels
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search View
                PersistentSearchView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    state = searchState,
                    hint = "Search library...",
                    homeButtonMode = HomeButtonMode.Arrow,
                    onHomeButtonClick = {
                        activity?.hideSoftKeyboard()
                        onBack()
                    },
                    onSearch = { query ->
                        query.addToLibrarySearchHistory()
                    },
                    onSearchTermChanged = { term ->
                        searchResults = if (term.isEmpty()) {
                            allNovels
                        } else {
                            allNovels.filter { novel ->
                                novel.name.contains(term, ignoreCase = true)
                            }
                        }
                    },
                    suggestionBuilder = suggestionBuilder,
                    elevation = 4
                )

                // Results
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults, key = { "${it.id}_${it.url}" }) { novel ->
                        LibraryNovelItem(
                            novel = novel,
                            onClick = { onNovelClick(novel) },
                            onDetails = { onNovelDetails(novel) },
                            onRead = { onReadChapter(novel) }
                        )
                    }
                }
            }

            // Background tint overlay
            SearchBackgroundTint(
                visible = searchState.isEditing,
                onClick = { searchState.closeSearch() }
            )
        }
    }
}

@Composable
fun LibraryNovelItem(
    novel: Novel,
    onClick: () -> Unit,
    onDetails: () -> Unit,
    onRead: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = novel.name,
                    style = MaterialTheme.typography.titleMedium
                )
                
                val lastRead = novel.metadata[Constants.MetaDataKeys.LAST_READ_DATE] ?: "N/A"
                val lastUpdated = novel.metadata[Constants.MetaDataKeys.LAST_UPDATED_DATE] ?: "N/A"
                
                Text(
                    text = "Last read: $lastRead",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Last updated: $lastUpdated",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

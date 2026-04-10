package io.github.gmathi.novellibrary.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.compose.search.*
import io.github.gmathi.novellibrary.compose.theme.NovelLibraryTheme
import io.github.gmathi.novellibrary.database.getAllNovels
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.lang.addToLibrarySearchHistory
import io.github.gmathi.novellibrary.util.system.hideSoftKeyboard
import io.github.gmathi.novellibrary.util.system.startChaptersActivity
import io.github.gmathi.novellibrary.util.system.startNovelDetailsActivity
import io.github.gmathi.novellibrary.util.system.startReaderDBPagerActivity

class LibrarySearchActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NovelLibraryTheme {
                LibrarySearchScreen()
            }
        }
    }

    @Composable
    private fun LibrarySearchScreen() {
        var searchResults by remember { mutableStateOf<List<Novel>>(emptyList()) }
        var allNovels by remember { mutableStateOf<List<Novel>>(emptyList()) }
        
        val searchState = rememberPersistentSearchState()
        var searchHistory by remember { mutableStateOf(dataCenter.loadLibrarySearchHistory()) }
        val suggestionBuilder = remember(searchHistory) {
            HistorySearchSuggestionsBuilder(searchHistory)
        }

        // Load all novels
        LaunchedEffect(Unit) {
            allNovels = dbHelper.getAllNovels()
            searchResults = allNovels
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
                        hint = getString(R.string.search_novel),
                        homeButtonMode = HomeButtonMode.Arrow,
                        onHomeButtonClick = {
                            hideSoftKeyboard()
                            finish()
                        },
                        onSearch = { query ->
                            query.addToLibrarySearchHistory()
                            searchHistory = dataCenter.loadLibrarySearchHistory()
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
                                onClick = { startChaptersActivity(novel) },
                                onDetails = { startNovelDetailsActivity(novel, false) },
                                onRead = { startReader(novel) }
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
    private fun LibraryNovelItem(
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = novel.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    val lastRead = novel.metadata[Constants.MetaDataKeys.LAST_READ_DATE] ?: "N/A"
                    val lastUpdated = novel.metadata[Constants.MetaDataKeys.LAST_UPDATED_DATE] ?: "N/A"
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Last read: $lastRead | Updated: $lastUpdated",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = onDetails) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More options"
                    )
                }
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
                lifecycleOwner(this@LibrarySearchActivity)
            }
        }
    }
}

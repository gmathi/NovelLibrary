package io.github.gmathi.novellibrary.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.compose.library.LibrarySearchScreen
import io.github.gmathi.novellibrary.compose.theme.NovelLibraryTheme
import io.github.gmathi.novellibrary.database.getAllNovels
import io.github.gmathi.novellibrary.database.getGenres
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.util.lang.addToLibrarySearchHistory
import io.github.gmathi.novellibrary.util.system.hideSoftKeyboard
import io.github.gmathi.novellibrary.util.system.startChaptersActivity
import io.github.gmathi.novellibrary.util.system.startNovelDetailsActivity
import io.github.gmathi.novellibrary.util.system.startReaderDBPagerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LibrarySearchActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NovelLibraryTheme {
                var allNovels by remember { mutableStateOf<List<Novel>>(emptyList()) }
                var searchHistory by remember { mutableStateOf(dataCenter.loadLibrarySearchHistory()) }

                LaunchedEffect(Unit) {
                    allNovels = withContext(Dispatchers.IO) {
                        dbHelper.getAllNovels().onEach { novel ->
                            if (novel.genres == null) {
                                novel.genres = dbHelper.getGenres(novel.id)
                            }
                        }
                    }
                }

                LibrarySearchScreen(
                    allNovels = allNovels,
                    searchHistory = searchHistory,
                    searchHint = getString(R.string.search_novel),
                    onSearch = { query ->
                        query.addToLibrarySearchHistory()
                        searchHistory = dataCenter.loadLibrarySearchHistory()
                    },
                    onBackClick = {
                        hideSoftKeyboard()
                        finish()
                    },
                    onNovelClick = { novel -> startChaptersActivity(novel) },
                    onNovelDetailsClick = { novel -> startNovelDetailsActivity(novel, false) },
                    onNovelReadClick = { novel -> startReader(novel) }
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
                lifecycleOwner(this@LibrarySearchActivity)
            }
        }
    }
}

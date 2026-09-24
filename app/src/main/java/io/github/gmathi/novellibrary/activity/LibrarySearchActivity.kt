package io.github.gmathi.novellibrary.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItems
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.compose.library.LibrarySearchScreen
import io.github.gmathi.novellibrary.compose.theme.NovelLibraryTheme
import io.github.gmathi.novellibrary.database.getAllNovelSections
import io.github.gmathi.novellibrary.database.getAllNovels
import io.github.gmathi.novellibrary.database.getGenres
import io.github.gmathi.novellibrary.database.updateNovelSectionId
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.NovelSection
import io.github.gmathi.novellibrary.model.other.NovelSectionEvent
import io.github.gmathi.novellibrary.network.sync.NovelSync
import io.github.gmathi.novellibrary.util.lang.addToLibrarySearchHistory
import io.github.gmathi.novellibrary.util.system.hideSoftKeyboard
import io.github.gmathi.novellibrary.util.system.startChaptersActivity
import io.github.gmathi.novellibrary.util.system.startNovelDetailsActivity
import io.github.gmathi.novellibrary.util.system.startReaderDBPagerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

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
                    onNovelReadClick = { novel -> startReader(novel) },
                    onNovelAssignSection = { novel -> showNovelSectionsList(novel) }
                )
            }
        }
    }

    /**
     * Shows a picker of the user's novel sections and assigns the given novel to the chosen one.
     * The search screen is not scoped to any section, so the default section ("Currently Reading",
     * id -1L) is offered at the top of the list alongside every user-created section.
     */
    private fun showNovelSectionsList(novel: Novel) {
        val novelSections = ArrayList(dbHelper.getAllNovelSections())
        if (novelSections.isEmpty()) {
            MaterialDialog(this).show {
                message(R.string.no_novel_sections_error)
                lifecycleOwner(this@LibrarySearchActivity)
            }
            return
        }

        val sectionNames = ArrayList<String>()
        sectionNames.add(getString(R.string.default_novel_section_name))
        sectionNames.addAll(novelSections.map { it.name ?: "" })

        MaterialDialog(this).show {
            title(text = getString(R.string.assign_novel_section))
            listItems(items = sectionNames.toList()) { _, which, _ ->
                val targetSectionId = if (which == 0) -1L else novelSections[which - 1].id
                assignNovelToSection(novel, novelSections, targetSectionId)
            }
            lifecycleOwner(this@LibrarySearchActivity)
        }
    }

    private fun assignNovelToSection(novel: Novel, novelSections: ArrayList<NovelSection>, novelSectionId: Long) {
        dbHelper.updateNovelSectionId(novel.id, novelSectionId)
        EventBus.getDefault().post(NovelSectionEvent(novelSectionId))
        NovelSync.getInstance(novel)?.applyAsync(lifecycleScope) { novelSync ->
            if (dataCenter.getSyncAddNovels(novelSync.host)) {
                novelSync.updateNovel(novel, novelSections.firstOrNull { section -> section.id == novelSectionId })
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

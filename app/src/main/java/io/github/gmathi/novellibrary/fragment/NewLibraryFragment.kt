package io.github.gmathi.novellibrary.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.bumptech.glide.Glide
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.NavDrawerActivity
import io.github.gmathi.novellibrary.compose.library.NewLibraryActions
import io.github.gmathi.novellibrary.compose.library.NewLibraryScreen
import io.github.gmathi.novellibrary.compose.theme.NovelLibraryTheme
import io.github.gmathi.novellibrary.database.getAllNovels
import io.github.gmathi.novellibrary.database.getNovelByUrl
import io.github.gmathi.novellibrary.database.getTagIdsForNovel
import io.github.gmathi.novellibrary.database.resetNovel
import io.github.gmathi.novellibrary.database.setNovelTags
import io.github.gmathi.novellibrary.database.updateChaptersAndReleasesCount
import io.github.gmathi.novellibrary.database.updateNewReleasesCount
import io.github.gmathi.novellibrary.database.updateNovel
import io.github.gmathi.novellibrary.database.updateNovelMetaData
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.model.source.online.NovelUpdatesSource
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.lang.DomainThrottle
import io.github.gmathi.novellibrary.util.logging.Logs
import io.github.gmathi.novellibrary.database.runTransaction
import io.github.gmathi.novellibrary.database.createWebPage
import io.github.gmathi.novellibrary.database.createWebPageSettings
import io.github.gmathi.novellibrary.database.getAllWebPages
import io.github.gmathi.novellibrary.util.system.deleteNovel
import io.github.gmathi.novellibrary.util.system.startChaptersActivity
import io.github.gmathi.novellibrary.util.system.startLibrarySearchActivity
import io.github.gmathi.novellibrary.util.system.startNovelDetailsActivity
import io.github.gmathi.novellibrary.util.system.startReaderDBPagerActivity
import io.github.gmathi.novellibrary.viewmodel.NewLibraryViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.LinkedList

/**
 * Hosts the new Compose, tag-based library ([NewLibraryScreen]). It is additive: the legacy
 * [LibraryPagerFragment] is untouched and still reachable when the user turns the new library off
 * in settings (DataCenter.useNewLibrary). Sync, refresh-covers and novel deletion need an Android
 * context/source manager, so they live here and the Compose screen only renders + delegates.
 */
class NewLibraryFragment : BaseFragment() {

    private lateinit var viewModel: NewLibraryViewModel

    /** Re-entrancy guard for sync/refresh; the Compose-observable flag lives in the ViewModel. */
    @Volatile
    private var syncing: Boolean = false

    private fun beginSyncing() {
        syncing = true
        viewModel.setSyncing(true)
    }

    private fun endSyncing() {
        syncing = false
        viewModel.setSyncing(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[NewLibraryViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            NovelLibraryTheme {
                NewLibraryScreen(
                    viewModel = viewModel,
                    actions = NewLibraryActions(
                        onMenuClick = {
                            (activity as? NavDrawerActivity)?.binding?.drawerLayout?.openDrawer(GravityCompat.START)
                        },
                        onNovelClick = { novel -> (activity as? AppCompatActivity)?.startChaptersActivity(novel) },
                        onNovelDetails = { novel -> (activity as? AppCompatActivity)?.startNovelDetailsActivity(novel, false) },
                        onNovelRead = { novel -> startReader(novel) },
                        onNovelRemove = { novel -> confirmRemove(novel) },
                        onNovelHardReset = { novel -> confirmHardReset(novel) },
                        onSync = { startSync() },
                        onRefreshCovers = { startRefreshCovers() },
                        onSearchClick = { (activity as? AppCompatActivity)?.startLibrarySearchActivity() }
                    )
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    private fun startReader(novel: Novel) {
        if (novel.currentChapterUrl != null) {
            dbHelper.updateNewReleasesCount(novel.id, 0L)
            (activity as? AppCompatActivity)?.startReaderDBPagerActivity(novel)
        } else {
            (activity as? AppCompatActivity)?.let {
                MaterialDialog(it).show {
                    title(R.string.no_bookmark_found_dialog_title)
                    message(R.string.no_bookmark_found_dialog_description, novel.name)
                    positiveButton(R.string.okay) { dialog ->
                        it.startChaptersActivity(novel, false)
                        dialog.dismiss()
                    }
                    negativeButton(R.string.cancel)
                    lifecycleOwner(this@NewLibraryFragment)
                }
            }
        }
    }

    private fun confirmRemove(novel: Novel) {
        activity?.let { act ->
            MaterialDialog(act).show {
                title(R.string.confirm_remove)
                message(R.string.confirm_remove_description_novel)
                positiveButton(R.string.remove) { dialog ->
                    this@NewLibraryFragment.deleteNovel(novel)
                    viewModel.refreshAfterExternalChange()
                    dialog.dismiss()
                }
                negativeButton(R.string.cancel)
                lifecycleOwner(this@NewLibraryFragment)
            }
        }
    }

    /**
     * Hard reset: delete ALL of a novel's local data (chapters, webpage settings, genres, downloads)
     * and re-fetch its details fresh, via the existing [resetNovel]. This matches the legacy
     * library's "hard reset". Because resetNovel deletes and re-inserts the novel row (new id), the
     * tag links keyed on the old id are re-applied to the fresh novel afterwards so the user's tags
     * survive the reset. Network-dependent, so it is gated on connectivity and runs off the main thread.
     */
    private fun confirmHardReset(novel: Novel) {
        activity?.let { act ->
            MaterialDialog(act).show {
                title(text = "Hard reset \"${novel.name}\"?")
                message(text = "This deletes all downloaded chapters and local data for this novel and re-fetches it from the source. Your reading position will be lost. Tags are kept.")
                positiveButton(text = "Hard reset") { dialog ->
                    dialog.dismiss()
                    if (!networkHelper.isConnectedToNetwork()) { showNoInternet(); return@positiveButton }
                    if (syncing) return@positiveButton
                    beginSyncing()
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val tagIds = dbHelper.getTagIdsForNovel(novel.id)
                            dbHelper.resetNovel(novel)
                            // Re-link tags to the freshly re-inserted novel (matched by url).
                            val fresh = dbHelper.getNovelByUrl(novel.url)
                            if (fresh != null && tagIds.isNotEmpty()) dbHelper.setNovelTags(fresh.id, tagIds)
                        } catch (e: Exception) {
                            Logs.error(TAG, "hardReset: ${novel.name}", e)
                        } finally {
                            endSyncing()
                            withContext(Dispatchers.Main) { viewModel.load() }
                        }
                    }
                }
                negativeButton(R.string.cancel)
                lifecycleOwner(this@NewLibraryFragment)
            }
        }
    }

    //region Sync — mirrors LibraryFragment.syncNovels but across the whole library (no section scope).

    private fun startSync() {
        if (!networkHelper.isConnectedToNetwork()) {
            showNoInternet(); return
        }
        if (syncing) return
        beginSyncing()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val throttle = DomainThrottle()
                val novels = dbHelper.getAllNovels()
                val totalChaptersMap = java.util.Collections.synchronizedMap(HashMap<Novel, ArrayList<WebPage>>())

                val fetchJobs = LinkedList<Deferred<Boolean>>()
                novels.forEach { novel ->
                    fetchJobs.add(async {
                        try {
                            val newChaptersList = throttle.withDomainPermit(novel.url) {
                                withContext(Dispatchers.IO) {
                                    val source = sourceManager.get(novel.sourceId)
                                    if (source is NovelUpdatesSource) source.getUnsortedChapterList(novel)
                                    else source?.getChapterList(novel)
                                }
                            } ?: ArrayList()

                            var currentHash = (novel.metadata[Constants.MetaDataKeys.HASH_CODE] ?: "0").toInt()
                            if (currentHash == 0) currentHash = dbHelper.getAllWebPages(novel.id).sumOf { it.hashCode() }
                            val newHash = newChaptersList.sumOf { it.hashCode() }
                            if (newChaptersList.isNotEmpty() && newHash != currentHash) {
                                novel.metadata[Constants.MetaDataKeys.HASH_CODE] = newHash.toString()
                                totalChaptersMap[novel] = ArrayList(newChaptersList)
                            }
                        } catch (e: Exception) {
                            Logs.error(TAG, "sync fetch: ${novel.name}", e)
                        }
                        true
                    })
                }
                fetchJobs.awaitAll()

                val entries = synchronized(totalChaptersMap) { totalChaptersMap.entries.toList() }
                val writeJobs = LinkedList<Deferred<Boolean>>()
                entries.forEach { entry ->
                    val novel = entry.key
                    var chapters = entry.value
                    novel.metadata[Constants.MetaDataKeys.LAST_UPDATED_DATE] = Utils.getCurrentFormattedDate()
                    var newChaptersCount = chapters.size - novel.chaptersCount
                    if (newChaptersCount <= 0) newChaptersCount = 0
                    val newReleasesCount = novel.newReleasesCount + newChaptersCount
                    novel.chaptersCount = chapters.size.toLong()
                    novel.newReleasesCount = newReleasesCount
                    dbHelper.updateNovelMetaData(novel)
                    dbHelper.updateChaptersAndReleasesCount(novel.id, chapters.size.toLong(), newReleasesCount)

                    try {
                        if (novel.sourceId == Constants.SourceId.NOVEL_UPDATES) {
                            chapters = ArrayList(withContext(Dispatchers.IO) { sourceManager.get(novel.sourceId)?.getChapterList(novel) } ?: emptyList())
                        }
                        writeJobs.add(async {
                            dbHelper.writableDatabase.runTransaction { db ->
                                for (i in chapters.indices) {
                                    dbHelper.createWebPage(chapters[i], db)
                                    dbHelper.createWebPageSettings(WebPageSettings(chapters[i].url, novel.id), db)
                                }
                            }
                            true
                        })
                    } catch (e: Exception) {
                        Logs.error(TAG, "sync write: ${novel.name}", e)
                    }
                }
                writeJobs.awaitAll()
            } catch (e: Exception) {
                Logs.error(TAG, "sync", e)
            } finally {
                endSyncing()
                withContext(Dispatchers.Main) { viewModel.load() }
            }
        }
    }

    private fun startRefreshCovers() {
        if (!networkHelper.isConnectedToNetwork()) {
            showNoInternet(); return
        }
        if (syncing) return
        beginSyncing()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                try { Glide.get(requireContext().applicationContext).clearDiskCache() } catch (e: Exception) {
                    Logs.error(TAG, "refreshCovers: clearDiskCache", e)
                }
                val throttle = DomainThrottle()
                val novels = dbHelper.getAllNovels()
                val jobs = LinkedList<Deferred<Boolean>>()
                novels.forEach { novel ->
                    jobs.add(async {
                        try {
                            val source = sourceManager.get(novel.sourceId)
                            if (source != null) {
                                val fetched = throttle.withDomainPermit(novel.url) {
                                    withContext(Dispatchers.IO) { source.getNovelDetails(novel) }
                                }
                                if (fetched != null && fetched.id != -1L) dbHelper.updateNovel(fetched)
                            }
                        } catch (e: Exception) {
                            Logs.error(TAG, "refreshCovers: ${novel.name}", e)
                        }
                        true
                    })
                }
                jobs.awaitAll()
                withContext(Dispatchers.Main) {
                    activity?.let {
                        try { Glide.get(it.applicationContext).clearMemory() } catch (e: Exception) {
                            Logs.error(TAG, "refreshCovers: clearMemory", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Logs.error(TAG, "refreshCovers", e)
            } finally {
                endSyncing()
                withContext(Dispatchers.Main) { viewModel.load() }
            }
        }
    }

    //endregion

    private fun showNoInternet() {
        activity?.let {
            MaterialDialog(it).show {
                message(R.string.no_internet)
                lifecycleOwner(this@NewLibraryFragment)
            }
        }
    }

    companion object {
        private const val TAG = "NewLibraryFragment"
    }
}

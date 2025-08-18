package io.github.gmathi.novellibrary.viewmodel

import android.content.Context
import androidx.lifecycle.*
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.model.database.Download
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.*
import io.github.gmathi.novellibrary.util.Exceptions.MISSING_SOURCE_ID
import io.github.gmathi.novellibrary.viewmodel.ChaptersViewModel.Action.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.lang.ref.WeakReference
import javax.inject.Inject
import com.google.firebase.analytics.logEvent

@HiltViewModel
class ChaptersViewModel @Inject constructor(
    private val state: SavedStateHandle,
    private val dbHelper: DBHelper,
    private val dataCenter: DataCenter,
    private val networkHelper: NetworkHelper,
    private val sourceManager: SourceManager,
    private val firebaseAnalytics: FirebaseAnalytics
) : ViewModel(), LifecycleObserver {

    companion object {
        const val TAG = "ChaptersViewModel"
        const val KEY_NOVEL = "novelKey"
    }

    //Objects to be part of the savedState
    val novel: Novel
        get() = state.get(KEY_NOVEL) as? Novel ?: throw Error("Invalid Novel")

    private fun setNovel(novel: Novel) {
        state.set(KEY_NOVEL, novel)
    }

    private lateinit var _ctx: WeakReference<Context>
    fun getContext(): Context? = _ctx.get()

    var chapters: ArrayList<WebPage>? = null
    var chapterSettings: ArrayList<WebPageSettings>? = null

    //Other variables
    private val _loadingStatus = MutableStateFlow("")
    val loadingStatus: StateFlow<String> = _loadingStatus.asStateFlow()
    
    private val _actionModeProgress = MutableStateFlow("")
    val actionModeProgress: StateFlow<String> = _actionModeProgress.asStateFlow()
    
    var showSources: Boolean = false

    fun init(novel: Novel, lifecycleOwner: LifecycleOwner, context: Context) {
        setNovel(novel)
        this._ctx = WeakReference(context)
        lifecycleOwner.lifecycle.addObserver(this)
        this.showSources = novel.metadata[Constants.MetaDataKeys.SHOW_SOURCES]?.toBoolean() ?: false
    }

    fun getData(forceUpdate: Boolean = false) {
        viewModelScope.launch {
            _loadingStatus.value = Constants.Status.START
            withContext(Dispatchers.IO) {
                if (novel.id != -1L) {
                    dbHelper.getNovel(novel.id)?.let { setNovel(it) }
                    dbHelper.updateNewReleasesCount(novel.id, 0L)
                }
            }
            getChapters(forceUpdate = forceUpdate)

            if (chapters == null && !networkHelper.isConnectedToNetwork()) {
                _loadingStatus.value = Constants.Status.NO_INTERNET
                return@launch
            }

            if (chapters == null) {
                _loadingStatus.value = Constants.Status.NETWORK_ERROR
                return@launch
            }

            if (chapters.isNullOrEmpty()) { //we already did null check above
                _loadingStatus.value = Constants.Status.EMPTY_DATA
                return@launch
            }

            if (forceUpdate || chapterSettings.isNullOrEmpty()) {
                //Add Chapters to database, only if novel was added to Library
                if (novel.id != -1L) {
                    addToDB(forceUpdate)
                }
            }
            _loadingStatus.value = Constants.Status.DONE
        }
    }

    fun toggleSources() {
        showSources = !showSources
        viewModelScope.launch {
            novel.metadata[Constants.MetaDataKeys.SHOW_SOURCES] = showSources.toString()
            withContext(Dispatchers.IO) { dbHelper.updateNovelMetaData(novel) }
        }
        _loadingStatus.value = Constants.Status.DONE
    }

    private suspend fun getChapters(forceUpdate: Boolean) = withContext(Dispatchers.IO) {
        //Reset Chapters & ChapterSettings to Null, if this is a forceUpdate
        if (forceUpdate) {
            this@ChaptersViewModel.chapters = null
            this@ChaptersViewModel.chapterSettings = null
        }

        try {
            if (!forceUpdate && novel.id != -1L) {
                _loadingStatus.value = "Checking Cache…"
                val chapters = dbHelper.getAllWebPages(novel.id)
                val chapterSettings = dbHelper.getAllWebPageSettings(novel.id)

                if (chapters.isEmpty() || chapters.size < novel.chaptersCount.toInt()) {
                    novel.metadata[Constants.MetaDataKeys.LAST_UPDATED_DATE] = Utils.getCurrentFormattedDate()
                    dbHelper.updateNovelMetaData(novel)
                } else {
                    this@ChaptersViewModel.chapters = chapters
                    this@ChaptersViewModel.chapterSettings = chapterSettings
                    return@withContext
                }
            }

            if (showSources)
                _loadingStatus.value = "Downloading Chapters by Source…"
            else
                _loadingStatus.value = "Downloading Chapters…"

            val source = sourceManager.get(novel.sourceId) ?: throw Exception(MISSING_SOURCE_ID)
            val fetchedChapters = source.getChapterList(novel)
            if (novel.id != -1L)
                fetchedChapters.forEach { it.novelId = novel.id }
            this@ChaptersViewModel.chapters = ArrayList(fetchedChapters)
            return@withContext

        } catch (e: Exception) {
            Logs.error(TAG, "getChapters - isRemote:$forceUpdate", e)
        }
    }

    private suspend fun addToDB(forceUpdate: Boolean) = withContext(Dispatchers.IO) {
        _loadingStatus.value = "Adding/Updating Cache…"

        //DB transaction for faster insertions
        dbHelper.writableDatabase.runTransaction { writableDatabase ->

            if (forceUpdate) {
                //Delete the current data
                dbHelper.deleteWebPages(novel.id, writableDatabase)
                chapters?.forEach { dbHelper.deleteWebPage(it.url, writableDatabase) }

                // We will not delete chapter settings so as to not delete the downloaded chapters file location.
                // dbHelper.deleteWebPageSettings(novel.id)
            }

            val chaptersList = chapters ?: return@runTransaction
            val chaptersCount = chaptersList.size
            dbHelper.updateChaptersCount(novel.id, chaptersCount.toLong(), writableDatabase)

            val chaptersHash = chaptersList.sumOf { it.hashCode() }
            novel.metadata[Constants.MetaDataKeys.HASH_CODE] = chaptersHash.toString()
            dbHelper.updateNovelMetaData(novel)

            for (i in 0 until chaptersCount) {
                _loadingStatus.value = "Caching Chapters: $i/$chaptersCount"
                dbHelper.createWebPage(chaptersList[i], writableDatabase)
                dbHelper.createWebPageSettings(WebPageSettings(chaptersList[i].url, novel.id), writableDatabase)
            }
        }

        chapters = dbHelper.getAllWebPages(novel.id)
        chapterSettings = dbHelper.getAllWebPageSettings(novel.id)
    }

    fun addNovelToLibrary() {
        if (novel.id != -1L) return
        _loadingStatus.value = Constants.Status.START
        addNewNovel(novel)
        if (novel.id == -1L) return
        chapters?.forEach { it.novelId = novel.id }
        addNovelChaptersToDB()
    }

    private fun addNewNovel(novel: Novel) {
        if (novel.id == -1L) {
            novel.id = dbHelper.insertNovel(novel)
            logNovelEvent(FAC.Event.ADD_NOVEL, novel)
        }
    }

    private fun logNovelEvent(eventName: String, novel: Novel) {
        firebaseAnalytics.logEvent(eventName) {
            param(FAC.Param.NOVEL_NAME, novel.name)
            param(FAC.Param.NOVEL_URL, novel.url)
        }
    }

    private fun addNovelChaptersToDB() {
        viewModelScope.launch {
            addToDB(true)
            _loadingStatus.value = Constants.Status.DONE
        }
    }

    fun updateChapters(webPages: ArrayList<WebPage>, action: Action, callback: (() -> Unit)? = null) {
        actionModeScope {
            when (action) {
                ADD_DOWNLOADS -> {
                    addChaptersToDownloadQueue(webPages)
                    callback?.let { it() }
                }
                DELETE_DOWNLOADS -> deleteDownloadedChapters(webPages)
                MARK_READ -> updateReadStatus(webPages, markRead = true)
                MARK_UNREAD -> updateReadStatus(webPages, markRead = false)
                MARK_FAVORITE -> updateFavoriteStatus(webPages, true)
                REMOVE_FAVORITE -> updateFavoriteStatus(webPages, false)
            }
        }
    }


    private suspend fun deleteDownloadedChapters(webPages: ArrayList<WebPage>) = withContext(Dispatchers.IO) {
        var counter = 0
        webPages.forEach {
            withContext(Dispatchers.IO) {
                deleteDownloadedChapter(it)
                _actionModeProgress.value = counter++.toString()
            }
        }
    }

    private fun deleteDownloadedChapter(webPage: WebPage) {
        val chaptersSettingsList = chapterSettings ?: return
        val webPageSettings = chaptersSettingsList.firstOrNull { it.url == webPage.url }
        webPageSettings?.filePath?.let { filePath ->
            val file = File(filePath)
            file.delete()
            webPageSettings.filePath = null
            try {
                if (webPageSettings.metadata.contains(Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES)) {
                    val linkedPages = webPageSettings.getLinkedPagesCompat()
                    linkedPages.forEach {
                        val linkedWebPageSettings = dbHelper.getWebPageSettings(it.href)
                        if (linkedWebPageSettings?.filePath != null) {
                            val linkedFile = File(linkedWebPageSettings.filePath!!)
                            linkedFile.delete()
                            dbHelper.deleteWebPageSettings(linkedWebPageSettings.url)
                        }
                    }
                    webPageSettings.metadata[Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES] = "[]"
                }
            } catch (e: Exception) {
                Logs.error(TAG, "Delete WebPage: $webPage", e)
            }
            dbHelper.updateWebPageSettings(webPageSettings)
        }
    }

    private suspend fun addChaptersToDownloadQueue(webPages: List<WebPage>) = withContext(Dispatchers.IO) {
        var counter = 0
        dbHelper.writableDatabase.runTransaction { writableDatabase ->
            webPages.forEach {
                val download = Download(it.url, novel.name, novel.id, it.chapterName)
                download.orderId = it.orderId.toInt()
                dbHelper.createDownload(download, writableDatabase)
                _actionModeProgress.value = counter++.toString()
            }
        }
    }

    private suspend fun updateReadStatus(webPages: ArrayList<WebPage>, markRead: Boolean) = withContext(Dispatchers.IO) {
        var counter = 0
        val chapters = ArrayList(webPages)
        val chaptersSettingsList = chapterSettings ?: return@withContext
        dbHelper.writableDatabase.runTransaction { writableDatabase ->
            chapters.forEach { webPage ->
                val webPageSettings = chaptersSettingsList.firstOrNull { it.url == webPage.url } ?: return@runTransaction
                dbHelper.updateWebPageSettingsReadStatus(webPageSettings, markRead, writableDatabase)
                _actionModeProgress.value = counter++.toString()
            }
        }
        chapterSettings = dbHelper.getAllWebPageSettings(novel.id)
    }

    private suspend fun updateFavoriteStatus(webPages: ArrayList<WebPage>, favoriteStatus: Boolean) = withContext(Dispatchers.IO) {
        var counter = 0
        val chapters = ArrayList(webPages)
        val chaptersSettingsList = chapterSettings ?: return@withContext
        dbHelper.writableDatabase.runTransaction { writableDatabase ->
            chapters.forEach { webPage ->
                val webPageSettings = chaptersSettingsList.firstOrNull { it.url == webPage.url } ?: return@runTransaction
                webPageSettings.metadata[Constants.MetaDataKeys.IS_FAVORITE] = favoriteStatus.toString()
                dbHelper.updateWebPageSettings(webPageSettings, writableDatabase)
                _actionModeProgress.value = counter++.toString()
            }
        }
    }

    private fun actionModeScope(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch {
            _actionModeProgress.value = Constants.Status.START
            block()
            _actionModeProgress.value = Constants.Status.DONE
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        getData()
    }

    enum class Action {
        ADD_DOWNLOADS, DELETE_DOWNLOADS, MARK_READ, MARK_UNREAD, MARK_FAVORITE, REMOVE_FAVORITE
    }


}

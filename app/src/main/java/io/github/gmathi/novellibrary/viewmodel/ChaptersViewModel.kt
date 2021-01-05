package io.github.gmathi.novellibrary.viewmodel

import android.content.Context
import androidx.lifecycle.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.db
import io.github.gmathi.novellibrary.model.database.Download
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.network.getChapterUrls
import io.github.gmathi.novellibrary.network.sync.NovelSync
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.viewmodel.ChaptersViewModel.Action.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ChaptersViewModel(private val state: SavedStateHandle) : ViewModel(), LifecycleObserver {

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

    lateinit var context: Context

    var chapters: ArrayList<WebPage>? = null
    var chapterSettings: ArrayList<WebPageSettings>? = null

    //Other variables
    var loadingStatus = MutableLiveData<String>()
    var actionModeProgress = MutableLiveData<String>()
    var showSources: Boolean = false

    fun init(novel: Novel, lifecycleOwner: LifecycleOwner, context: Context) {
        setNovel(novel)
        lifecycleOwner.lifecycle.addObserver(this)
        this.context = context
        this.showSources = novel.metadata[Constants.MetaDataKeys.SHOW_SOURCES]?.toBoolean() ?: false
    }

    fun getData(forceUpdate: Boolean = false) {
        viewModelScope.launch {
            loadingStatus.value = Constants.Status.START
            withContext(Dispatchers.IO) {
                if (novel.id != -1L) {
                    db.novelDao().findOneById(novel.id)?.let { setNovel(it) }
                    novel.newReleasesCount = 0L
                    db.novelDao().update(novel)
                }
            }
            getChapters(forceUpdate = forceUpdate)

            if (chapters == null && !Utils.isConnectedToNetwork(context)) {
                loadingStatus.postValue(Constants.Status.NO_INTERNET)
                return@launch
            }

            if (chapters == null) {
                loadingStatus.value = Constants.Status.NETWORK_ERROR
                return@launch
            }

            if (chapters.isNullOrEmpty()) { //we already did null check above
                loadingStatus.value = Constants.Status.EMPTY_DATA
                return@launch
            }

            if (forceUpdate || chapterSettings.isNullOrEmpty()) {
                //Add Chapters to database, only if novel was added to Library
                if (novel.id != -1L) {
                    addToDB(forceUpdate)
                }
            }
            loadingStatus.value = Constants.Status.DONE
        }
    }

    fun toggleSources() {
        showSources = !showSources
        viewModelScope.launch {
            novel.metadata[Constants.MetaDataKeys.SHOW_SOURCES] = showSources.toString()
            withContext(Dispatchers.IO) { db.novelDao().update(novel) }
        }
        getData(true)
    }

    private suspend fun getChapters(forceUpdate: Boolean) = withContext(Dispatchers.IO) {
        //Reset Chapters & ChapterSettings to Null, if this is a forceUpdate
        if (forceUpdate) {
            this@ChaptersViewModel.chapters = null
            this@ChaptersViewModel.chapterSettings = null
        }

        try {
            if (!forceUpdate && novel.id != -1L) {
                loadingStatus.postValue("Checking Cache…")
                val chapters = db.webPageDao().findByNovelId(novel.id)
                val chapterSettings = db.webPageSettingsDao().findByNovelId(novel.id)

                if (chapters.isEmpty() || chapters.size < novel.chaptersCount.toInt()) {
                    novel.metadata[Constants.MetaDataKeys.LAST_UPDATED_DATE] = Utils.getCurrentFormattedDate()
                    db.novelDao().update(novel)
                } else {
                    this@ChaptersViewModel.chapters = ArrayList(chapters)
                    this@ChaptersViewModel.chapterSettings = ArrayList(chapterSettings)
                    return@withContext
                }
            }

            if (showSources)
                loadingStatus.postValue("Downloading Chapters by Source…")
            else
                loadingStatus.postValue("Downloading Chapters…")

            this@ChaptersViewModel.chapters = NovelApi.getChapterUrls(novel, showSources)
            return@withContext

        } catch (e: Exception) {
            Logs.error(TAG, "getChapters - isRemote:$forceUpdate", e)
        }
    }

    private suspend fun addToDB(forceUpdate: Boolean) = withContext(Dispatchers.IO) {
        loadingStatus.postValue("Adding/Updating Cache…")

        if (forceUpdate) {
            //Delete the current data
            db.webPageDao().deleteByNovelId(novel.id)
            chapters?.forEach { db.webPageDao().deleteByUrl(it.url) }

            // We will not delete chapter settings so as to not delete the downloaded chapters file location.
            // dbHelper.deleteWebPageSettings(novel.id)
        }

        val chaptersList = chapters ?: return@withContext
        val chaptersCount = chaptersList.size
        novel.chaptersCount = chaptersCount.toLong()
        db.novelDao().update(novel)

        for (i in 0 until chaptersCount) {
            loadingStatus.postValue("Caching Chapters: $i/$chaptersCount")
            db.webPageDao().insertOrReplace(chaptersList[i])
            db.webPageSettingsDao().insertOrReplace(WebPageSettings(chaptersList[i].url, novel.id))
        }
        chapters = ArrayList<WebPage>(db.webPageDao().findByNovelId(novel.id))
        chapterSettings = ArrayList<WebPageSettings>(db.webPageSettingsDao().findByNovelId(novel.id))
    }

    fun addNovelToLibrary() {
        if (novel.id != -1L) return
        loadingStatus.value = Constants.Status.START
        novel.id = db.insertNovel(novel)
        NovelSync.getInstance(novel)?.applyAsync(viewModelScope) { if (dataCenter.getSyncAddNovels(it.host)) it.addNovel(novel) }
        //There is a chance that the above insertion might fail
        if (novel.id == -1L) return
        chapters?.forEach { it.novelId = novel.id }
        addNovelChaptersToDB()
    }

    private fun addNovelChaptersToDB() {
        viewModelScope.launch {
            addToDB(true)
            loadingStatus.value = Constants.Status.DONE
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
                MARK_READ -> updateReadStatus(webPages, 1)
                MARK_UNREAD -> updateReadStatus(webPages, 0)
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
                actionModeProgress.postValue(counter++.toString())
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
                val otherLinkedPagesJsonString = webPageSettings.metadata[Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES]
                if (otherLinkedPagesJsonString != null) {
                    val linkedPages: ArrayList<WebPage> = Gson().fromJson(otherLinkedPagesJsonString, object : TypeToken<java.util.ArrayList<WebPage>>() {}.type)
                    linkedPages.forEach {
                        val linkedWebPageSettings = db.webPageSettingsDao().findOneByUrl(it.url)
                        if (linkedWebPageSettings?.filePath != null) {
                            val linkedFile = File(linkedWebPageSettings.filePath!!)
                            linkedFile.delete()
                            db.webPageSettingsDao().delete(linkedWebPageSettings)
                        }
                    }
                    webPageSettings.metadata[Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES] = "[]"
                }
            } catch (e: Exception) {
                Logs.error(TAG, "Delete WebPage: $webPage", e)
            }
            db.webPageSettingsDao().update(webPageSettings)
        }
    }

    private suspend fun addChaptersToDownloadQueue(webPages: List<WebPage>) = withContext(Dispatchers.IO) {
        var counter = 0
        webPages.forEach {
            withContext(Dispatchers.IO) {
                val download = Download(it.url, novel.name, it.chapter)
                download.orderId = it.orderId.toInt()
                db.downloadDao().insert(download)
                actionModeProgress.postValue(counter++.toString())
            }
        }
    }

    private suspend fun updateReadStatus(webPages: ArrayList<WebPage>, readStatus: Int) = withContext(Dispatchers.IO) {
        var counter = 0
        val chapters = ArrayList(webPages)
        chapters.forEach { webPage ->
            withContext(Dispatchers.IO) sub@{
                val chaptersSettingsList = chapterSettings ?: return@sub
                val webPageSettings = chaptersSettingsList.firstOrNull { it.url == webPage.url } ?: return@sub
                webPageSettings.isRead = readStatus
                db.updateWebPageSettingsReadStatus(webPageSettings)
                actionModeProgress.postValue(counter++.toString())
            }
        }
        chapterSettings = ArrayList(db.webPageSettingsDao().findByNovelId(novel.id))
    }

    private suspend fun updateFavoriteStatus(webPages: ArrayList<WebPage>, favoriteStatus: Boolean) = withContext(Dispatchers.IO) {
        var counter = 0
        val chapters = ArrayList(webPages)
        chapters.forEach { webPage ->
            withContext(Dispatchers.IO) sub@{
                val chaptersSettingsList = chapterSettings ?: return@sub
                val webPageSettings = chaptersSettingsList.firstOrNull { it.url == webPage.url } ?: return@sub
                webPageSettings.metadata[Constants.MetaDataKeys.IS_FAVORITE] = favoriteStatus.toString()
                db.webPageSettingsDao().update(webPageSettings)
                actionModeProgress.postValue(counter++.toString())
            }
        }
    }

    private fun actionModeScope(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch {
            actionModeProgress.value = Constants.Status.START
            block()
            actionModeProgress.value = Constants.Status.DONE
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

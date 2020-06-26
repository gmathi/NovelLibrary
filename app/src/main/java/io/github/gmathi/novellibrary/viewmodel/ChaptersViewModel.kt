package io.github.gmathi.novellibrary.viewmodel

import android.content.Context
import androidx.lifecycle.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.model.Download
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.WebPage
import io.github.gmathi.novellibrary.model.WebPageSettings
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.network.getChapterUrls
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

    private var isFetchingData = false

    fun init(novel: Novel, lifecycleOwner: LifecycleOwner, context: Context) {
        setNovel(novel)
        lifecycleOwner.lifecycle.addObserver(this)
        this.context = context
        this.showSources = novel.metaData[Constants.MetaDataKeys.SHOW_SOURCES]?.toBoolean() ?: false
    }


    fun getData(forceUpdate: Boolean = false) {
        if (isFetchingData) return
        isFetchingData = true
        viewModelScope.launch {
            loadingStatus.value = Constants.Status.START
            withContext(Dispatchers.IO) { dbHelper.updateNewReleasesCount(novel.id, 0L) }
            getChapters(forceUpdate = forceUpdate)

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
            isFetchingData = false
        }
    }

    fun toggleSources() {
        showSources = !showSources
        viewModelScope.launch {
            novel.metaData[Constants.MetaDataKeys.SHOW_SOURCES] = showSources.toString()
            withContext(Dispatchers.IO) { dbHelper.updateNovelMetaData(novel) }
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
                val chapters = dbHelper.getAllWebPages(novel.id)
                val chapterSettings = dbHelper.getAllWebPageSettings(novel.id)

                if (chapters.isEmpty() || chapters.size < novel.chaptersCount.toInt()) {
                    novel.metaData[Constants.MetaDataKeys.LAST_UPDATED_DATE] = Utils.getCurrentFormattedDate()
                    dbHelper.updateNovelMetaData(novel)
                } else {
                    this@ChaptersViewModel.chapters = chapters
                    this@ChaptersViewModel.chapterSettings = chapterSettings
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
            dbHelper.deleteWebPages(novel.id)

            // We will not delete chapter settings so as to not delete the downloaded chapters file location.
            // dbHelper.deleteWebPageSettings(novel.id)
        }

        val chaptersList = chapters!!
        val chaptersCount = chaptersList.size
        dbHelper.updateChaptersCount(novel.id, chaptersCount.toLong())

        for (i in 0 until chaptersCount) {
            loadingStatus.postValue("Caching Chapters: $i/$chaptersCount")
            dbHelper.createWebPage(chaptersList[i])
            dbHelper.createWebPageSettings(WebPageSettings(chaptersList[i].url, novel.id))
        }

        chapterSettings = dbHelper.getAllWebPageSettings(novel.id)
    }

    fun addNovelToLibrary() {
        viewModelScope.launch {
            if (novel.id != -1L) return@launch
            loadingStatus.value = Constants.Status.START
            novel.id = withContext(Dispatchers.IO) { dbHelper.insertNovel(novel) }
            chapters?.forEach { it.novelId = novel.id }
        }
    }

    fun addNovelChaptersToDB() {
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
            }
        }
    }


    private suspend fun deleteDownloadedChapters(webPages: ArrayList<WebPage>) = withContext(Dispatchers.IO) {
        var counter = 0
        webPages.forEach {
            withContext(Dispatchers.IO) {
                deleteWebPage(it)
                actionModeProgress.postValue(counter++.toString())
            }
        }
    }

    private fun deleteWebPage(webPage: WebPage) {
        val chaptersSettingsList = chapterSettings ?: return
        val webPageSettings = chaptersSettingsList.firstOrNull { it.url == webPage.url }
        webPageSettings?.filePath?.let { filePath ->
            val file = File(filePath)
            file.delete()
            webPageSettings.filePath = null
            try {
                val otherLinkedPagesJsonString = webPageSettings.metaData[Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES]
                if (otherLinkedPagesJsonString != null) {
                    val linkedPages: ArrayList<WebPage> = Gson().fromJson(otherLinkedPagesJsonString, object : TypeToken<java.util.ArrayList<WebPage>>() {}.type)
                    linkedPages.forEach {
                        val linkedWebPageSettings = dbHelper.getWebPageSettings(it.url)
                        if (linkedWebPageSettings?.filePath != null) {
                            val linkedFile = File(linkedWebPageSettings.filePath!!)
                            linkedFile.delete()
                            dbHelper.deleteWebPageSettings(linkedWebPageSettings.url)
                        }
                    }
                    webPageSettings.metaData[Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES] = "[]"
                }
            } catch (e: Exception) {
                Logs.error(TAG, "Delete WebPage: $webPage", e)
            }
            dbHelper.updateWebPageSettings(webPageSettings)
        }
    }

    private suspend fun addChaptersToDownloadQueue(webPages: List<WebPage>) = withContext(Dispatchers.IO) {
        var counter = 0
        webPages.forEach {
            withContext(Dispatchers.IO) {
                val download = Download(it.url, novel.name, it.chapter)
                download.orderId = it.orderId.toInt()
                dbHelper.createDownload(download)
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
                dbHelper.updateWebPageSettingsReadStatus(webPageSettings.url, readStatus, HashMap(webPageSettings.metaData))
                actionModeProgress.postValue(counter++.toString())
            }
        }
    }

    private fun actionModeScope(codeBlock: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch {
            actionModeProgress.value = Constants.Status.START
            codeBlock()
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
        ADD_DOWNLOADS, DELETE_DOWNLOADS, MARK_READ, MARK_UNREAD
    }

}

//package io.github.gmathi.novellibrary.viewmodel
//
//import android.content.Context
//import android.view.View
//import androidx.lifecycle.*
//import com.google.gson.Gson
//import com.google.gson.reflect.TypeToken
//import io.github.gmathi.novellibrary.R
//import io.github.gmathi.novellibrary.dataCenter
//import io.github.gmathi.novellibrary.database.*
//import io.github.gmathi.novellibrary.dbHelper
//import io.github.gmathi.novellibrary.extensions.showError
//import io.github.gmathi.novellibrary.extensions.showLoading
//import io.github.gmathi.novellibrary.model.*
//import io.github.gmathi.novellibrary.network.NovelApi
//import io.github.gmathi.novellibrary.network.getChapterUrls
//import io.github.gmathi.novellibrary.network.sync.NovelSync
//import io.github.gmathi.novellibrary.util.Constants
//import io.github.gmathi.novellibrary.util.Logs
//import io.github.gmathi.novellibrary.util.Utils
//import io.github.gmathi.novellibrary.viewmodel.ChaptersViewModel.Action.*
//import kotlinx.android.synthetic.main.content_import_library.*
//import kotlinx.coroutines.*
//import org.jsoup.Jsoup
//import org.jsoup.nodes.Document
//import java.io.File
//
//
//class ImportLibraryViewModel(private val state: SavedStateHandle) : ViewModel(), LifecycleObserver {
//
//    companion object {
//        const val TAG = "ChaptersViewModel"
//        const val KEY_URL = "novelKey"
//    }
//
//    lateinit var context: Context
//
//    var importList = MutableLiveData<ArrayList<ImportListItem>>()
//    var updateSet = MutableLiveData<HashSet<ImportListItem>>()
//
//    //Objects to be part of the savedState
////    val novel: Novel
////        get() = state.get(KEY_NOVEL) as? Novel ?: throw Error("Invalid Novel")
////
////    private fun setNovel(novel: Novel) {
////        state.set(KEY_NOVEL, novel)
////    }
//
//
//
//    fun init(lifecycleOwner: LifecycleOwner, context: Context) {
//        lifecycleOwner.lifecycle.addObserver(this)
//        this.context = context
//    }
//
//    fun getNovelsToImport() {
//        viewModelScope.launch {
//                try {
//                    progressLayout.showLoading()
//                    val url = getUrl() ?: return@runBlocking
//                    val userId = getUserIdFromUrl(url)
//                    val adminUrl = "https://www.novelupdates.com/wp-admin/admin-ajax.php"
//                    val formData: HashMap<String, String> = hashMapOf(
//                        "action" to "nu_prevew",
//                        "pagenum" to "0",
//                        "intUserID" to userId,
//                        "isMobile" to "yes"
//                    )
//                    var body = withContext(Dispatchers.IO) { NovelApi.getStringWithFormData(adminUrl, formData) }
//                    body = body.replace("\\\"", "\"")
//                        .replace("\\n", "")
//                        .replace("\\t", "")
//                        .replace("\\/", "/")
//
//                    val doc: Document = Jsoup.parse(body)
//                    val novels = doc.body().select("a.mb-box-btn")
//                    if (novels != null && novels.isNotEmpty()) {
//                        importList.clear()
//                        novels.mapTo(importList) {
//                            val importItem = ImportListItem()
//                            importItem.novelName = it.getElementsByClass("title")?.firstOrNull()?.text()
//                            importItem.novelUrl = it.attr("href")
//                            val styleAttr = it.getElementsByClass("icon-thumb")?.firstOrNull()?.attr("style")
//                            if (styleAttr != null && styleAttr.length > 26)
//                                importItem.novelImageUrl = styleAttr.substring(22, styleAttr.length - 3)
//                            importItem.currentlyReadingChapterName = it.getElementsByClass("cr_status")?.firstOrNull()?.text()
//                            importItem.currentlyReading = it.getElementsByClass("cr_status")?.firstOrNull()?.parent()?.text()
//                            importItem.isAlreadyInLibrary = dbHelper.getNovelByUrl(importItem.novelUrl!!) != null
//                            importItem
//                        }
//                        progressLayout.showContent()
//                        headerLayout.visibility = View.VISIBLE
//                    } else {
//                        progressLayout.showError(errorText = "No Novels found!", buttonText = getString(R.string.try_again), onClickListener = View.OnClickListener {
//                            getNovelsFromUrl()
//                        })
//                    }
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//    }
//
//    fun getData(forceUpdate: Boolean = false) {
//        viewModelScope.launch {
//            loadingStatus.value = Constants.Status.START
//            withContext(Dispatchers.IO) {
//                if (novel.id != -1L) {
//                    dbHelper.getNovel(novel.id)?.let { setNovel(it) }
//                    dbHelper.updateNewReleasesCount(novel.id, 0L)
//                }
//            }
//            getChapters(forceUpdate = forceUpdate)
//
//            if (chapters == null && !Utils.isConnectedToNetwork(context)) {
//                loadingStatus.postValue(Constants.Status.NO_INTERNET)
//                return@launch
//            }
//
//            if (chapters == null) {
//                loadingStatus.value = Constants.Status.NETWORK_ERROR
//                return@launch
//            }
//
//            if (chapters.isNullOrEmpty()) { //we already did null check above
//                loadingStatus.value = Constants.Status.EMPTY_DATA
//                return@launch
//            }
//
//            if (forceUpdate || chapterSettings.isNullOrEmpty()) {
//                //Add Chapters to database, only if novel was added to Library
//                if (novel.id != -1L) {
//                    addToDB(forceUpdate)
//                }
//            }
//            loadingStatus.value = Constants.Status.DONE
//        }
//    }
//
//    fun toggleSources() {
//        showSources = !showSources
//        viewModelScope.launch {
//            novel.metadata[Constants.MetaDataKeys.SHOW_SOURCES] = showSources.toString()
//            withContext(Dispatchers.IO) { dbHelper.updateNovelMetaData(novel) }
//        }
//        getData(true)
//    }
//
//    private suspend fun getChapters(forceUpdate: Boolean) = withContext(Dispatchers.IO) {
//        //Reset Chapters & ChapterSettings to Null, if this is a forceUpdate
//        if (forceUpdate) {
//            this@ImportLibraryViewModel.chapters = null
//            this@ImportLibraryViewModel.chapterSettings = null
//        }
//
//        try {
//            if (!forceUpdate && novel.id != -1L) {
//                loadingStatus.postValue("Checking Cache…")
//                val chapters = dbHelper.getAllWebPages(novel.id)
//                val chapterSettings = dbHelper.getAllWebPageSettings(novel.id)
//
//                if (chapters.isEmpty() || chapters.size < novel.chaptersCount.toInt()) {
//                    novel.metadata[Constants.MetaDataKeys.LAST_UPDATED_DATE] = Utils.getCurrentFormattedDate()
//                    dbHelper.updateNovelMetaData(novel)
//                } else {
//                    this@ImportLibraryViewModel.chapters = chapters
//                    this@ImportLibraryViewModel.chapterSettings = chapterSettings
//                    return@withContext
//                }
//            }
//
//            if (showSources)
//                loadingStatus.postValue("Downloading Chapters by Source…")
//            else
//                loadingStatus.postValue("Downloading Chapters…")
//
//            this@ImportLibraryViewModel.chapters = NovelApi.getChapterUrls(novel, showSources)
//            return@withContext
//
//        } catch (e: Exception) {
//            Logs.error(TAG, "getChapters - isRemote:$forceUpdate", e)
//        }
//    }
//
//    private suspend fun addToDB(forceUpdate: Boolean) = withContext(Dispatchers.IO) {
//        loadingStatus.postValue("Adding/Updating Cache…")
//
//        if (forceUpdate) {
//            //Delete the current data
//            dbHelper.deleteWebPages(novel.id)
//            chapters?.forEach { dbHelper.deleteWebPage(it.url) }
//
//            // We will not delete chapter settings so as to not delete the downloaded chapters file location.
//            // dbHelper.deleteWebPageSettings(novel.id)
//        }
//
//        val chaptersList = chapters ?: return@withContext
//        val chaptersCount = chaptersList.size
//        dbHelper.updateChaptersCount(novel.id, chaptersCount.toLong())
//
//        for (i in 0 until chaptersCount) {
//            loadingStatus.postValue("Caching Chapters: $i/$chaptersCount")
//            dbHelper.createWebPage(chaptersList[i])
//            dbHelper.createWebPageSettings(WebPageSettings(chaptersList[i].url, novel.id))
//        }
//        chapters = dbHelper.getAllWebPages(novel.id)
//        chapterSettings = dbHelper.getAllWebPageSettings(novel.id)
//    }
//
//    fun addNovelToLibrary() {
//        if (novel.id != -1L) return
//        loadingStatus.value = Constants.Status.START
//        novel.id = dbHelper.insertNovel(novel)
//        NovelSync.getInstance(novel)?.applyAsync { if (dataCenter.getSyncAddNovels(it.host)) it.addNovel(novel) }
//        //There is a chance that the above insertion might fail
//        if (novel.id == -1L) return
//        chapters?.forEach { it.novelId = novel.id }
//        addNovelChaptersToDB()
//    }
//
//    private fun addNovelChaptersToDB() {
//        viewModelScope.launch {
//            addToDB(true)
//            loadingStatus.value = Constants.Status.DONE
//        }
//    }
//
//    fun updateChapters(webPages: ArrayList<WebPage>, action: Action, callback: (() -> Unit)? = null) {
//        actionModeScope {
//            when (action) {
//                ADD_DOWNLOADS -> {
//                    addChaptersToDownloadQueue(webPages)
//                    callback?.let { it() }
//                }
//                DELETE_DOWNLOADS -> deleteDownloadedChapters(webPages)
//                MARK_READ -> updateReadStatus(webPages, 1)
//                MARK_UNREAD -> updateReadStatus(webPages, 0)
//                MARK_FAVORITE -> updateFavoriteStatus(webPages, true)
//                REMOVE_FAVORITE -> updateFavoriteStatus(webPages, false)
//            }
//        }
//    }
//
//
//    private suspend fun deleteDownloadedChapters(webPages: ArrayList<WebPage>) = withContext(Dispatchers.IO) {
//        var counter = 0
//        webPages.forEach {
//            withContext(Dispatchers.IO) {
//                deleteDownloadedChapter(it)
//                actionModeProgress.postValue(counter++.toString())
//            }
//        }
//    }
//
//    private fun deleteDownloadedChapter(webPage: WebPage) {
//        val chaptersSettingsList = chapterSettings ?: return
//        val webPageSettings = chaptersSettingsList.firstOrNull { it.url == webPage.url }
//        webPageSettings?.filePath?.let { filePath ->
//            val file = File(filePath)
//            file.delete()
//            webPageSettings.filePath = null
//            try {
//                val otherLinkedPagesJsonString = webPageSettings.metadata[Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES]
//                if (otherLinkedPagesJsonString != null) {
//                    val linkedPages: ArrayList<WebPage> = Gson().fromJson(otherLinkedPagesJsonString, object : TypeToken<java.util.ArrayList<WebPage>>() {}.type)
//                    linkedPages.forEach {
//                        val linkedWebPageSettings = dbHelper.getWebPageSettings(it.url)
//                        if (linkedWebPageSettings?.filePath != null) {
//                            val linkedFile = File(linkedWebPageSettings.filePath!!)
//                            linkedFile.delete()
//                            dbHelper.deleteWebPageSettings(linkedWebPageSettings.url)
//                        }
//                    }
//                    webPageSettings.metadata[Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES] = "[]"
//                }
//            } catch (e: Exception) {
//                Logs.error(TAG, "Delete WebPage: $webPage", e)
//            }
//            dbHelper.updateWebPageSettings(webPageSettings)
//        }
//    }
//
//    private suspend fun addChaptersToDownloadQueue(webPages: List<WebPage>) = withContext(Dispatchers.IO) {
//        var counter = 0
//        webPages.forEach {
//            withContext(Dispatchers.IO) {
//                val download = Download(it.url, novel.name, it.chapter)
//                download.orderId = it.orderId.toInt()
//                dbHelper.createDownload(download)
//                actionModeProgress.postValue(counter++.toString())
//            }
//        }
//    }
//
//    private suspend fun updateReadStatus(webPages: ArrayList<WebPage>, readStatus: Int) = withContext(Dispatchers.IO) {
//        var counter = 0
//        val chapters = ArrayList(webPages)
//        chapters.forEach { webPage ->
//            withContext(Dispatchers.IO) sub@{
//                val chaptersSettingsList = chapterSettings ?: return@sub
//                val webPageSettings = chaptersSettingsList.firstOrNull { it.url == webPage.url } ?: return@sub
//                dbHelper.updateWebPageSettingsReadStatus(webPageSettings.url, readStatus, HashMap(webPageSettings.metadata))
//                actionModeProgress.postValue(counter++.toString())
//            }
//        }
//        chapterSettings = dbHelper.getAllWebPageSettings(novel.id)
//    }
//
//    private suspend fun updateFavoriteStatus(webPages: ArrayList<WebPage>, favoriteStatus: Boolean) = withContext(Dispatchers.IO) {
//        var counter = 0
//        val chapters = ArrayList(webPages)
//        chapters.forEach { webPage ->
//            withContext(Dispatchers.IO) sub@{
//                val chaptersSettingsList = chapterSettings ?: return@sub
//                val webPageSettings = chaptersSettingsList.firstOrNull { it.url == webPage.url } ?: return@sub
//                webPageSettings.metadata[Constants.MetaDataKeys.IS_FAVORITE] = favoriteStatus.toString()
//                dbHelper.updateWebPageSettings(webPageSettings)
//                actionModeProgress.postValue(counter++.toString())
//            }
//        }
//    }
//
//    private fun actionModeScope(codeBlock: suspend CoroutineScope.() -> Unit) {
//        viewModelScope.launch {
//            actionModeProgress.value = Constants.Status.START
//            codeBlock()
//            actionModeProgress.value = Constants.Status.DONE
//        }
//    }
//
//    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
//    fun onPause() {
//    }
//
//    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
//    fun onResume() {
//        getData()
//    }
//
//    enum class Action {
//        ADD_DOWNLOADS, DELETE_DOWNLOADS, MARK_READ, MARK_UNREAD, MARK_FAVORITE, REMOVE_FAVORITE
//    }
//
//}

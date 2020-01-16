package io.github.gmathi.novellibrary.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import co.metalab.asyncawait.async
import com.afollestad.materialdialogs.MaterialDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.ChaptersPageListener
import io.github.gmathi.novellibrary.adapter.GenericFragmentStatePagerAdapter
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.extensions.shareUrl
import io.github.gmathi.novellibrary.model.*
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.network.getChapterUrls
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.Utils
import kotlinx.android.synthetic.main.activity_chapters_pager.*
import kotlinx.android.synthetic.main.content_chapters_pager.*
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.*
import kotlin.collections.ArrayList


class ChaptersPagerActivity : BaseActivity(), ActionMode.Callback {

    companion object {
        private const val TAG = "ChaptersPagerActivity"
    }

    lateinit var novel: Novel

    var chapters: ArrayList<WebPage> = ArrayList()
    var chaptersSettings: ArrayList<WebPageSettings> = ArrayList()
    var dataSet: HashSet<WebPage> = HashSet()

    private val sources: ArrayList<Pair<Long, String>> = ArrayList()
    private var actionMode: ActionMode? = null
    private var confirmDialog: MaterialDialog? = null
    private var showSources = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chapters_pager)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        novel = intent.getParcelableExtra("novel")!!
        showSources = novel.metaData[Constants.MetaDataKeys.SHOW_SOURCES]?.toBoolean() ?: false
        dbHelper.updateNewReleasesCount(novel.id, 0L)

        sourcesToggle.setOnClickListener {
            if (Utils.isConnectedToNetwork(this)) {
                showSources = !showSources
                novel.metaData[Constants.MetaDataKeys.SHOW_SOURCES] = showSources.toString()
                dbHelper.updateNovelMetaData(novel)
                getChapters(forceUpdate = true)
            } else {
                confirmDialog("You need to have internet connection to do this!", MaterialDialog.SingleButtonCallback { dialog, _ ->
                    dialog.dismiss()
                })
            }
        }

        if (novel.id == -1L)
            getChapters()
    }

    override fun onResume() {
        super.onResume()
        if (novel.id != -1L) {
            novel = dbHelper.getNovel(novel.id)!!
            progressLayout.showLoading()
            getChaptersFromDB()
        }
    }

    private fun setViewPager() {
        async {
            while (supportFragmentManager.backStackEntryCount > 0)
                supportFragmentManager.popBackStack()

            sources.clear()

            if (showSources) {
                val sourceIds = chapters.distinctBy { it.sourceId }.map { it.sourceId }
                sourceIds.forEach { dbHelper.getSource(it)?.let { sources.add(it) } }
            } else {
                dbHelper.getSource(-1L)?.let { sources.add(it) }
            }

            val titles = Array(sources.size, init = {
                sources[it].second
            })

            val navPageAdapter = GenericFragmentStatePagerAdapter(supportFragmentManager, titles, titles.size, ChaptersPageListener(novel, sources))
            viewPager.offscreenPageLimit = 3
            viewPager.adapter = navPageAdapter
            tabStrip.setViewPager(viewPager)
            scrollToBookmark()
        }
    }

    private fun scrollToBookmark() {
        novel.currentWebPageUrl?.let { currentWebPageUrl ->
            val currentBookmarkWebPage = dbHelper.getWebPage(currentWebPageUrl) ?: return
            val currentSource = sources.firstOrNull { it.first == currentBookmarkWebPage.sourceId }
                    ?: return
            val index = sources.indexOf(currentSource)
            if (index != -1)
                viewPager.currentItem = index
        }
    }

    //region Data
    private fun getChaptersFromDB() {
        async {
            chapters = await { ArrayList(dbHelper.getAllWebPages(novel.id)) }
            chaptersSettings = await { ArrayList(dbHelper.getAllWebPageSettings(novel.id)) }
            if (chapters.isEmpty() || chapters.size < novel.chaptersCount.toInt()) {
                novel.metaData[Constants.MetaDataKeys.LAST_UPDATED_DATE] = Utils.getCurrentFormattedDate()
                dbHelper.updateNovelMetaData(novel)
                getChapters()
            } else {
                progressLayout.showContent()
                setViewPager()
            }
        }
    }

    private fun getChapters(forceUpdate: Boolean = false) {
        async chapters@{
            progressLayout.showLoading()
            if (!Utils.isConnectedToNetwork(this@ChaptersPagerActivity)) {
                if (chapters.isEmpty())
                    progressLayout.showError(ContextCompat.getDrawable(this@ChaptersPagerActivity, R.drawable.ic_warning_white_vector), getString(R.string.no_internet), getString(R.string.try_again)) {
                        progressLayout.showLoading()
                        getChapters()
                    }
                return@chapters
            }

            //Download latest chapters from network
            try {
                chapters = await { NovelApi.getChapterUrls(novel, showSources) } ?: ArrayList()

                //Save to DB if the novel is in Library
                if (novel.id != -1L) {
                    await { addChaptersToDB(forceUpdate) }
                }
                actionMode?.finish()
                progressLayout.showContent()
                setViewPager()

            } catch (e: Exception) {
                e.printStackTrace()
                if (progressLayout.isLoading)
                    progressLayout.showError(ContextCompat.getDrawable(this@ChaptersPagerActivity, R.drawable.ic_warning_white_vector), getString(R.string.failed_to_load_url), getString(R.string.try_again)) {
                        progressLayout.showLoading()
                        getChapters()
                    }
            }
        }
    }

    private fun addChaptersToDB(forceUpdate: Boolean = false) {
        if (forceUpdate) {
            dbHelper.deleteWebPages(novel.id)
        }
        dbHelper.updateChaptersAndReleasesCount(novel.id, chapters.size.toLong(), 0L)
        for (i in 0 until chapters.size) {
            if (forceUpdate)
                dbHelper.createWebPage(chapters[i])
            else {
                if (dbHelper.getWebPage(chapters[i].url) == null)
                    dbHelper.createWebPage(chapters[i])
            }
            if (dbHelper.getWebPageSettings(chapters[i].url) == null)
                dbHelper.createWebPageSettings(WebPageSettings(chapters[i].url, novel.id))
        }
        chaptersSettings = ArrayList(dbHelper.getAllWebPageSettings(novel.id))
    }

    //endregion

    //region Options Menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_activity_chapters_pager, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.getItem(1)?.isVisible = (novel.id == -1L)
        menu?.getItem(2)?.isVisible = (novel.id != -1L)
        return super.onPrepareOptionsMenu(menu)
    }

    private var devCounter: Int = 0
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when {
            item?.itemId == android.R.id.home -> finish()
            item?.itemId == R.id.action_sync -> {
                getChapters(forceUpdate = true)
                devCounter++
                if (devCounter == 40) dataCenter.isDeveloper = true
                return true
            }
            item?.itemId == R.id.action_download -> {
                confirmDialog(getString(R.string.download_all_chapters_dialog_content), MaterialDialog.SingleButtonCallback { dialog, _ ->
                    addWebPagesToDownload()
                    dialog.dismiss()
                })
                return true
            }
            item?.itemId == R.id.action_add_to_library -> {
                addNovelToLibrary()
                invalidateOptionsMenu()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    //endregion

    //region ActionMode Callback


    internal fun addToDataSet(webPage: WebPage) {
        dataSet.add(webPage)
        if (dataSet.isNotEmpty() && actionMode == null) {
            actionMode = startSupportActionMode(this)
        }
        actionMode?.title = getString(R.string.chapters_selected, dataSet.size)
    }

    internal fun removeFromDataSet(webPage: WebPage) {
        dataSet.remove(webPage)
        if (dataSet.isEmpty()) {
            actionMode?.finish()
        } else {
            actionMode?.title = getString(R.string.chapters_selected, dataSet.size)
        }
    }

    private fun addToDataSet(webPages: List<WebPage>) {
        dataSet.addAll(webPages)
        if (dataSet.isNotEmpty() && actionMode == null) {
            actionMode = startSupportActionMode(this)
        }
        actionMode?.title = getString(R.string.chapters_selected, dataSet.size)
    }

    private fun removeFromDataSet(webPages: List<WebPage>) {
        dataSet.removeAll(webPages)
        if (dataSet.isEmpty()) {
            actionMode?.finish()
        } else {
            actionMode?.title = getString(R.string.chapters_selected, dataSet.size)
        }
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_unread -> {
                confirmDialog(getString(R.string.mark_chapters_unread_dialog_content), MaterialDialog.SingleButtonCallback { dialog, _ ->
                    dataSet.forEach { webPage ->
                        val webPageSettings = chaptersSettings.firstOrNull { it.url == webPage.url }
                        webPageSettings?.let {
                            it.metaData.remove(Constants.MetaDataKeys.SCROLL_POSITION)
                            it.isRead = 0
                            dbHelper.updateWebPageSettingsReadStatus(it)
                        }
                    }
                    dialog.dismiss()
                    mode?.finish()
                })
            }
            R.id.action_read -> {
                confirmDialog(getString(R.string.mark_chapters_read_dialog_content), MaterialDialog.SingleButtonCallback { dialog, _ ->
                    dataSet.forEach { webPage ->
                        val webPageSettings = chaptersSettings.firstOrNull { it.url == webPage.url }
                        webPageSettings?.let {
                            it.isRead = 1
                            dbHelper.updateWebPageSettingsReadStatus(it)
                        }
                    }
                    dialog.dismiss()
                    mode?.finish()
                })
            }
            R.id.action_download -> {
                confirmDialog(getString(R.string.download_chapters_dialog_content), MaterialDialog.SingleButtonCallback { dialog, _ ->
                    if (novel.id == -1L) {
                        showNotInLibraryDialog()
                    } else {
                        val listToDownload = ArrayList(dataSet)
                        if (listToDownload.isNotEmpty()) {
                            addWebPagesToDownload(listToDownload)
                        }
                        dialog.dismiss()
                        mode?.finish()
                    }
                })
            }
            R.id.action_select_interval -> {
                progressLayout.showLoading()
                val sourceId = sources[viewPager.currentItem].first
                if (sourceId == -1L) {
                    val chaptersForSource = chapters.sortedBy { it.orderId }
                    val selectedChaptersForSource = dataSet.sortedBy { it.orderId }
                    if (selectedChaptersForSource.size > 1) {
                        val firstIndex = chaptersForSource.indexOf(selectedChaptersForSource.first())
                        val lastIndex = chaptersForSource.indexOf(selectedChaptersForSource.last())
                        val subList = chaptersForSource.subList(firstIndex, lastIndex)
                        addToDataSet(subList)
                        EventBus.getDefault().post(ChapterActionModeEvent(sourceId, EventType.UPDATE))
                    }
                } else {
                    val chaptersForSource = chapters.filter { it.sourceId == sourceId }.sortedBy { it.orderId }
                    val selectedChaptersForSource = dataSet.filter { it.sourceId == sourceId }.sortedBy { it.orderId }
                    if (selectedChaptersForSource.size > 1) {
                        val firstIndex = chaptersForSource.indexOf(selectedChaptersForSource.first())
                        val lastIndex = chaptersForSource.indexOf(selectedChaptersForSource.last())
                        val subList = chaptersForSource.subList(firstIndex, lastIndex)
                        addToDataSet(subList)
                        EventBus.getDefault().post(ChapterActionModeEvent(sourceId, EventType.UPDATE))
                    }
                }
                progressLayout.showContent()
            }
            R.id.action_select_all -> {
                progressLayout.showLoading()
                val sourceId = sources[viewPager.currentItem].first
                if (sourceId == -1L) {
                    addToDataSet(webPages = chapters)
                } else {
                    addToDataSet(webPages = chapters.filter { it.sourceId == sourceId })
                }
                EventBus.getDefault().post(ChapterActionModeEvent(sourceId, EventType.UPDATE))
                progressLayout.showContent()
            }
            R.id.action_clear_selection -> {
                progressLayout.showLoading()
                val sourceId = sources[viewPager.currentItem].first
                if (sourceId == -1L) {
                    removeFromDataSet(webPages = chapters)
                } else {
                    removeFromDataSet(webPages = chapters.filter { it.sourceId == sourceId })
                }
                removeFromDataSet(webPages = chapters.filter { it.sourceId == sourceId })
                EventBus.getDefault().post(ChapterActionModeEvent(sourceId, EventType.UPDATE))
                progressLayout.showContent()
            }
            R.id.action_share_chapter -> {
                val urls = dataSet.joinToString(separator = ", ") { it.url }
                shareUrl(urls)
            }
            R.id.action_delete -> {
                dataSet.forEach {
                    deleteWebPage(it)
                }
                mode?.finish()
            }
        }
        return false
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode?.menuInflater?.inflate(R.menu.menu_chapters_action_mode, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        menu?.findItem(R.id.action_download)?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        dataSet.clear()
        actionMode = null
        EventBus.getDefault().post(ChapterActionModeEvent(eventType = EventType.COMPLETE))
    }

    //endregion

    //region Dialogs

    private fun confirmDialog(content: String, callback: MaterialDialog.SingleButtonCallback) {
        if (confirmDialog == null || !confirmDialog!!.isShowing) {
            confirmDialog = MaterialDialog.Builder(this)
                    .title(getString(R.string.confirm_action))
                    .content(content)
                    .positiveText(getString(R.string.okay))
                    .negativeText(R.string.cancel)
                    .onPositive(callback)
                    .onNegative { dialog, _ -> dialog.dismiss() }.build()
            confirmDialog!!.show()
        }
    }

    internal fun confirmDialog(content: String, positiveCallback: MaterialDialog.SingleButtonCallback, negativeCallback: MaterialDialog.SingleButtonCallback) {
        if (confirmDialog == null || !confirmDialog!!.isShowing) {
            confirmDialog = MaterialDialog.Builder(this)
                    .title(getString(R.string.confirm_action))
                    .content(content)
                    .positiveText(getString(R.string.okay))
                    .negativeText(R.string.cancel)
                    .onPositive(positiveCallback)
                    .onNegative(negativeCallback).build()
            confirmDialog!!.show()
        }
    }


    private fun showNotInLibraryDialog() {
        MaterialDialog.Builder(this)
                .iconRes(R.drawable.ic_warning_white_vector)
                .title(getString(R.string.alert))
                .content(getString(R.string.novel_not_in_library_dialog_content))
                .positiveText(getString(R.string.okay))
                .onPositive { dialog, _ -> dialog.dismiss() }
                .show()
    }

    private fun manageDownloadsDialog() {
        MaterialDialog.Builder(this)
                .iconRes(R.drawable.ic_info_white_vector)
                .title(getString(R.string.manage_downloads))
                .content("Novel downloads can be managed by navigating to \"Downloads\" through the navigation drawer menu. Please start the download manually!!")
                .positiveText(getString(R.string.take_me_there))
                .negativeText(getString(R.string.okay))
                .onPositive { dialog, _ -> dialog.dismiss(); setResult(Constants.OPEN_DOWNLOADS_RES_CODE); finish() }
                .onNegative { dialog, _ -> dialog.dismiss() }
                .show()
    }
    //endregion

    private fun addWebPagesToDownload(webPages: List<WebPage> = chapters) {
        async {
            val dialog = MaterialDialog.Builder(this@ChaptersPagerActivity)
                    .content("Adding chapters to download queue…")
                    .progress(false, webPages.size, true)
                    .cancelable(false)
                    .show()

            webPages.forEach {
                val download = Download(it.url, novel.name, it.chapter)
                download.orderId = it.orderId.toInt()
                await { dbHelper.createDownload(download) }
                dialog.incrementProgress(1)
            }
            dialog.setContent("Finished adding chapters…")
            dialog.dismiss()
            manageDownloadsDialog()
        }
    }

    private fun deleteWebPage(webPage: WebPage) {
        val webPageSettings = chaptersSettings.firstOrNull { it.url == webPage.url }
        webPageSettings?.filePath?.let { filePath ->
            try {
                val file = File(filePath)
                file.delete()
                webPageSettings.filePath = null
                if (webPageSettings.metaData.containsKey(Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES)) {
                    val linkedPages: ArrayList<String> = Gson().fromJson(webPageSettings.metaData[Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES_SETTINGS], object : TypeToken<java.util.ArrayList<WebPage>>() {}.type)
                    linkedPages.forEach {
                        val linkedWebPageSettings = chaptersSettings.firstOrNull { it.url == webPage.url }
                        if (linkedWebPageSettings?.filePath != null) {
                            val linkedFile = File(linkedWebPageSettings.filePath)
                            linkedFile.delete()
                            dbHelper.deleteWebPageSettings(linkedWebPageSettings.url)
                        }
                    }
                    webPageSettings.metaData[Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES] = "[]"
                }
                dbHelper.updateWebPageSettings(webPageSettings)
            } catch (e: Exception) {
                Logs.error(TAG, e.localizedMessage)
            }
        }
    }

    internal fun addNovelToLibrary() {
        async {
            if (novel.id != -1L) return@async
            progressLayout.showLoading()
            novel.id = await { dbHelper.insertNovel(novel) }
            invalidateOptionsMenu()
            chapters.forEach { it.novelId = novel.id }
            await { addChaptersToDB(true) }
            progressLayout.showContent()
        }
    }


}

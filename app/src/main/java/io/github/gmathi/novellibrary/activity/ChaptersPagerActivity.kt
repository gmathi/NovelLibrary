package io.github.gmathi.novellibrary.activity

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import co.metalab.asyncawait.async
import com.afollestad.materialdialogs.MaterialDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.ChaptersPageListener
import io.github.gmathi.novellibrary.adapter.GenericFragmentStatePagerAdapter
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.model.*
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.network.getChapterUrls
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Utils
import kotlinx.android.synthetic.main.activity_chapters_pager.*
import kotlinx.android.synthetic.main.content_chapters_pager.*
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.*


class ChaptersPagerActivity : BaseActivity(), ActionMode.Callback {

    lateinit var novel: Novel

    var chapters: ArrayList<WebPage> = ArrayList()
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

        novel = intent.getSerializableExtra("novel") as Novel
        showSources = novel.metaData[Constants.MetaDataKeys.SHOW_SOURCES]?.toBoolean() ?: false
        dbHelper.updateNewReleasesCount(novel.id, 0L)

        sourcesToggle.setOnClickListener {
            showSources = !showSources
            novel.metaData[Constants.MetaDataKeys.SHOW_SOURCES] = showSources.toString()
            dbHelper.updateNovelMetaData(novel)
            setViewPager()
        }

        progressLayout.showLoading()
        if (novel.id != -1L)
            getChaptersFromDB()
        else
            getChapters()
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
        if (novel.currentWebPageId != -1L) {
            val currentBookmarkWebPage = dbHelper.getWebPage(novel.currentWebPageId) ?: return
            val currentSource = sources.firstOrNull { it.first == currentBookmarkWebPage.sourceId }
                    ?: return
            val index = sources.indexOf(currentSource)
            if (index != -1)
                viewPager.currentItem = index
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

    }

    //region Data
    private fun getChaptersFromDB() {
        async {
            Utils.error("CPA", "chpaDBStart, " + Calendar.getInstance().timeInMillis.toString())
            chapters = await { ArrayList(dbHelper.getAllWebPages(novel.id)) }
            if (chapters.isEmpty() || chapters.size < novel.chaptersCount.toInt()) {
                novel.metaData[Constants.MetaDataKeys.LAST_UPDATED_DATE] = Utils.getCurrentFormattedDate()
                dbHelper.updateNovelMetaData(novel)
                getChapters()
            } else {
                progressLayout.showContent()
                setViewPager()
            }
            Utils.error("CPA", "chpaDBEnd, " + Calendar.getInstance().timeInMillis.toString())
        }
    }

    private fun getChapters() {
        Utils.error("CPA", "chpaNetwork, " + Calendar.getInstance().timeInMillis.toString())
        async chapters@{
            progressLayout.showLoading()
            if (!Utils.isConnectedToNetwork(this@ChaptersPagerActivity)) {
                if (chapters.isEmpty())
                    progressLayout.showError(ContextCompat.getDrawable(this@ChaptersPagerActivity, R.drawable.ic_warning_white_vector), getString(R.string.no_internet), getString(R.string.try_again), {
                        progressLayout.showLoading()
                        getChapters()
                    })
                return@chapters
            }

            //Download latest chapters from network
            try {
                Utils.error("CPA", "callStart, " + Calendar.getInstance().timeInMillis.toString())
                chapters = await { NovelApi.getChapterUrls(novel) } ?: ArrayList()
                Utils.error("CPA", "callEnd, " + Calendar.getInstance().timeInMillis.toString())

                //Save to DB if the novel is in Library
                if (novel.id != -1L) {
                    Utils.error("CPA", "dbStart, " + Calendar.getInstance().timeInMillis.toString())
                    await { addChaptersToDB() }
                    Utils.error("CPA", "dbEnd, " + Calendar.getInstance().timeInMillis.toString())
                }
                actionMode?.finish()
                progressLayout.showContent()
                setViewPager()

            } catch (e: Exception) {
                e.printStackTrace()
                if (progressLayout.isLoading)
                    progressLayout.showError(ContextCompat.getDrawable(this@ChaptersPagerActivity, R.drawable.ic_warning_white_vector), getString(R.string.failed_to_load_url), getString(R.string.try_again), {
                        progressLayout.showLoading()
                        getChapters()
                    })
            }
        }
    }

    private fun addChaptersToDB() {
        dbHelper.updateChaptersAndReleasesCount(novel.id, chapters.size.toLong(), 0L)
        for (i in 0 until chapters.size) {
            val webPage = dbHelper.getWebPage(novel.id, chapters[i].url)
            if (webPage == null) {
                chapters[i].id = dbHelper.createWebPage(chapters[i])
            } else {
                chapters[i].copyFrom(webPage)
                if (webPage.sourceId == -1L && chapters[i].sourceId != -1L) {
                    dbHelper.updateWebPage(chapters[i])
                }
            }
        }

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

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when {
            item?.itemId == android.R.id.home -> finish()
            item?.itemId == R.id.action_sync -> {
                getChapters()
                return true
            }
            item?.itemId == R.id.action_download -> {
                confirmDialog(if (novel.id != -1L) getString(R.string.download_all_new_chapters_dialog_content) else getString(R.string.download_all_chapters_dialog_content), MaterialDialog.SingleButtonCallback { dialog, _ ->
                    addWebPagesToDownload()
                    dialog.dismiss()
                    manageDownloadsDialog()
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
        if (webPage.id != -1L)
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
        val filteredSet = webPages.filter { it.id != -1L }
        dataSet.addAll(filteredSet)
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
                    dataSet.forEach {
                        it.isRead = 0
                        dbHelper.updateWebPageReadStatus(it)
                    }
                    dialog.dismiss()
                    mode?.finish()
                })
            }
            R.id.action_read -> {
                confirmDialog(getString(R.string.mark_chapters_read_dialog_content), MaterialDialog.SingleButtonCallback { dialog, _ ->
                    dataSet.forEach {
                        it.isRead = 1
                        dbHelper.updateWebPageReadStatus(it)
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
                        manageDownloadsDialog()
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
                addToDataSet(webPages = chapters.filter { it.sourceId == sourceId })
                EventBus.getDefault().post(ChapterActionModeEvent(sourceId, EventType.UPDATE))
                progressLayout.showContent()
            }
            R.id.action_clear_selection -> {
                progressLayout.showLoading()
                val sourceId = sources[viewPager.currentItem].first
                removeFromDataSet(webPages = chapters.filter { it.sourceId == sourceId })
                EventBus.getDefault().post(ChapterActionModeEvent(sourceId, EventType.UPDATE))
                progressLayout.showContent()
            }
            R.id.action_share_chapter -> {
                val urls = dataSet.joinToString(separator = ", ") {
                    if (it.redirectedUrl != null)
                        it.redirectedUrl!!
                    else
                        it.url
                }
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
                .content("Novel downloads can be managed by navigating to \"Downloads\" through the navigation drawer menu.")
                .positiveText(getString(R.string.take_me_there))
                .negativeText(getString(R.string.okay))
                .onPositive { dialog, _ -> dialog.dismiss(); setResult(Constants.OPEN_DOWNLOADS_RES_CODE); finish() }
                .onNegative { dialog, _ -> dialog.dismiss() }
                .show()
    }
    //endregion

    private fun addWebPagesToDownload(webPages: List<WebPage> = chapters) {
        async {
            progressLayout.showLoading()
            webPages.forEach {
                val download = Download(it.id, novel.name, it.chapter)
                download.orderId = it.orderId.toInt()
                await { dbHelper.createDownload(download) }
            }
            progressLayout.showContent()
            startDownloadNovelService(novel.name)
        }
    }

    private fun deleteWebPage(webPage: WebPage) {
        if (webPage.filePath != null)
            try {
                val file = File(webPage.filePath)
                file.delete()
                webPage.filePath = null
                if (webPage.metaData.containsKey(Constants.MD_OTHER_LINKED_WEB_PAGES)) {
                    val linkedPages: ArrayList<WebPage> = Gson().fromJson(webPage.metaData[Constants.MD_OTHER_LINKED_WEB_PAGES], object : TypeToken<java.util.ArrayList<WebPage>>() {}.type)
                    linkedPages.forEach {
                        if (it.filePath != null) {
                            val linkedFile = File(it.filePath)
                            linkedFile.delete()
                        }
                        it.filePath = null
                    }
                    webPage.metaData[Constants.MD_OTHER_LINKED_WEB_PAGES] = Gson().toJson(linkedPages)
                }
                dbHelper.updateWebPage(webPage)
            } catch (e: Exception) {
                Utils.error("OldChaptersActivity", e.localizedMessage)
            }
    }

    internal fun addNovelToLibrary() {
        async {
            if (novel.id != -1L) return@async
            progressLayout.showLoading()
            novel.id = await { dbHelper.insertNovel(novel) }
            await { addChaptersToDB() }
            progressLayout.showContent()
        }
    }

}

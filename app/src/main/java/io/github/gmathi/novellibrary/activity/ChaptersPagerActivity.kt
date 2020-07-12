package io.github.gmathi.novellibrary.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.observe
import com.afollestad.materialdialogs.MaterialDialog
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.ChaptersPageListener
import io.github.gmathi.novellibrary.adapter.GenericFragmentStatePagerAdapter
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.getSource
import io.github.gmathi.novellibrary.database.getWebPage
import io.github.gmathi.novellibrary.database.updateNovel
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.extensions.*
import io.github.gmathi.novellibrary.model.ChapterActionModeEvent
import io.github.gmathi.novellibrary.model.EventType
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.WebPage
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.ProgressLayout
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.viewmodel.ChaptersViewModel
import kotlinx.android.synthetic.main.activity_chapters_pager.*
import kotlinx.android.synthetic.main.content_chapters_pager.*
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.collections.ArrayList


class ChaptersPagerActivity : BaseActivity(), ActionMode.Callback {

    companion object {
        //private const val TAG = "ChaptersPagerActivity"
    }

    val vm: ChaptersViewModel by viewModels()

    var dataSet: HashSet<WebPage> = HashSet()
    private val sources: ArrayList<Pair<Long, String>> = ArrayList()
    private var actionMode: ActionMode? = null

    private var confirmDialog: MaterialDialog? = null
    private var progressDialog: MaterialDialog? = null

    private var maxProgress: Int = 0
    private var progressMessage = "In Progress…"
    private var isSyncing = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chapters_pager)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val novel: Novel?
        @Suppress("LiftReturnOrAssignment")
        if (savedInstanceState != null) {
            val isProgressShowing = savedInstanceState.getBoolean("isProgressShowing", false)
            if (isProgressShowing) {
                setProgressDialog(savedInstanceState.getString("progressMessage", "In Progress…"), savedInstanceState.getInt("maxProgress", 0))
                progressDialog?.show()
            }
            novel = savedInstanceState.getSerializable("novel") as? Novel
        } else {
            novel = intent.getSerializableExtra("novel") as? Novel
        }

        if (novel == null) return
        vm.init(novel, this, this)

        addListeners()
        addObservers()
    }

    private fun addListeners() {
        sourcesToggle.setOnClickListener {
            if (Utils.isConnectedToNetwork(this)) {
                vm.toggleSources()
            } else {
                confirmDialog("You need to have internet connection to do this!", MaterialDialog.SingleButtonCallback { dialog, _ ->
                    dialog.dismiss()
                })
            }
        }
    }

    private fun addObservers() {
        vm.loadingStatus.observe(this) { newStatus ->
            //Update loading status
            when (newStatus) {
                Constants.Status.START -> {
                    progressLayout.showLoading()
                    //progressLayout.showLoading(rawId = R.raw.baby_peeking, loadingText = getString(R.string.loading))
                }
                Constants.Status.EMPTY_DATA -> {
                    progressLayout.showEmpty(resId = R.raw.monkey_logo, isLottieAnimation = true, emptyText = getString(R.string.empty_chapters))
                }
                Constants.Status.NETWORK_ERROR -> {
                    progressLayout.showError(errorText = getString(R.string.failed_to_load_url), buttonText = getString(R.string.try_again), onClickListener = View.OnClickListener {
                        vm.getData()
                    })
                }
                Constants.Status.NO_INTERNET -> {
                    progressLayout.noInternetError(View.OnClickListener {
                        vm.getData()
                    })
                }
                Constants.Status.DONE -> {
                    isSyncing = false
                    progressLayout.showContent()
                    setViewPager()
                }
                else -> {
                    progressLayout.updateLoadingStatus(newStatus)
                }
            }
        }

        vm.actionModeProgress.observe(this) { progress ->
            //Update Action mode actions status
            when (progress) {
                Constants.Status.START -> {
                    progressDialog?.show()
                }
                Constants.Status.DONE -> {
                    progressDialog?.dismiss()
                    EventBus.getDefault().post(ChapterActionModeEvent(eventType = EventType.COMPLETE))
                }
                else -> {
                    progressDialog?.setProgress(progress.toInt())
                    //progressDialog?.setProgressNumberFormat(progress)
                }
            }
        }

    }


    private fun setViewPager() {
        while (supportFragmentManager.backStackEntryCount > 0)
            supportFragmentManager.popBackStack()

        sources.clear()
        val chapters = vm.chapters ?: throw Error("Chapters cannot be null")

        if (vm.showSources) {
            val sourceIds = chapters.distinctBy { it.sourceId }.map { it.sourceId }
            sourceIds.forEach { sourceId -> dbHelper.getSource(sourceId)?.let { sources.add(it) } }
        } else {
            dbHelper.getSource(-1L)?.let { sources.add(it) }
        }

        val titles = Array(sources.size, init = {
            sources[it].second
        })

        val navPageAdapter = GenericFragmentStatePagerAdapter(supportFragmentManager, titles, titles.size, ChaptersPageListener(vm.novel, sources))
        viewPager.offscreenPageLimit = 3
        viewPager.adapter = navPageAdapter
        tabStrip.setViewPager(viewPager)
        scrollToBookmark()
    }


    private fun scrollToBookmark() {
        vm.novel.currentWebPageUrl?.let { currentWebPageUrl ->
            val currentBookmarkWebPage = dbHelper.getWebPage(currentWebPageUrl) ?: return
            val currentSource = sources.firstOrNull { it.first == currentBookmarkWebPage.sourceId }
                ?: return
            val index = sources.indexOf(currentSource)
            if (index != -1)
                viewPager.currentItem = index
        }
    }

    //region Options Menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_activity_chapters_pager, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.getItem(1)?.isVisible = (vm.novel.id == -1L)
        menu?.getItem(2)?.isVisible = (vm.novel.id != -1L)
        return super.onPrepareOptionsMenu(menu)
    }

    private var devCounter: Int = 0
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_sync -> {
                if (!isSyncing) {
                    isSyncing = true
                    vm.getData(forceUpdate = true)
                }
                devCounter++
                if (devCounter == 40) dataCenter.isDeveloper = true
                return true
            }
            R.id.action_download -> {
                confirmDialog(getString(R.string.download_all_chapters_dialog_content), MaterialDialog.SingleButtonCallback { dialog, _ ->
                    val publisher = vm.novel.metaData["English Publisher"]
                    dialog.dismiss()
                    setProgressDialog("Adding chapters to download queue…", vm.chapters?.size ?: 0)
                    vm.updateChapters(
                        vm.chapters!!,
                        ChaptersViewModel.Action.ADD_DOWNLOADS,
                        callback = {
                            manageDownloadsDialog()
                        })
                })
                return true
            }
            R.id.action_add_to_library -> {
                vm.addNovelToLibrary()
                invalidateOptionsMenu()
                vm.addNovelChaptersToDB()
                return true
            }
            R.id.action_sort -> {
                if (vm.novel.metaData["chapterOrder"] == "des")
                    vm.novel.metaData["chapterOrder"] = "asc"
                else
                    vm.novel.metaData["chapterOrder"] = "des"
                EventBus.getDefault().post(ChapterActionModeEvent(eventType = EventType.COMPLETE))
                if (vm.novel.id != -1L)
                    dbHelper.updateNovel(vm.novel)
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
                    setProgressDialog("Marking chapters as unread…", dataSet.size)
                    vm.updateChapters(ArrayList(dataSet), ChaptersViewModel.Action.MARK_UNREAD)
                    dialog.dismiss()
                    mode?.finish()
                })
            }
            R.id.action_read -> {
                confirmDialog(getString(R.string.mark_chapters_read_dialog_content), MaterialDialog.SingleButtonCallback { dialog, _ ->
                    setProgressDialog("Marking chapters as read…", dataSet.size)
                    vm.updateChapters(ArrayList(dataSet), ChaptersViewModel.Action.MARK_READ)
                    dialog.dismiss()
                    mode?.finish()
                })
            }
            R.id.action_unfav -> {
                confirmDialog(getString(R.string.unfavorite_chapters_read_dialog_content), MaterialDialog.SingleButtonCallback { dialog, _ ->
                    setProgressDialog("Removing chapters from favorites…", dataSet.size)
                    vm.updateChapters(ArrayList(dataSet), ChaptersViewModel.Action.REMOVE_FAVORITE)
                    dialog.dismiss()
                    mode?.finish()
                })
            }
            R.id.action_fav -> {
                confirmDialog(getString(R.string.favorite_chapters_read_dialog_content), MaterialDialog.SingleButtonCallback { dialog, _ ->
                    setProgressDialog("Marking chapters as favorites…", dataSet.size)
                    vm.updateChapters(ArrayList(dataSet), ChaptersViewModel.Action.MARK_FAVORITE)
                    dialog.dismiss()
                    mode?.finish()
                })
            }


            R.id.action_download -> {
                confirmDialog(getString(R.string.download_chapters_dialog_content), MaterialDialog.SingleButtonCallback { dialog, _ ->
                    if (vm.novel.id == -1L) {
                        dialog.dismiss()
                        showNotInLibraryDialog()
                        mode?.finish()
                    } else {
                        val publisher = vm.novel.metaData["English Publisher"]
                        val listToDownload = ArrayList(dataSet)
                        if (listToDownload.isNotEmpty()) {
                            dialog.dismiss()
                            setProgressDialog("Add to Downloads…", listToDownload.size)
                            vm.updateChapters(
                                listToDownload,
                                ChaptersViewModel.Action.ADD_DOWNLOADS
                            ) {
                                manageDownloadsDialog()
                            }
                            mode?.finish()
                        }
                    }
                })
            }
            R.id.action_select_interval -> {
                val sourceId = sources[viewPager.currentItem].first
                if (sourceId == -1L) {
                    val chaptersForSource = vm.chapters!!.sortedBy { it.orderId }
                    val selectedChaptersForSource = dataSet.sortedBy { it.orderId }
                    if (selectedChaptersForSource.size > 1) {
                        val firstIndex = chaptersForSource.indexOf(selectedChaptersForSource.first())
                        val lastIndex = chaptersForSource.indexOf(selectedChaptersForSource.last())
                        val subList = chaptersForSource.subList(firstIndex, lastIndex)
                        addToDataSet(subList)
                        EventBus.getDefault().post(ChapterActionModeEvent(sourceId, EventType.UPDATE))
                    }
                } else {
                    val chaptersForSource = vm.chapters!!.filter { it.sourceId == sourceId }.sortedBy { it.orderId }
                    val selectedChaptersForSource = dataSet.filter { it.sourceId == sourceId }.sortedBy { it.orderId }
                    if (selectedChaptersForSource.size > 1) {
                        val firstIndex = chaptersForSource.indexOf(selectedChaptersForSource.first())
                        val lastIndex = chaptersForSource.indexOf(selectedChaptersForSource.last())
                        val subList = chaptersForSource.subList(firstIndex, lastIndex)
                        addToDataSet(subList)
                        EventBus.getDefault().post(ChapterActionModeEvent(sourceId, EventType.UPDATE))
                    }
                }
            }
            R.id.action_select_all -> {
                val sourceId = sources[viewPager.currentItem].first
                if (sourceId == -1L) {
                    addToDataSet(webPages = vm.chapters!!)
                } else {
                    addToDataSet(webPages = vm.chapters!!.filter { it.sourceId == sourceId })
                }
                EventBus.getDefault().post(ChapterActionModeEvent(sourceId, EventType.UPDATE))
            }
            R.id.action_clear_selection -> {
                val sourceId = sources[viewPager.currentItem].first
                if (sourceId == -1L) {
                    removeFromDataSet(webPages = vm.chapters!!)
                } else {
                    removeFromDataSet(webPages = vm.chapters!!.filter { it.sourceId == sourceId })
                }
                removeFromDataSet(webPages = vm.chapters!!.filter { it.sourceId == sourceId })
                EventBus.getDefault().post(ChapterActionModeEvent(sourceId, EventType.UPDATE))
            }
            R.id.action_share_chapter -> {
                val urls = dataSet.joinToString(separator = ", ") { it.url }
                shareUrl(urls)
            }
            R.id.action_delete -> {
                setProgressDialog("Deleting downloaded chapters…", dataSet.size)
                vm.updateChapters(ArrayList(dataSet), ChaptersViewModel.Action.DELETE_DOWNLOADS)
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
    fun confirmDialog(content: String, positiveCallback: MaterialDialog.SingleButtonCallback, negativeCallback: MaterialDialog.SingleButtonCallback? = null) {
        if (confirmDialog == null || !confirmDialog!!.isShowing) {
            val dialogBuilder = MaterialDialog.Builder(this)
                .title(getString(R.string.confirm_action))
                .content(content)
                .positiveText(getString(R.string.okay))
                .negativeText(R.string.cancel)
                .onPositive(positiveCallback)

            if (negativeCallback != null) {
                dialogBuilder.onNegative(negativeCallback)
            } else {
                dialogBuilder.onNegative { dialog, _ -> dialog.dismiss() }
            }

            confirmDialog = dialogBuilder.build()
            confirmDialog?.show()
        }
    }

    private fun showAlertDialog(message: String) {
        MaterialDialog.Builder(this)
            .iconRes(R.drawable.ic_warning_white_vector)
            .title(getString(R.string.alert))
            .content(message)
            .positiveText(getString(R.string.okay))
            .onPositive { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showNotInLibraryDialog() {
        showAlertDialog(getString(R.string.novel_not_in_library_dialog_content))
    }

    private fun manageDownloadsDialog() {
        //startDownloadNovelService(vm.novel.name)
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

    private fun setProgressDialog(message: String, maxProgress: Int) {
        progressMessage = message
        this.maxProgress = maxProgress
        progressDialog = MaterialDialog.Builder(this@ChaptersPagerActivity)
            .content(message)
            .progress(false, maxProgress, true)
            .cancelable(false).build()
    }
    //endregion

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("maxProgress", maxProgress)
        outState.putString("progressMessage", progressMessage)
        outState.putBoolean("isProgressShowing", progressDialog?.isShowing ?: false)
        outState.putSerializable("novel", vm.novel)
    }

}

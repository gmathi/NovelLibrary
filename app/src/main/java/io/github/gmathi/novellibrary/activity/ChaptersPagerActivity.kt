package io.github.gmathi.novellibrary.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import com.afollestad.materialdialogs.DialogCallback
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.ktx.logEvent
import com.tingyik90.snackprogressbar.SnackProgressBar
import com.tingyik90.snackprogressbar.SnackProgressBarManager
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.ChaptersPageListener
import io.github.gmathi.novellibrary.adapter.GenericFragmentStatePagerAdapter
import io.github.gmathi.novellibrary.database.getWebPage
import io.github.gmathi.novellibrary.database.updateNovel
import io.github.gmathi.novellibrary.databinding.ActivityChaptersPagerBinding
import io.github.gmathi.novellibrary.extensions.*
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.other.ChapterActionModeEvent
import io.github.gmathi.novellibrary.model.other.EventType
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Constants.ALL_TRANSLATOR_SOURCES
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.system.shareUrl
import io.github.gmathi.novellibrary.viewmodel.ChaptersViewModel
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.collections.ArrayList


class ChaptersPagerActivity : BaseActivity(), ActionMode.Callback {

    companion object {
        //private const val TAG = "ChaptersPagerActivity"
    }

    val vm: ChaptersViewModel by viewModels()

    var dataSet: HashSet<WebPage> = HashSet()
    private val translatorSourceNames: ArrayList<String> = ArrayList()
    private var actionMode: ActionMode? = null

    private var confirmDialog: MaterialDialog? = null

    private var maxProgress: Int = 0
    private var progressMessage = "In Progress…"
    private var isSyncing = false
    private var isChaptersProcessing = false

    private val snackProgressBarManager by lazy { Utils.createSnackProgressBarManager(findViewById(android.R.id.content), this) }
    private var snackProgressBar: SnackProgressBar? = null

    private lateinit var binding: ActivityChaptersPagerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChaptersPagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val novel: Novel?
        @Suppress("LiftReturnOrAssignment")
        if (savedInstanceState != null) {
            val isProgressShowing = savedInstanceState.getBoolean("isProgressShowing", false)
            if (isProgressShowing) {
                setProgressDialog(savedInstanceState.getString("progressMessage", "In Progress…"), savedInstanceState.getInt("maxProgress", 0))
                showProgressDialog()
            }
            novel = savedInstanceState.getParcelable("novel")
        } else {
            novel = intent.getParcelableExtra("novel")
        }

        if (novel == null) return
        vm.init(novel, this, this)

        addListeners()
        addObservers()
    }

    private fun addListeners() {
        binding.activityChaptersPager.sourcesToggle.setOnClickListener {
            if (networkHelper.isConnectedToNetwork()) {
                vm.toggleSources()
            } else {
                confirmDialog("You need to have internet connection to do this!", { dialog ->
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
                    binding.activityChaptersPager.progressLayout.showLoading(loadingText = getString(R.string.loading))
                }
                Constants.Status.EMPTY_DATA -> {
                    binding.activityChaptersPager.progressLayout.showEmpty(resId = R.raw.monkey_logo, isLottieAnimation = true, emptyText = getString(R.string.empty_chapters))
                }
                Constants.Status.NETWORK_ERROR -> {
                    binding.activityChaptersPager.progressLayout.showError(errorText = getString(R.string.failed_to_load_url), buttonText = getString(R.string.try_again)) {
                        vm.getData()
                    }
                }
                Constants.Status.NO_INTERNET -> {
                    binding.activityChaptersPager.progressLayout.noInternetError {
                        vm.getData()
                    }
                }
                Constants.Status.DONE -> {
                    isSyncing = false
                    binding.activityChaptersPager.progressLayout.showContent()
                    setViewPager()

                    //TODO: Recreate menu for those instance where the chapters can be empty.
                }
                else -> {
                    binding.activityChaptersPager.progressLayout.updateLoadingStatus(newStatus)
                }
            }
        }

        vm.actionModeProgress.observe(this) { progress ->
            //Update Action mode actions status
            when (progress) {
                Constants.Status.START -> {
                    showProgressDialog()
                }
                Constants.Status.DONE -> {
                    isChaptersProcessing = false
                    snackProgressBar = null
                    snackProgressBarManager.dismiss()
                    EventBus.getDefault().post(ChapterActionModeEvent(eventType = EventType.COMPLETE))
                }
                else -> {
                    setProgressDialogValue(progress.toInt())
                }
            }
        }

    }


    private fun setViewPager() {
        while (supportFragmentManager.backStackEntryCount > 0)
            supportFragmentManager.popBackStack()

        translatorSourceNames.clear()
        val chapters = vm.chapters ?: throw Error("Chapters cannot be null")
        val sourcesList = chapters.distinctBy { it.translatorSourceName }.mapNotNull { it.translatorSourceName }

        if (vm.showSources && sourcesList.isNotEmpty()) {
            translatorSourceNames.addAll(sourcesList)
        } else {
            translatorSourceNames.addAll(listOf(ALL_TRANSLATOR_SOURCES))
        }

        val navPageAdapter =
            GenericFragmentStatePagerAdapter(supportFragmentManager, translatorSourceNames.toTypedArray(), translatorSourceNames.size, ChaptersPageListener(vm.novel, translatorSourceNames))
        binding.activityChaptersPager.viewPager.offscreenPageLimit = 3
        binding.activityChaptersPager.viewPager.adapter = navPageAdapter
        binding.activityChaptersPager.tabStrip.setViewPager(binding.activityChaptersPager.viewPager)
        scrollToBookmark()
    }


    private fun scrollToBookmark() {
        vm.novel.currentChapterUrl?.let { currentChapterUrl ->
            val currentBookmarkWebPage = dbHelper.getWebPage(currentChapterUrl) ?: return@let
            val currentSource = translatorSourceNames.firstOrNull { it == currentBookmarkWebPage.translatorSourceName }
                ?: return@let
            val index = translatorSourceNames.indexOf(currentSource)
            if (index != -1)
                binding.activityChaptersPager.viewPager.currentItem = index
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
                confirmDialog(getString(R.string.download_all_chapters_dialog_content), { dialog ->
                    val publisher = vm.novel.metadata["English Publisher"]
                    val isWuxiaChapterPresent = publisher?.contains("Wuxiaworld", ignoreCase = true) ?: false
                    if (dataCenter.disableWuxiaDownloads && isWuxiaChapterPresent) {
                        dialog.dismiss()
                        showWuxiaWorldDownloadDialog()
                    } else {
                        dialog.dismiss()
                        vm.chapters?.let {
                            setProgressDialog("Adding chapters to download queue…", it.size)
                            vm.updateChapters(it, ChaptersViewModel.Action.ADD_DOWNLOADS, callback = {
                                manageDownloadsDialog()
                                firebaseAnalytics.logEvent(FAC.Event.DOWNLOAD_NOVEL) {
                                    param(FAC.Param.NOVEL_NAME, vm.novel.name)
                                    param(FAC.Param.NOVEL_URL, vm.novel.url)
                                }
                            })
                        }
                    }
                })
                return true
            }
            R.id.action_add_to_library -> {
                vm.addNovelToLibrary()
                invalidateOptionsMenu()
                firebaseAnalytics.logEvent(FAC.Event.ADD_NOVEL) {
                    param(FAC.Param.NOVEL_NAME, vm.novel.name)
                    param(FAC.Param.NOVEL_URL, vm.novel.url)
                }
                return true
            }
            R.id.action_sort -> {
                if (vm.novel.metadata["chapterOrder"] == "des")
                    vm.novel.metadata["chapterOrder"] = "asc"
                else
                    vm.novel.metadata["chapterOrder"] = "des"
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
                confirmDialog(getString(R.string.mark_chapters_unread_dialog_content), { dialog ->
                    setProgressDialog("Marking chapters as unread…", dataSet.size)
                    vm.updateChapters(ArrayList(dataSet), ChaptersViewModel.Action.MARK_UNREAD)
                    dialog.dismiss()
                    mode?.finish()
                })
            }
            R.id.action_read -> {
                confirmDialog(getString(R.string.mark_chapters_read_dialog_content), { dialog ->
                    setProgressDialog("Marking chapters as read…", dataSet.size)
                    vm.updateChapters(ArrayList(dataSet), ChaptersViewModel.Action.MARK_READ)
                    dialog.dismiss()
                    mode?.finish()
                })
            }
            R.id.action_unfav -> {
                confirmDialog(getString(R.string.unfavorite_chapters_read_dialog_content), { dialog ->
                    setProgressDialog("Removing chapters from favorites…", dataSet.size)
                    vm.updateChapters(ArrayList(dataSet), ChaptersViewModel.Action.REMOVE_FAVORITE)
                    dialog.dismiss()
                    mode?.finish()
                })
            }
            R.id.action_fav -> {
                confirmDialog(getString(R.string.favorite_chapters_read_dialog_content), { dialog ->
                    setProgressDialog("Marking chapters as favorites…", dataSet.size)
                    vm.updateChapters(ArrayList(dataSet), ChaptersViewModel.Action.MARK_FAVORITE)
                    dialog.dismiss()
                    mode?.finish()
                })
            }


            R.id.action_download -> {
                confirmDialog(getString(R.string.download_chapters_dialog_content), { dialog ->
                    if (vm.novel.id == -1L) {
                        dialog.dismiss()
                        showNotInLibraryDialog()
                        mode?.finish()
                    } else {
                        val publisher = vm.novel.metadata["English Publisher"]
                        val isWuxiaChapterPresent = publisher?.contains("Wuxiaworld", ignoreCase = true) ?: false
                        if (dataCenter.disableWuxiaDownloads && isWuxiaChapterPresent) {
                            dialog.dismiss()
                            showWuxiaWorldDownloadDialog()
                        } else {
                            val listToDownload = ArrayList(dataSet)
                            if (listToDownload.isNotEmpty()) {
                                dialog.dismiss()
                                setProgressDialog("Add to Downloads…", listToDownload.size)
                                vm.updateChapters(listToDownload, ChaptersViewModel.Action.ADD_DOWNLOADS) {
                                    manageDownloadsDialog()
                                    firebaseAnalytics.logEvent(FAC.Event.DOWNLOAD_NOVEL) {
                                        param(FAC.Param.NOVEL_NAME, vm.novel.name)
                                        param(FAC.Param.NOVEL_URL, vm.novel.url)
                                    }
                                }
                                mode?.finish()
                            }
                        }
                    }
                })
            }
            R.id.action_select_interval -> {
                val translatorSourceName = translatorSourceNames[binding.activityChaptersPager.viewPager.currentItem]
                if (translatorSourceName == ALL_TRANSLATOR_SOURCES) {
                    val chaptersForSource = vm.chapters!!.sortedBy { it.orderId }
                    val selectedChaptersForSource = dataSet.sortedBy { it.orderId }
                    if (selectedChaptersForSource.size > 1) {
                        val firstIndex = chaptersForSource.indexOf(selectedChaptersForSource.first())
                        val lastIndex = chaptersForSource.indexOf(selectedChaptersForSource.last())
                        val subList = chaptersForSource.subList(firstIndex, lastIndex)
                        addToDataSet(subList)
                        EventBus.getDefault().post(ChapterActionModeEvent(translatorSourceName, EventType.UPDATE))
                    }
                } else {
                    val chaptersForSource = vm.chapters!!.filter { it.translatorSourceName == translatorSourceName }.sortedBy { it.orderId }
                    val selectedChaptersForSource = dataSet.filter { it.translatorSourceName == translatorSourceName }.sortedBy { it.orderId }
                    if (selectedChaptersForSource.size > 1) {
                        val firstIndex = chaptersForSource.indexOf(selectedChaptersForSource.first())
                        val lastIndex = chaptersForSource.indexOf(selectedChaptersForSource.last())
                        val subList = chaptersForSource.subList(firstIndex, lastIndex)
                        addToDataSet(subList)
                        EventBus.getDefault().post(ChapterActionModeEvent(translatorSourceName, EventType.UPDATE))
                    }
                }
            }
            R.id.action_select_all -> {
                val translatorSourceName = translatorSourceNames[binding.activityChaptersPager.viewPager.currentItem]
                if (translatorSourceName == ALL_TRANSLATOR_SOURCES) {
                    addToDataSet(webPages = vm.chapters!!)
                } else {
                    addToDataSet(webPages = vm.chapters!!.filter { it.translatorSourceName == translatorSourceName })
                }
                EventBus.getDefault().post(ChapterActionModeEvent(translatorSourceName, EventType.UPDATE))
            }
            R.id.action_clear_selection -> {
                val translatorSourceName = translatorSourceNames[binding.activityChaptersPager.viewPager.currentItem]
                if (translatorSourceName == ALL_TRANSLATOR_SOURCES) {
                    removeFromDataSet(webPages = vm.chapters!!)
                } else {
                    removeFromDataSet(webPages = vm.chapters!!.filter { it.translatorSourceName == translatorSourceName })
                }
                removeFromDataSet(webPages = vm.chapters!!.filter { it.translatorSourceName == translatorSourceName })
                EventBus.getDefault().post(ChapterActionModeEvent(translatorSourceName, EventType.UPDATE))
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
    fun confirmDialog(content: String, positiveCallback: DialogCallback, negativeCallback: DialogCallback? = null) {
        if (confirmDialog == null || !confirmDialog!!.isShowing) {
            MaterialDialog(this).show {
                title(R.string.confirm_action)
                message(text = content)
                positiveButton(R.string.okay, click = positiveCallback)
                negativeButton(R.string.cancel, click = negativeCallback)

                lifecycleOwner(this@ChaptersPagerActivity)
            }
        }
    }

    private fun showAlertDialog() {
        MaterialDialog(this).show {
            icon(R.drawable.ic_warning_white_vector)
            title(R.string.alert)
            positiveButton(R.string.okay) {
                it.dismiss()
            }
            lifecycleOwner(this@ChaptersPagerActivity)
        }
    }

    private fun showNotInLibraryDialog() {
        showAlertDialog()
    }

    private fun showWuxiaWorldDownloadDialog() {
        showAlertDialog()
    }

    private fun manageDownloadsDialog() {
        //startDownloadNovelService(vm.novel.name)
        MaterialDialog(this).show {
            icon(R.drawable.ic_info_white_vector)
            title(R.string.manage_downloads)
            message(R.string.manage_downloads_dialog_content)
            positiveButton(R.string.take_me_there) {
                it.dismiss()
                setResult(Constants.OPEN_DOWNLOADS_RES_CODE)
                finish()
            }
            negativeButton(R.string.okay) {
                it.dismiss()
            }
        }
    }

    private fun showProgressDialog() {
        if (snackProgressBar != null) {
            snackProgressBarManager.show(snackProgressBar!!, SnackProgressBarManager.LENGTH_INDEFINITE)
        }
    }

    private fun setProgressDialog(message: String, maxProgress: Int) {
        isChaptersProcessing = true
        progressMessage = message
        this.maxProgress = maxProgress

        if (snackProgressBar != null) {
            snackProgressBar = null
            snackProgressBarManager.dismissAll()
        }

        if (maxProgress == 0 || maxProgress > 10) {
            snackProgressBar = SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, message)
                .setProgressMax(maxProgress)
        } else {
            showSnackbar(message)
        }
    }

    private fun setProgressDialogValue(progress: Int) {
        if (snackProgressBar != null) {
            snackProgressBarManager.setProgress(progress)
        }
    }

    private fun showSnackbar(message: String) {
        val snackbar = Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_SHORT
        )
        snackbar.show()
    }
    //endregion

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("maxProgress", maxProgress)
        outState.putString("progressMessage", progressMessage)
        outState.putBoolean("isProgressShowing", snackProgressBar != null)
        outState.putParcelable("novel", vm.novel)
    }

    override fun onBackPressed() {
        if (isSyncing || isChaptersProcessing) {
            return
        }
        super.onBackPressed()
    }

}

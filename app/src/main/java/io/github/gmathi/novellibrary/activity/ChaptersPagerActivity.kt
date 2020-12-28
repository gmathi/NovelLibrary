package io.github.gmathi.novellibrary.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import com.afollestad.materialdialogs.MaterialDialog
import com.google.firebase.analytics.ktx.logEvent
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.ChaptersPageListener
import io.github.gmathi.novellibrary.adapter.GenericFragmentStatePagerAdapter
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.getTranslatorSource
import io.github.gmathi.novellibrary.database.getWebPage
import io.github.gmathi.novellibrary.database.updateNovel
import io.github.gmathi.novellibrary.databinding.ActivityChaptersPagerBinding
import io.github.gmathi.novellibrary.databinding.ContentChaptersPagerBinding
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.extensions.*
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.TranslatorSource
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.other.ChapterActionModeEvent
import io.github.gmathi.novellibrary.model.other.EventType
import io.github.gmathi.novellibrary.util.Constants
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
    private val translatorSources: ArrayList<TranslatorSource> = ArrayList()
    private var actionMode: ActionMode? = null

    private var confirmDialog: MaterialDialog? = null
    private var progressDialog: MaterialDialog? = null

    private var maxProgress: Int = 0
    private var progressMessage = "In Progress…"
    private var isSyncing = false

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
        binding.activityChaptersPager.sourcesToggle.setOnClickListener {
            if (Utils.isConnectedToNetwork(this)) {
                vm.toggleSources()
            } else {
                confirmDialog("You need to have internet connection to do this!", { dialog, _ ->
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
//                    progressLayout.showLoading()
                    binding.activityChaptersPager.progressLayout.showLoading(loadingText = getString(R.string.loading))
                }
                Constants.Status.EMPTY_DATA -> {
                    binding.activityChaptersPager.progressLayout.showEmpty(resId = R.raw.monkey_logo, isLottieAnimation = true, emptyText = getString(R.string.empty_chapters))
                }
                Constants.Status.NETWORK_ERROR -> {
                    binding.activityChaptersPager.progressLayout.showError(errorText = getString(R.string.failed_to_load_url), buttonText = getString(R.string.try_again), onClickListener = {
                        vm.getData()
                    })
                }
                Constants.Status.NO_INTERNET -> {
                    binding.activityChaptersPager.progressLayout.noInternetError({
                        vm.getData()
                    })
                }
                Constants.Status.DONE -> {
                    isSyncing = false
                    binding.activityChaptersPager.progressLayout.showContent()
                    setViewPager()

                    //TODO: Recreate menu for those instance where thee chapters can be empty.
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

        translatorSources.clear()
        val chapters = vm.chapters ?: throw Error("Chapters cannot be null")

        if (vm.showSources) {
            val sourceIds = chapters.distinctBy { it.translatorSourceId }.map { it.translatorSourceId }
            sourceIds.forEach { sourceId -> dbHelper.getTranslatorSource(sourceId)?.let { translatorSources.add(it) } }
        } else {
            dbHelper.getTranslatorSource(-1L)?.let { translatorSources.add(it) }
        }

        val titles = translatorSources.map { it.name }.toTypedArray()

        val navPageAdapter = GenericFragmentStatePagerAdapter(supportFragmentManager, titles, titles.size, ChaptersPageListener(vm.novel, translatorSources))
        binding.activityChaptersPager.viewPager.offscreenPageLimit = 3
        binding.activityChaptersPager.viewPager.adapter = navPageAdapter
        binding.activityChaptersPager.tabStrip.setViewPager(binding.activityChaptersPager.viewPager)
        scrollToBookmark()
    }


    private fun scrollToBookmark() {
        vm.novel.currentChapterUrl?.let { currentChapterUrl ->
            val currentBookmarkWebPage = dbHelper.getWebPage(currentChapterUrl) ?: return@let
            val currentSource = translatorSources.firstOrNull { it.id == currentBookmarkWebPage.translatorSourceId }
                ?: return@let
            val index = translatorSources.indexOf(currentSource)
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
                confirmDialog(getString(R.string.download_all_chapters_dialog_content), { dialog, _ ->
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
                confirmDialog(getString(R.string.mark_chapters_unread_dialog_content), { dialog, _ ->
                    setProgressDialog("Marking chapters as unread…", dataSet.size)
                    vm.updateChapters(ArrayList(dataSet), ChaptersViewModel.Action.MARK_UNREAD)
                    dialog.dismiss()
                    mode?.finish()
                })
            }
            R.id.action_read -> {
                confirmDialog(getString(R.string.mark_chapters_read_dialog_content), { dialog, _ ->
                    setProgressDialog("Marking chapters as read…", dataSet.size)
                    vm.updateChapters(ArrayList(dataSet), ChaptersViewModel.Action.MARK_READ)
                    dialog.dismiss()
                    mode?.finish()
                })
            }
            R.id.action_unfav -> {
                confirmDialog(getString(R.string.unfavorite_chapters_read_dialog_content), { dialog, _ ->
                    setProgressDialog("Removing chapters from favorites…", dataSet.size)
                    vm.updateChapters(ArrayList(dataSet), ChaptersViewModel.Action.REMOVE_FAVORITE)
                    dialog.dismiss()
                    mode?.finish()
                })
            }
            R.id.action_fav -> {
                confirmDialog(getString(R.string.favorite_chapters_read_dialog_content), { dialog, _ ->
                    setProgressDialog("Marking chapters as favorites…", dataSet.size)
                    vm.updateChapters(ArrayList(dataSet), ChaptersViewModel.Action.MARK_FAVORITE)
                    dialog.dismiss()
                    mode?.finish()
                })
            }


            R.id.action_download -> {
                confirmDialog(getString(R.string.download_chapters_dialog_content), { dialog, _ ->
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
                val translatorSourceId = translatorSources[binding.activityChaptersPager.viewPager.currentItem].id
                if (translatorSourceId == -1L) {
                    val chaptersForSource = vm.chapters!!.sortedBy { it.orderId }
                    val selectedChaptersForSource = dataSet.sortedBy { it.orderId }
                    if (selectedChaptersForSource.size > 1) {
                        val firstIndex = chaptersForSource.indexOf(selectedChaptersForSource.first())
                        val lastIndex = chaptersForSource.indexOf(selectedChaptersForSource.last())
                        val subList = chaptersForSource.subList(firstIndex, lastIndex)
                        addToDataSet(subList)
                        EventBus.getDefault().post(ChapterActionModeEvent(translatorSourceId, EventType.UPDATE))
                    }
                } else {
                    val chaptersForSource = vm.chapters!!.filter { it.translatorSourceId == translatorSourceId }.sortedBy { it.orderId }
                    val selectedChaptersForSource = dataSet.filter { it.translatorSourceId == translatorSourceId }.sortedBy { it.orderId }
                    if (selectedChaptersForSource.size > 1) {
                        val firstIndex = chaptersForSource.indexOf(selectedChaptersForSource.first())
                        val lastIndex = chaptersForSource.indexOf(selectedChaptersForSource.last())
                        val subList = chaptersForSource.subList(firstIndex, lastIndex)
                        addToDataSet(subList)
                        EventBus.getDefault().post(ChapterActionModeEvent(translatorSourceId, EventType.UPDATE))
                    }
                }
            }
            R.id.action_select_all -> {
                val translatorSourceId = translatorSources[binding.activityChaptersPager.viewPager.currentItem].id
                if (translatorSourceId == -1L) {
                    addToDataSet(webPages = vm.chapters!!)
                } else {
                    addToDataSet(webPages = vm.chapters!!.filter { it.translatorSourceId == translatorSourceId })
                }
                EventBus.getDefault().post(ChapterActionModeEvent(translatorSourceId, EventType.UPDATE))
            }
            R.id.action_clear_selection -> {
                val translatorSourceId = translatorSources[binding.activityChaptersPager.viewPager.currentItem].id
                if (translatorSourceId == -1L) {
                    removeFromDataSet(webPages = vm.chapters!!)
                } else {
                    removeFromDataSet(webPages = vm.chapters!!.filter { it.translatorSourceId == translatorSourceId })
                }
                removeFromDataSet(webPages = vm.chapters!!.filter { it.translatorSourceId == translatorSourceId })
                EventBus.getDefault().post(ChapterActionModeEvent(translatorSourceId, EventType.UPDATE))
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

    private fun showWuxiaWorldDownloadDialog() {
        showAlertDialog("Downloads are not supported for WuxiaWorld content. Please use their app for downloads/offline reading WuxiaWorld novels.")
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

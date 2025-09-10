package io.github.gmathi.novellibrary.fragment

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs

import com.afollestad.materialdialogs.DialogCallback
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.logEvent
import com.tingyik90.snackprogressbar.SnackProgressBar
import com.tingyik90.snackprogressbar.SnackProgressBarManager
import dagger.hilt.android.AndroidEntryPoint
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.ChaptersPageListener
import io.github.gmathi.novellibrary.adapter.GenericFragmentStatePagerAdapter
import io.github.gmathi.novellibrary.database.getNovel
import io.github.gmathi.novellibrary.database.getWebPage
import io.github.gmathi.novellibrary.database.updateNovel
import io.github.gmathi.novellibrary.database.updateNovelMetaData
import io.github.gmathi.novellibrary.databinding.FragmentChaptersMainBinding
import io.github.gmathi.novellibrary.extensions.*
import io.github.gmathi.novellibrary.model.ChaptersUiState
import io.github.gmathi.novellibrary.model.UiState
import io.github.gmathi.novellibrary.model.handle
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.model.other.ChapterActionModeEvent
import io.github.gmathi.novellibrary.model.other.DownloadWebPageEvent
import io.github.gmathi.novellibrary.model.other.EventType
import io.github.gmathi.novellibrary.service.download.DownloadListener
import io.github.gmathi.novellibrary.service.download.DownloadNovelService
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Constants.ALL_TRANSLATOR_SOURCES
import io.github.gmathi.novellibrary.util.FAC
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.navigation.NavigationManager
import io.github.gmathi.novellibrary.util.system.shareUrl
import io.github.gmathi.novellibrary.util.system.startDownloadNovelService
import io.github.gmathi.novellibrary.viewmodel.ChaptersMainViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class ChaptersMainFragment : BaseViewBindingFragment<FragmentChaptersMainBinding>(), ActionMode.Callback, DownloadListener, MenuProvider {

    companion object {
        private const val TAG = "ChaptersMainFragment"
    }

    private val args: ChaptersMainFragmentArgs by navArgs()
    internal val viewModel: ChaptersMainViewModel by viewModels()

    @Inject
    lateinit var navigationManager: NavigationManager

    var dataSet: HashSet<WebPage> = HashSet()
    
    // Properties for child fragments to access
    val novel: Novel?
        get() = (viewModel.uiState.value as? ChaptersUiState.Success)?.novel
    
    val chapters: List<WebPage>?
        get() = (viewModel.uiState.value as? ChaptersUiState.Success)?.chapters
        
    val chapterSettings: List<WebPageSettings>?
        get() = (viewModel.uiState.value as? ChaptersUiState.Success)?.chapterSettings
    private val translatorSourceNames: ArrayList<String> = ArrayList()
    private var actionMode: ActionMode? = null

    private var confirmDialog: MaterialDialog? = null

    private var downloadNovelService: DownloadNovelService? = null
    private var isServiceConnected: Boolean = false

    private var maxProgress: Int = 0
    private var progressMessage = "In Progress…"
    private var isSyncing = false
    private var isChaptersProcessing = false

    private val snackProgressBarManager by lazy { 
        Utils.createSnackProgressBarManager(requireActivity().findViewById(android.R.id.content), requireActivity()) 
    }
    private var snackProgressBar: SnackProgressBar? = null

    /** Defines callbacks for service binding, passed to bindService()  */
    private val mConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as DownloadNovelService.DownloadNovelBinder
            downloadNovelService = binder.getService()
            downloadNovelService?.downloadListener = this@ChaptersMainFragment
            isServiceConnected = true
        }

        override fun onServiceDisconnected(className: ComponentName) {
            isServiceConnected = false
            downloadNovelService?.downloadListener = null
        }
    }

    override fun getLayoutId() = R.layout.fragment_chapters_main

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        if (view != null) {
            val binding = FragmentChaptersMainBinding.bind(view)
            setBinding(binding)
            
            // Setup menu
            val menuHost: MenuHost = requireActivity()
            menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Load novel from database using the ID from navigation args
        val novelId = args.novelId
        val novel = dbHelper.getNovel(novelId) ?: return
        
        viewModel.initialize(novel)
        
        addListeners()
        observeViewModel()
    }

    private fun addListeners() {
        binding.sourcesToggle.setOnClickListener {
            if (networkHelper.isConnectedToNetwork()) {
                // Toggle between showing all sources and showing individual sources
                viewModel.toggleSources()
            } else {
                confirmDialog("You need to have internet connection to do this!", { dialog ->
                    dialog.dismiss()
                })
            }
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            state.handle(
                onLoading = {
                    binding.progressLayout.showLoading(loadingText = getString(R.string.loading))
                },
                onSuccess = { successState ->
                    isSyncing = false
                    binding.progressLayout.showContent()
                    setViewPager(successState)
                },
                onError = { message, _ ->
                    binding.progressLayout.showError(
                        errorText = message,
                        buttonText = getString(R.string.try_again)
                    ) {
                        viewModel.loadChapters()
                    }
                },
                onEmpty = { message ->
                    binding.progressLayout.showEmpty(
                        resId = R.raw.monkey_logo,
                        isLottieAnimation = true,
                        emptyText = message
                    )
                },
                onNoInternet = {
                    binding.progressLayout.noInternetError {
                        viewModel.loadChapters()
                    }
                },
                onNetworkError = {
                    binding.progressLayout.showError(
                        errorText = getString(R.string.failed_to_load_url),
                        buttonText = getString(R.string.try_again)
                    ) {
                        viewModel.loadChapters()
                    }
                }
            )
        }

        viewModel.actionModeState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    showProgressDialog()
                }
                is UiState.Success -> {
                    isChaptersProcessing = false
                    snackProgressBar = null
                    snackProgressBarManager.dismiss()
                    EventBus.getDefault().post(ChapterActionModeEvent(eventType = EventType.COMPLETE))
                }
                is UiState.Error -> {
                    isChaptersProcessing = false
                    snackProgressBar = null
                    snackProgressBarManager.dismiss()
                    showSnackBar(state.message)
                }
            }
        }
    }

    private fun setViewPager(state: ChaptersUiState.Success) {
        while (childFragmentManager.backStackEntryCount > 0)
            childFragmentManager.popBackStack()

        translatorSourceNames.clear()
        translatorSourceNames.addAll(state.translatorSources)

        val navPageAdapter =
            GenericFragmentStatePagerAdapter(
                childFragmentManager,
                translatorSourceNames.toTypedArray(),
                translatorSourceNames.size,
                ChaptersPageListener(state.novel, translatorSourceNames)
            )
        binding.viewPager.offscreenPageLimit = 3
        binding.viewPager.adapter = navPageAdapter
        binding.tabStrip.setViewPager(binding.viewPager)
        scrollToBookmark(state.novel)
    }

    private fun scrollToBookmark(novel: Novel) {
        novel.currentChapterUrl?.let { currentChapterUrl ->
            val currentBookmarkWebPage = dbHelper.getWebPage(currentChapterUrl) ?: return@let
            val currentSource = translatorSourceNames.firstOrNull { it == currentBookmarkWebPage.translatorSourceName }
                ?: return@let
            val index = translatorSourceNames.indexOf(currentSource)
            if (index != -1)
                binding.viewPager.currentItem = index
        }
    }

    //region Menu Provider
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_activity_chapters_pager, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        val currentState = viewModel.uiState.value as? ChaptersUiState.Success
        val novelId = currentState?.novel?.id ?: -1L
        menu.findItem(R.id.action_add_to_library)?.isVisible = (novelId == -1L)
        menu.findItem(R.id.action_download)?.isVisible = (novelId != -1L)
    }

    private var devCounter: Int = 0
    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.action_sync -> {
                if (!isSyncing) {
                    isSyncing = true
                    viewModel.loadChapters(forceRefresh = true)
                }
                devCounter++
                if (devCounter == 40) dataCenter.isDeveloper = true
                return true
            }

            R.id.action_download -> {
                confirmDialog(getString(R.string.download_all_chapters_dialog_content), { dialog ->
                    val currentState = viewModel.uiState.value as? ChaptersUiState.Success ?: return@confirmDialog
                    val publisher = currentState.novel.metadata["English Publisher"]
                    val isWuxiaChapterPresent = publisher?.contains("Wuxiaworld", ignoreCase = true)
                        ?: false
                    if (dataCenter.disableWuxiaDownloads && isWuxiaChapterPresent) {
                        dialog.dismiss()
                        showWuxiaWorldDownloadDialog()
                    } else {
                        dialog.dismiss()
                        setProgressDialog("Adding chapters to download queue…", currentState.chapters.size)
                        viewModel.updateChapters(currentState.chapters, ChaptersMainViewModel.ChapterAction.ADD_DOWNLOADS, callback = {
                            if (Utils.isServiceRunning(requireContext(), DownloadNovelService.QUALIFIED_NAME)) {
                                downloadNovelService?.handleNovelDownload(currentState.novel.id, DownloadNovelService.ACTION_START)
                            } else {
                                (requireActivity() as AppCompatActivity).startDownloadNovelService(currentState.novel.id)
                                bindService()
                            }
                            manageDownloadsDialog()
                            firebaseAnalytics.logEvent(FAC.Event.DOWNLOAD_NOVEL) {
                                param(FAC.Param.NOVEL_NAME, currentState.novel.name)
                                param(FAC.Param.NOVEL_URL, currentState.novel.url)
                            }
                        })
                    }
                })
                return true
            }

            R.id.action_add_to_library -> {
                viewModel.addNovelToLibrary()
                requireActivity().invalidateOptionsMenu()
                return true
            }

            R.id.action_sort -> {
                val currentState = viewModel.uiState.value as? ChaptersUiState.Success ?: return false
                val novel = currentState.novel
                // Toggle chapter order between ascending and descending
                if (novel.metadata["chapterOrder"] == "des")
                    novel.metadata["chapterOrder"] = "asc"
                else
                    novel.metadata["chapterOrder"] = "des"
                
                // Notify all chapter fragments to refresh their display
                EventBus.getDefault().post(ChapterActionModeEvent(eventType = EventType.COMPLETE))
                
                // Update novel metadata in database if it's in library
                if (novel.id != -1L) {
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            dbHelper.updateNovelMetaData(novel)
                        }
                    }
                }
                return true
            }
        }
        return false
    }
    //endregion

    //region ActionMode Callback

    internal fun addToDataSet(webPage: WebPage) {
        dataSet.add(webPage)
        if (dataSet.isNotEmpty() && actionMode == null) {
            actionMode = (activity as? androidx.appcompat.app.AppCompatActivity)?.startSupportActionMode(this)
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
            actionMode = (activity as? androidx.appcompat.app.AppCompatActivity)?.startSupportActionMode(this)
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
                    viewModel.updateChapters(ArrayList(dataSet), ChaptersMainViewModel.ChapterAction.MARK_UNREAD)
                    dialog.dismiss()
                    mode?.finish()
                })
            }

            R.id.action_read -> {
                confirmDialog(getString(R.string.mark_chapters_read_dialog_content), { dialog ->
                    setProgressDialog("Marking chapters as read…", dataSet.size)
                    viewModel.updateChapters(ArrayList(dataSet), ChaptersMainViewModel.ChapterAction.MARK_READ)
                    dialog.dismiss()
                    mode?.finish()
                })
            }

            R.id.action_unfav -> {
                confirmDialog(getString(R.string.unfavorite_chapters_read_dialog_content), { dialog ->
                    setProgressDialog("Removing chapters from favorites…", dataSet.size)
                    viewModel.updateChapters(ArrayList(dataSet), ChaptersMainViewModel.ChapterAction.REMOVE_FAVORITE)
                    dialog.dismiss()
                    mode?.finish()
                })
            }

            R.id.action_fav -> {
                confirmDialog(getString(R.string.favorite_chapters_read_dialog_content), { dialog ->
                    setProgressDialog("Marking chapters as favorites…", dataSet.size)
                    viewModel.updateChapters(ArrayList(dataSet), ChaptersMainViewModel.ChapterAction.MARK_FAVORITE)
                    dialog.dismiss()
                    mode?.finish()
                })
            }

            R.id.action_download -> {
                confirmDialog(getString(R.string.download_chapters_dialog_content), { dialog ->
                    val currentState = viewModel.uiState.value as? ChaptersUiState.Success ?: return@confirmDialog
                    if (currentState.novel.id == -1L) {
                        dialog.dismiss()
                        showNotInLibraryDialog()
                        mode?.finish()
                    } else {
                        val publisher = currentState.novel.metadata["English Publisher"]
                        val isWuxiaChapterPresent = publisher?.contains("Wuxiaworld", ignoreCase = true)
                            ?: false
                        if (dataCenter.disableWuxiaDownloads && isWuxiaChapterPresent) {
                            dialog.dismiss()
                            showWuxiaWorldDownloadDialog()
                        } else {
                            val listToDownload = ArrayList(dataSet)
                            if (listToDownload.isNotEmpty()) {
                                dialog.dismiss()
                                setProgressDialog("Add to Downloads…", listToDownload.size)
                                viewModel.updateChapters(listToDownload, ChaptersMainViewModel.ChapterAction.ADD_DOWNLOADS) {
                                    if (Utils.isServiceRunning(requireContext(), DownloadNovelService.QUALIFIED_NAME)) {
                                        downloadNovelService?.handleNovelDownload(currentState.novel.id, DownloadNovelService.ACTION_START)
                                    } else {
                                        (requireActivity() as AppCompatActivity).startDownloadNovelService(currentState.novel.id)
                                        bindService()
                                    }
                                    manageDownloadsDialog()
                                    firebaseAnalytics.logEvent(FAC.Event.DOWNLOAD_NOVEL) {
                                        param(FAC.Param.NOVEL_NAME, currentState.novel.name)
                                        param(FAC.Param.NOVEL_URL, currentState.novel.url)
                                    }
                                }
                                mode?.finish()
                            }
                        }
                    }
                })
            }

            R.id.action_select_interval -> {
                val currentState = viewModel.uiState.value as? ChaptersUiState.Success ?: return false
                val translatorSourceName = translatorSourceNames[binding.viewPager.currentItem]
                
                // Select all chapters between the first and last selected chapters
                if (translatorSourceName == ALL_TRANSLATOR_SOURCES) {
                    val chaptersForSource = currentState.chapters.sortedBy { it.orderId }
                    val selectedChaptersForSource = dataSet.sortedBy { it.orderId }
                    if (selectedChaptersForSource.size > 1) {
                        val firstIndex = chaptersForSource.indexOf(selectedChaptersForSource.first())
                        val lastIndex = chaptersForSource.indexOf(selectedChaptersForSource.last())
                        if (firstIndex != -1 && lastIndex != -1 && firstIndex < lastIndex) {
                            val subList = chaptersForSource.subList(firstIndex, lastIndex + 1)
                            addToDataSet(subList)
                            EventBus.getDefault().post(ChapterActionModeEvent(translatorSourceName, EventType.UPDATE))
                        }
                    }
                } else {
                    val chaptersForSource = currentState.chapters.filter { it.translatorSourceName == translatorSourceName }.sortedBy { it.orderId }
                    val selectedChaptersForSource = dataSet.filter { it.translatorSourceName == translatorSourceName }.sortedBy { it.orderId }
                    if (selectedChaptersForSource.size > 1) {
                        val firstIndex = chaptersForSource.indexOf(selectedChaptersForSource.first())
                        val lastIndex = chaptersForSource.indexOf(selectedChaptersForSource.last())
                        if (firstIndex != -1 && lastIndex != -1 && firstIndex < lastIndex) {
                            val subList = chaptersForSource.subList(firstIndex, lastIndex + 1)
                            addToDataSet(subList)
                            EventBus.getDefault().post(ChapterActionModeEvent(translatorSourceName, EventType.UPDATE))
                        }
                    }
                }
            }

            R.id.action_select_all -> {
                val currentState = viewModel.uiState.value as? ChaptersUiState.Success ?: return false
                val translatorSourceName = translatorSourceNames[binding.viewPager.currentItem]
                if (translatorSourceName == ALL_TRANSLATOR_SOURCES) {
                    addToDataSet(webPages = currentState.chapters)
                } else {
                    addToDataSet(webPages = currentState.chapters.filter { it.translatorSourceName == translatorSourceName })
                }
                EventBus.getDefault().post(ChapterActionModeEvent(translatorSourceName, EventType.UPDATE))
            }

            R.id.action_clear_selection -> {
                val currentState = viewModel.uiState.value as? ChaptersUiState.Success ?: return false
                val translatorSourceName = translatorSourceNames[binding.viewPager.currentItem]
                if (translatorSourceName == ALL_TRANSLATOR_SOURCES) {
                    removeFromDataSet(webPages = currentState.chapters)
                } else {
                    removeFromDataSet(webPages = currentState.chapters.filter { it.translatorSourceName == translatorSourceName })
                }
                EventBus.getDefault().post(ChapterActionModeEvent(translatorSourceName, EventType.UPDATE))
            }

            R.id.action_share_chapter -> {
                val urls = dataSet.joinToString(separator = ", ") { it.url }
                (requireActivity() as AppCompatActivity).shareUrl(urls)
            }

            R.id.action_delete -> {
                setProgressDialog("Deleting downloaded chapters…", dataSet.size)
                viewModel.updateChapters(ArrayList(dataSet), ChaptersMainViewModel.ChapterAction.DELETE_DOWNLOADS)
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
            MaterialDialog(requireContext()).show {
                title(R.string.confirm_action)
                message(text = content)
                positiveButton(R.string.okay, click = positiveCallback)
                negativeButton(R.string.cancel, click = negativeCallback)

                lifecycleOwner(this@ChaptersMainFragment)
            }
        }
    }

    private fun showAlertDialog(title: String? = null, message: String? = null) {
        MaterialDialog(requireContext()).show {
            icon(R.drawable.ic_warning_white_vector)
            if (title.isNullOrBlank())
                title(R.string.alert)
            else
                title(text = title)
            message(text = message)
            positiveButton(R.string.okay) {
                it.dismiss()
            }
            lifecycleOwner(this@ChaptersMainFragment)
        }
    }

    private fun showNotInLibraryDialog() {
        showAlertDialog()
    }

    private fun showWuxiaWorldDownloadDialog() {
        showAlertDialog(title = "Downloads Restricted", message = "WuxiaWorld novels cannot be downloaded.")
    }

    private fun manageDownloadsDialog() {
        MaterialDialog(requireContext()).show {
            icon(R.drawable.ic_info_white_vector)
            title(R.string.manage_downloads)
            message(R.string.manage_downloads_dialog_content)
            positiveButton(R.string.take_me_there) {
                it.dismiss()
                // Navigate to downloads using Navigation Component
                try {
                    navigationManager.navigateToDownloads(findNavController())
                } catch (e: Exception) {
                    Logs.error(TAG, "Error navigating to downloads", e)
                }
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
            showSnackBar(message)
        }
    }

    private fun setProgressDialogValue(progress: Int) {
        if (snackProgressBar != null) {
            snackProgressBarManager.setProgress(progress)
        }
    }

    private fun showSnackBar(message: String) {
        val snackBar = Snackbar.make(
            requireView(),
            message,
            Snackbar.LENGTH_SHORT
        )
        snackBar.show()
    }
    //endregion

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("maxProgress", maxProgress)
        outState.putString("progressMessage", progressMessage)
        outState.putBoolean("isProgressShowing", snackProgressBar != null)
        outState.putParcelable("novel", (viewModel.uiState.value as? ChaptersUiState.Success)?.novel)
    }

    //Service Binding
    private fun bindService() {
        Intent(requireContext(), DownloadNovelService::class.java).also { intent ->
            requireContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun handleEvent(downloadWebPageEvent: DownloadWebPageEvent) {
        val currentState = viewModel.uiState.value as? ChaptersUiState.Success ?: return
        
        // Only handle events for the current novel
        if (downloadWebPageEvent.download.novelId == currentState.novel.id) {
            when (downloadWebPageEvent.type) {
                EventType.RUNNING -> {
                    // Chapter download started - update UI to show download progress
                    EventBus.getDefault().post(ChapterActionModeEvent(url = downloadWebPageEvent.webPageUrl, eventType = EventType.DOWNLOAD))
                }
                EventType.COMPLETE -> {
                    // Chapter download completed - refresh chapter list to show downloaded status
                    EventBus.getDefault().post(ChapterActionModeEvent(url = downloadWebPageEvent.webPageUrl, eventType = EventType.COMPLETE))
                }
                EventType.PAUSED, EventType.DELETE -> {
                    // Chapter download paused or failed - update UI
                    EventBus.getDefault().post(ChapterActionModeEvent(url = downloadWebPageEvent.webPageUrl, eventType = EventType.UPDATE))
                }
                else -> {
                    // Handle other event types if needed
                }
            }
        }
    }

    /**
     * Check if a specific chapter is currently being downloaded
     */
    fun isChapterDownloading(chapterUrl: String): Boolean {
        val currentState = viewModel.uiState.value as? ChaptersUiState.Success ?: return false
        return downloadNovelService?.isChapterDownloading(currentState.novel.id, chapterUrl) ?: false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isServiceConnected) {
            requireContext().unbindService(mConnection)
        }
    }
}
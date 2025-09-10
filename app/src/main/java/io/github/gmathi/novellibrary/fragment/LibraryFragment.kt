package io.github.gmathi.novellibrary.fragment

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItems
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.tingyik90.snackprogressbar.SnackProgressBar
import com.tingyik90.snackprogressbar.SnackProgressBarManager
import dagger.hilt.android.AndroidEntryPoint
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.databinding.ContentLibraryBinding
import io.github.gmathi.novellibrary.databinding.ListitemLibraryBinding
import io.github.gmathi.novellibrary.util.system.showAlertDialog
import io.github.gmathi.novellibrary.extensions.*
import io.github.gmathi.novellibrary.model.UiState
import io.github.gmathi.novellibrary.model.LibraryUiState
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.other.NovelEvent
import io.github.gmathi.novellibrary.model.other.NovelSectionEvent
import io.github.gmathi.novellibrary.util.*
import io.github.gmathi.novellibrary.util.lang.getGlideUrl
import io.github.gmathi.novellibrary.util.view.SimpleItemTouchHelperCallback
import io.github.gmathi.novellibrary.util.view.SimpleItemTouchListener
import io.github.gmathi.novellibrary.util.view.extensions.applyFont
import io.github.gmathi.novellibrary.util.view.setDefaults
import io.github.gmathi.novellibrary.viewmodel.LibraryViewModel
import io.github.gmathi.novellibrary.viewmodel.SyncProgress
import io.github.gmathi.novellibrary.database.getWebPage
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.ArrayList

@AndroidEntryPoint
class LibraryFragment : BaseFragment(), GenericAdapter.Listener<Novel>, SimpleItemTouchListener, ActionMode.Callback {

    private val viewModel: LibraryViewModel by viewModels()
    
    private var _binding: ContentLibraryBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: GenericAdapter<Novel>
    private lateinit var touchHelper: ItemTouchHelper
    private lateinit var syncSnackBarManager: SnackProgressBarManager
    private var syncSnackBar: SnackProgressBar? = null
    
    private var novelSectionId: Long = -1L
    private var lastDeletedId: Long = -1
    private var isSorted = false
    private var actionMode: ActionMode? = null
    private var confirmDialog: MaterialDialog? = null

    companion object {
        private const val TAG = "LibraryFragment"
        private const val NOVEL_SECTION_ID = "novelSectionId"

        fun newInstance(novelSectionId: Long): LibraryFragment {
            val bundle = Bundle()
            bundle.putLong(NOVEL_SECTION_ID, novelSectionId)
            val fragment = LibraryFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        novelSectionId = arguments?.getLong(NOVEL_SECTION_ID) ?: -1L
    }

    override fun getLayoutId() = R.layout.content_library

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupSyncSnackBarManager()
        setupRecyclerView()
        observeViewModel()
        
        binding.progressLayout.showLoading()
        viewModel.loadNovels(novelSectionId)
    }

    private fun setupSyncSnackBarManager() {
        syncSnackBarManager = SnackProgressBarManager(requireView().parent as ViewGroup, this)
        syncSnackBarManager
            .setViewToMove(requireView().parent as ViewGroup)
            .setProgressBarColor(R.color.colorAccent)
            .setBackgroundColor(SnackProgressBarManager.BACKGROUND_COLOR_DEFAULT)
            .setTextSize(14f)
            .setMessageMaxLines(2)
            .setOverlayLayoutColor(R.color.colorDarkKnight)
            .setOverlayLayoutAlpha(0.8F)
    }

    private fun setupRecyclerView() {
        adapter = GenericAdapter(ArrayList(), R.layout.listitem_library, this)
        val callback = SimpleItemTouchHelperCallback(this, longPressDragEnabled = true, itemViewSwipeEnabled = false)
        touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(binding.recyclerView)
        binding.recyclerView.setDefaults(adapter)
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshNovels()
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LibraryUiState.Loading -> showLoading()
                is LibraryUiState.Success -> showContent(state)
                is LibraryUiState.Error -> showError(state.message)
            }
        }

        viewModel.syncProgress.observe(viewLifecycleOwner) { progress ->
            handleSyncProgress(progress)
        }

        lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    // Show error using existing extension method
                    activity?.showAlertDialog(message = it)
                }
            }
        }
    }

    private fun showLoading() {
        binding.progressLayout.showLoading()
        binding.swipeRefreshLayout.isRefreshing = false
    }

    private fun showContent(state: LibraryUiState.Success) {
        adapter.updateData(ArrayList(state.novels))
        binding.swipeRefreshLayout.isRefreshing = state.isRefreshing
        
        // Handle selection changes
        handleSelectionChange(state.selectedNovels)
        
        if (state.novels.isEmpty()) {
            binding.progressLayout.showEmpty(
                resId = R.raw.no_data_blob,
                isLottieAnimation = true,
                emptyText = "Your Library is empty!\nLet's start adding some from search screen…"
            )
        } else {
            binding.progressLayout.showContent()
        }
    }

    private fun showError(message: String) {
        binding.progressLayout.showError(
            resId = R.raw.no_data_blob,
            isLottieAnimation = true,
            errorText = message
        )
        binding.swipeRefreshLayout.isRefreshing = false
        activity?.showAlertDialog(message = message)
    }

    private fun handleSyncProgress(progress: SyncProgress) {
        when (progress) {
            is SyncProgress.Started -> {
                snackBarView(SnackBarStatus.Initialize, message = progress.message)
            }
            is SyncProgress.Progress -> {
                snackBarView(SnackBarStatus.Update, message = progress.message, progress = progress.current, maxProgress = progress.total)
            }
            is SyncProgress.Completed -> {
                snackBarView(SnackBarStatus.Dismiss)
            }
        }
    }

    private fun handleSelectionChange(selectedNovels: Set<Novel>) {
        if (selectedNovels.isNotEmpty() && actionMode == null) {
            actionMode = (activity as? AppCompatActivity)?.startSupportActionMode(this)
        } else if (selectedNovels.isEmpty() && actionMode != null) {
            actionMode?.finish()
        }
        
        actionMode?.title = selectedNovels.size.toString()
        adapter.notifyDataSetChanged()
    }

    //region Adapter Listener Methods - onItemClick(), viewBinder()

    override fun onItemClick(item: Novel, position: Int) {
        if (lastDeletedId != item.id) {
            val currentState = viewModel.uiState.value
            if (currentState is LibraryUiState.Success && currentState.selectedNovels.isNotEmpty()) {
                if (currentState.selectedNovels.contains(item)) {
                    viewModel.removeFromSelection(item)
                } else {
                    viewModel.addToSelection(item)
                }
            } else {
                // Navigate to chapters using Navigation Component
                navigateToChapters(item)
            }
        }
    }

    private fun navigateToChapters(novel: Novel) {
        try {
            val action = LibraryFragmentDirections.actionLibraryToChapters(novel.id)
            findNavController().navigate(action)
        } catch (e: Exception) {
            // Fallback to novel details if chapters navigation fails
            navigateToNovelDetails(novel)
        }
    }

    private fun navigateToNovelDetails(novel: Novel) {
        try {
            val action = LibraryFragmentDirections.actionLibraryToNovelDetails(novel.id)
            findNavController().navigate(action)
        } catch (e: Exception) {
            // Log error but don't crash
            activity?.showAlertDialog(message = "Navigation error: ${e.message}")
        }
    }

    override fun bind(item: Novel, itemView: View, position: Int) {
        val itemBinding = ListitemLibraryBinding.bind(itemView)
        itemBinding.novelImageView.setImageResource(android.R.color.transparent)

        if (!item.imageUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(item.imageUrl?.getGlideUrl())
                .apply(RequestOptions.circleCropTransform())
                .into(itemBinding.novelImageView)
        }

        itemBinding.novelTitleTextView.text = item.name
        itemBinding.novelTitleTextView.isSelected = dataCenter.enableScrollingText

        val lastRead = item.metadata[Constants.MetaDataKeys.LAST_READ_DATE] ?: "N/A"
        val lastUpdated = item.metadata[Constants.MetaDataKeys.LAST_UPDATED_DATE] ?: "N/A"

        itemBinding.lastOpenedDate.text = getString(R.string.last_read_n_updated, lastRead, lastUpdated)

        itemBinding.popMenu.setOnClickListener {
            val popup = PopupMenu(requireActivity(), it)
            popup.menuInflater.inflate(R.menu.menu_popup_novel, popup.menu)

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_novel_details -> {
                        if (lastDeletedId != item.id) {
                            navigateToNovelDetails(item)
                        }
                        true
                    }
                    R.id.action_novel_assign_novel_section -> {
                        showNovelSectionsList(arrayListOf(item))
                        true
                    }
                    R.id.action_reset_novel -> {
                        viewModel.resetNovel(item)
                        true
                    }
                    R.id.action_novel_remove -> {
                        onItemDismiss(position)
                        true
                    }
                    else -> {
                        true
                    }
                }
            }
            popup.show()
        }

//        itemView.reorderButton.setOnTouchListener { _, event ->
//            @Suppress("DEPRECATION")
//            if (MotionEventCompat.getActionMasked(event) ==
//                MotionEvent.ACTION_DOWN) {
//                touchHelper.startDrag(recyclerView.getChildViewHolder(itemView))
//            }
//            false
//        }

        itemBinding.readChapterImage.setOnClickListener {
            startReader(item)
        }

        val bookmarkOrderId = if (item.currentChapterUrl != null) {
            dbHelper.getWebPage(item.currentChapterUrl!!)?.orderId
        } else null

        val badgeCount = if (dataCenter.showChaptersLeftBadge) {
            if (bookmarkOrderId == null) item.chaptersCount
            else item.chaptersCount - bookmarkOrderId.toLong() - 1L
        } else item.newReleasesCount

        if (badgeCount != 0L) {
            val shape = GradientDrawable()
            shape.cornerRadius = 99f
            activity?.let {
                shape.setStroke(1, ContextCompat.getColor(it, R.color.Black))
                // Even if we want to see only chapters left - paint badge in red if there are new chapters
                // since last time novel was open.
                if (item.newReleasesCount == 0L)
                    shape.setColor(ContextCompat.getColor(it, R.color.Gray))
                else
                    shape.setColor(ContextCompat.getColor(it, R.color.DarkRed))
            }
            itemBinding.newChapterCount.background = shape
            itemBinding.newChapterCount.applyFont(activity?.assets).text = badgeCount.toString()
            itemBinding.newChapterCount.visibility = View.VISIBLE
        } else {
            itemBinding.newChapterCount.visibility = View.GONE
        }

        if (item.currentChapterUrl != null) {
            if (bookmarkOrderId != null) {
                val progress = "${bookmarkOrderId.plus(1)} / ${item.chaptersCount}"
                itemBinding.novelProgressText.text = progress
            }
        } else {
            itemBinding.novelProgressText.text = getString(R.string.no_bookmark)
        }

        itemView.setOnLongClickListener {
            val currentState = viewModel.uiState.value
            if (currentState is LibraryUiState.Success) {
                if (currentState.selectedNovels.contains(item)) {
                    viewModel.removeFromSelection(item)
                } else {
                    viewModel.addToSelection(item)
                }
            }
            true
        }

        itemBinding.checkbox.setOnCheckedChangeListener(null)
        val currentState = viewModel.uiState.value
        val selectedNovels = if (currentState is LibraryUiState.Success) currentState.selectedNovels else emptySet()
        
        if (selectedNovels.isNotEmpty()) {
            itemBinding.checkbox.visibility = View.VISIBLE
            itemBinding.popMenu.visibility = View.INVISIBLE
        } else {
            itemBinding.checkbox.visibility = View.INVISIBLE
            itemBinding.popMenu.visibility = View.VISIBLE
        }

        itemBinding.blackOverlay.visibility = if (selectedNovels.contains(item)) View.VISIBLE else View.INVISIBLE
        itemBinding.checkbox.isChecked = selectedNovels.contains(item)
        itemBinding.checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.addToSelection(item)
            } else {
                viewModel.removeFromSelection(item)
            }
        }
    }

    //endregion

    //region Sync Code
    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_library, menu)
        super.onCreateOptionsMenu(menu, menuInflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        if (activity != null)
            menu.getItem(0).isVisible = (syncSnackBar == null)
        super.onPrepareOptionsMenu(menu)
    }

    fun isSyncing(): Boolean = syncSnackBar != null

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sync -> {
                viewModel.syncNovels()
                return true
            }
            R.id.action_sort -> {
                sortNovelsAlphabetically()
            }
            R.id.action_search -> {
                try {
                    val action = LibraryFragmentDirections.actionLibraryToSearch()
                    findNavController().navigate(action)
                } catch (e: Exception) {
                    activity?.showAlertDialog(message = "Navigation error: ${e.message}")
                }
            }
            R.id.action_import_reading_list -> {
                // This would need to be implemented as a fragment or dialog
                activity?.showAlertDialog(message = "Import functionality will be implemented in a future update")
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun sortNovelsAlphabetically() {
        viewModel.sortNovelsAlphabetically(!isSorted)
        isSorted = !isSorted
    }



    //endregion

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshNovels()
    }

    override fun onPause() {
        super.onPause()
        val currentState = viewModel.uiState.value
        if (currentState is LibraryUiState.Success) {
            viewModel.updateOrderIds(currentState.novels)
        }
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNovelEvent(event: NovelEvent) {
        print(event.novelId)
    }

    private fun startReader(novel: Novel) {
        if (novel.currentChapterUrl != null) {
            viewModel.updateNewReleasesCount(novel.id, 0L)
            // Navigate to reader with novel ID and current chapter
            val currentChapter = dbHelper.getWebPage(novel.currentChapterUrl!!)
            if (currentChapter != null) {
                try {
                    val action = LibraryFragmentDirections.actionLibraryToReader(novel.id, currentChapter.orderId.toLong(), null)
                    findNavController().navigate(action)
                } catch (e: Exception) {
                    // Fallback to chapters if reader navigation fails
                    navigateToChapters(novel)
                }
            } else {
                navigateToChapters(novel)
            }
        } else {
            MaterialDialog(requireContext()).show {
                title(R.string.no_bookmark_found_dialog_title)
                message(R.string.no_bookmark_found_dialog_description, novel.name)
                positiveButton(R.string.okay) { dialog ->
                    navigateToChapters(novel)
                    dialog.dismiss()
                }
                negativeButton(R.string.cancel)
            }
        }
    }



    @SuppressLint("NotifyDataSetChanged")
    override fun onItemDismiss(viewHolderPosition: Int) {
        MaterialDialog(requireContext()).show {
            title(R.string.confirm_remove)
            message(R.string.confirm_remove_description_novel)
            positiveButton(R.string.remove) { dialog ->
                val novel = adapter.items[viewHolderPosition]
                viewModel.deleteNovel(novel)
                dialog.dismiss()
            }
            negativeButton(R.string.cancel) { dialog ->
                adapter.notifyDataSetChanged()
                dialog.dismiss()
            }
        }
    }

    override fun onItemMove(source: Int, target: Int) {
        adapter.onItemMove(source, target)
        viewModel.moveNovel(source, target)
    }

    @SuppressLint("CheckResult")
    private fun showNovelSectionsList(novels: ArrayList<Novel>) {
        val novelSections = viewModel.getNovelSections()
        if (novelSections.isEmpty()) {
            MaterialDialog(requireContext()).show {
                message(R.string.no_novel_sections_error)
            }
            return
        }
        
        val filteredSections = novelSections.filter { it.id != novelSectionId }
        val novelSectionsNames = ArrayList(filteredSections.map { it.name ?: "" })
        if (novelSectionId != -1L) {
            novelSectionsNames.add(0, getString(R.string.default_novel_section_name))
        }

        MaterialDialog(requireContext()).show {
            title(text = "Choose A Novel Section")
            listItems(items = novelSectionsNames.toList()) { _, which, _ ->
                val targetSectionId = if (this@LibraryFragment.novelSectionId == -1L) {
                    filteredSections[which].id
                } else if (which == 0) {
                    -1L
                } else {
                    filteredSections[which - 1].id
                }

                if (novels.size == 1) {
                    viewModel.assignNovelToSection(novels[0], targetSectionId)
                } else {
                    viewModel.assignNovelsToSection(novels, targetSectionId)
                }
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNovelSectionEvent(novelSectionEvent: NovelSectionEvent) {
        if (novelSectionEvent.novelSectionId == novelSectionId) {
            viewModel.refreshNovels()
        }
    }

    private fun confirmDialog(title: String, onConfirm: () -> Unit) {
        MaterialDialog(requireContext()).show {
            title(text = title)
            message(text = "Are you sure you want to proceed?")
            positiveButton(text = "Confirm") { dialog ->
                onConfirm()
                dialog.dismiss()
            }
            negativeButton(R.string.cancel) { dialog ->
                dialog.dismiss()
            }
        }
    }

    private enum class SnackBarStatus {
        Initialize, Update, Dismiss
    }

    private fun snackBarView(status: SnackBarStatus, message: String = "", progress: Int = 0, maxProgress: Int = 0) {
        when (status) {
            SnackBarStatus.Initialize -> {
                syncSnackBar = SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, message)
                    .setIsIndeterminate(false)
                    .setProgressMax(maxProgress)
                    .setShowProgressPercentage(true)
                syncSnackBarManager.show(syncSnackBar!!, SnackProgressBarManager.LENGTH_INDEFINITE)
                activity?.invalidateOptionsMenu()
            }
            SnackBarStatus.Update -> {
                syncSnackBar?.let { snackBar ->
                    syncSnackBarManager.updateTo(snackBar.setMessage(message))
                    syncSnackBarManager.setProgress(progress)
                }
            }
            SnackBarStatus.Dismiss -> {
                syncSnackBarManager.dismiss()
                syncSnackBar = null
                activity?.invalidateOptionsMenu()
            }
        }
    }

    //region - Action Mode for Library
    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode?.menuInflater?.inflate(R.menu.menu_library_action_mode, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        menu?.findItem(R.id.action_novel_assign_novel_section)?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        viewModel.clearSelection()
        actionMode = null
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        val currentState = viewModel.uiState.value
        val selectedNovels = if (currentState is LibraryUiState.Success) currentState.selectedNovels else emptySet()
        
        when (item?.itemId) {
            R.id.action_select_interval -> {
                viewModel.selectInterval()
            }
            R.id.action_select_all -> {
                viewModel.selectAll()
            }
            R.id.action_clear_selection -> {
                viewModel.clearSelection()
            }
            R.id.action_novel_remove -> {
                confirmDialog(getString(R.string.remove_novels)) {
                    viewModel.deleteNovels(selectedNovels.toList())
                }
            }
            R.id.action_reset_novel -> {
                confirmDialog(getString(R.string.reset_novel)) {
                    viewModel.resetNovels(selectedNovels.toList())
                }
            }
            R.id.action_novel_assign_novel_section -> {
                showNovelSectionsList(ArrayList(selectedNovels))
            }
        }
        return false
    }



    //endregion

    //region Dialogs
    private fun confirmDialog(content: String, positiveCallback: () -> Unit, negativeCallback: (() -> Unit)? = null) {
        if (confirmDialog == null || !confirmDialog!!.isShowing && activity != null) {
            MaterialDialog(requireActivity()).show {
                title(R.string.confirm_action)
                message(text = content)
                positiveButton(R.string.okay) { 
                    positiveCallback()
                    dismiss()
                }
                negativeButton(R.string.cancel) { 
                    negativeCallback?.invoke()
                    dismiss()
                }
                lifecycleOwner(this@LibraryFragment)
            }
        }
    }



    /**
     * Handy actionMode operations functions that wraps around updating the status for action being performed.
     */
    private fun withSnackBarStatus(novels: ArrayList<Novel>, action: String = "", shouldRefetchNovels: Boolean = false, operation: suspend (novel: Novel) -> Unit) {
        lifecycleScope.launch {
            snackBarView(SnackBarStatus.Initialize, maxProgress = novels.size)
            novels.forEachIndexed { index, novel ->
                val message = "$action: ${novel.name}\nChecking: ${index + 1}/${novels.size}"
                snackBarView(SnackBarStatus.Update, message = message, progress = index + 1)
                operation(novel)
            }
            snackBarView(SnackBarStatus.Dismiss)
            if (shouldRefetchNovels) refreshData()
        }
    }

    /**
     * Refresh the library data
     */
    private fun refreshData() {
        viewModel.refreshNovels()
    }

}

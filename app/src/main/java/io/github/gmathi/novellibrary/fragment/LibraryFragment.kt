package io.github.gmathi.novellibrary.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import com.afollestad.materialdialogs.DialogCallback
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItems
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.tingyik90.snackprogressbar.SnackProgressBar
import com.tingyik90.snackprogressbar.SnackProgressBarManager
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.NovelDetailsActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.databinding.ContentLibraryBinding
import io.github.gmathi.novellibrary.databinding.ListitemLibraryBinding
import io.github.gmathi.novellibrary.extensions.*
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.model.other.NovelEvent
import io.github.gmathi.novellibrary.model.other.NovelSectionEvent
import io.github.gmathi.novellibrary.network.sync.NovelSync
import io.github.gmathi.novellibrary.source.NovelUpdatesSource
import io.github.gmathi.novellibrary.util.*
import io.github.gmathi.novellibrary.util.lang.getGlideUrl
import io.github.gmathi.novellibrary.util.system.*
import io.github.gmathi.novellibrary.util.view.SimpleItemTouchHelperCallback
import io.github.gmathi.novellibrary.util.view.SimpleItemTouchListener
import io.github.gmathi.novellibrary.util.view.extensions.applyFont
import io.github.gmathi.novellibrary.util.view.setDefaults
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import kotlin.collections.ArrayList


class LibraryFragment : BaseFragment(), GenericAdapter.Listener<Novel>, SimpleItemTouchListener, ActionMode.Callback {

    lateinit var adapter: GenericAdapter<Novel>
    private lateinit var touchHelper: ItemTouchHelper

    private var novelSectionId: Long = -1L
    private var lastDeletedId: Long = -1
    private var isSorted = false

    private lateinit var syncSnackBarManager: SnackProgressBarManager
    private var syncSnackBar: SnackProgressBar? = null

    private lateinit var binding: ContentLibraryBinding

    var dataSet: HashSet<Novel> = HashSet()
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
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.content_library, container, false) ?: return null

        binding = ContentLibraryBinding.bind(view)

        syncSnackBarManager = SnackProgressBarManager(container!!, this)
        syncSnackBarManager
            .setViewToMove(container)
            .setProgressBarColor(R.color.colorAccent)
            .setBackgroundColor(SnackProgressBarManager.BACKGROUND_COLOR_DEFAULT)
            .setTextSize(14f)
            .setMessageMaxLines(2)
            .setOverlayLayoutColor(R.color.colorDarkKnight)
            .setOverlayLayoutAlpha(0.8F)

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        novelSectionId = requireArguments().getLong(NOVEL_SECTION_ID)

        setRecyclerView()
        binding.progressLayout.showLoading()
        setData()
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(ArrayList(), R.layout.listitem_library, this)
        val callback = SimpleItemTouchHelperCallback(this, longPressDragEnabled = true, itemViewSwipeEnabled = false)
        touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(binding.recyclerView)
        binding.recyclerView.setDefaults(adapter)
        binding.swipeRefreshLayout.setOnRefreshListener {
            setData()
        }
    }

    private fun setData() {
        updateOrderIds()
        adapter.updateData(ArrayList(dbHelper.getAllNovels(novelSectionId)))
        binding.swipeRefreshLayout.isRefreshing = false
        binding.progressLayout.showContent()
        if (adapter.items.size == 0) {
            binding.progressLayout.showEmpty(
                resId = R.raw.no_data_blob,
                isLottieAnimation = true,
                emptyText = "Your Library is empty!\nLet's start adding some from search screen…"
            )
        } else {
            binding.progressLayout.showContent()
        }
    }


    //region Adapter Listener Methods - onItemClick(), viewBinder()

    override fun onItemClick(item: Novel, position: Int) {
        if (lastDeletedId != item.id) {
            if (dataSet.isNotEmpty()) {
                if (dataSet.contains(item)) removeFromDataSet(item) else addToDataSet(item)
            } else
                (activity as? AppCompatActivity)?.startChaptersActivity(item)
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
                        if (lastDeletedId != item.id)
                            startNovelDetailsActivity(item)
                        true
                    }
                    R.id.action_novel_assign_novel_section -> {
                        showNovelSectionsList(listOf(item))
                        true
                    }
                    R.id.action_reset_novel -> {
                        if (networkHelper.isConnectedToNetwork()) {
                            val novel: Novel = adapter.items[position]
                            // We cannot block the main thread since we end up using Network methods later in dbHelper.resetNovel()
                            // Instead of using async{} which is deprecated, we can use GlobalScope.Launch {} which uses the Kotlin Coroutines
                            // We run resetNovel in GlobalScope, wait for it with .join() (which is why we need runBlocking{})
                            // then we syncNovels() so that it shows in Library
                            runBlocking {
                                GlobalScope.launch {
                                    try {
                                        dbHelper.resetNovel(novel)
                                    } catch (e: Exception) {
                                        Logs.error("LibraryFragment", "resetNovel: $novel", e)
                                    }
                                }.join()
                                setData()
                            }
                        } else {
                            showAlertDialog(message = "You need to be connected to Internet to Hard Reset.")
                        }
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
            else item.chaptersCount - bookmarkOrderId - 1
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
                val progress = "${bookmarkOrderId + 1} / ${item.chaptersCount}"
                itemBinding.novelProgressText.text = progress
            }
        } else {
            itemBinding.novelProgressText.text = getString(R.string.no_bookmark)
        }

        itemView.setOnLongClickListener {
            if (dataSet.contains(item)) removeFromDataSet(item) else addToDataSet(item)
            true
        }

        itemBinding.checkbox.setOnCheckedChangeListener(null)
        if (dataSet.isNotEmpty()) {
            itemBinding.checkbox.visibility = View.VISIBLE
            itemBinding.popMenu.visibility = View.INVISIBLE
        } else {
            itemBinding.checkbox.visibility = View.INVISIBLE
            itemBinding.popMenu.visibility = View.VISIBLE
        }

        itemBinding.blackOverlay.visibility = if (dataSet.contains(item)) View.VISIBLE else View.INVISIBLE
        itemBinding.checkbox.isChecked = dataSet.contains(item)
        itemBinding.checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked)
                addToDataSet(item)
            else
                removeFromDataSet(item)
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
                if (networkHelper.isConnectedToNetwork())
                    syncNovels()
                else {
                    showAlertDialog(message = getString(R.string.no_internet))
                }
                return true
            }
            R.id.action_sort -> {
                sortNovelsAlphabetically()
            }
            R.id.action_search -> {
                (activity as? AppCompatActivity)?.startLibrarySearchActivity()
            }
            R.id.action_import_reading_list -> {
                (activity as? AppCompatActivity)?.startImportLibraryActivity()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun sortNovelsAlphabetically() {
        if (adapter.items.isNotEmpty()) {
            val items = adapter.items
            if (!isSorted)
                adapter.updateData(ArrayList(items.sortedWith(compareBy { it.name })))
            else
                adapter.updateData(ArrayList(items.sortedWith(compareBy { it.name }).reversed()))
            isSorted = !isSorted
            updateOrderIds()
        }
    }

    private fun syncNovels(novel: Novel? = null) {
        //activity.startSyncService()
        lifecycleScope.launch(Dispatchers.IO) syncing@{

            if (activity == null) return@syncing
            snackBarView(SnackBarStatus.Initialize, message = getString(R.string.sync_in_progress) + " - " + getString(R.string.please_wait))

            var counter = 0
            val waitList = LinkedList<Deferred<Boolean>>()
            val totalChaptersMap: HashMap<Novel, ArrayList<WebPage>> = HashMap()
            val novels = if (novel == null) dbHelper.getAllNovels(novelSectionId) else listOf(novel)
            snackBarView(SnackBarStatus.MaxProgress, maxProgress = novels.count())

            novels.forEach {
                waitList.add(async {
                    try {

                        val newChaptersList = withContext(Dispatchers.IO) {
                            val source = sourceManager.get(it.sourceId)
                            if (source is NovelUpdatesSource)
                                source.getUnsortedChapterList(it)
                            else
                                source?.getChapterList(it)
                        } ?: ArrayList()
                        var currentChaptersHashCode = (it.metadata[Constants.MetaDataKeys.HASH_CODE] ?: "0").toInt()
                        if (currentChaptersHashCode == 0)
                            currentChaptersHashCode = dbHelper.getAllWebPages(it.id).sumOf { it.hashCode() }
                        val newChaptersHashCode = newChaptersList.sumOf { it.hashCode() }
                        if (newChaptersList.isNotEmpty() && newChaptersHashCode != currentChaptersHashCode) {
                            it.metadata[Constants.MetaDataKeys.HASH_CODE] = newChaptersHashCode.toString()
                            totalChaptersMap[it] = ArrayList(newChaptersList)
                        }

                        val message = getString(R.string.sync_done_fetching_chapters, it.name, (novels.count() - counter++))
                        snackBarView(SnackBarStatus.Update, message = message, progress = counter)

                    } catch (e: Exception) {
                        Logs.error(TAG, "Novel: $it", e)
                    }
                    true
                })
            }

            waitList.awaitAll()

            //Update DB with new chapters
            waitList.clear()
            counter = 0
            snackBarView(SnackBarStatus.MaxProgress, maxProgress = totalChaptersMap.count())

            totalChaptersMap.forEach {
                val novelToUpdate = it.key
                var chapters = it.value
                counter++

                val message = getString(R.string.sync_fetching_chapter_list, counter, totalChaptersMap.count(), novelToUpdate.name)
                snackBarView(SnackBarStatus.Update, message = message, progress = counter)

                // We re-fetch the chapters in-case of NovelUpdates so that we also retrieve the translator information.
                novelToUpdate.metadata[Constants.MetaDataKeys.LAST_UPDATED_DATE] = Utils.getCurrentFormattedDate()

                var newChaptersCount = chapters.size - novelToUpdate.chaptersCount
                if (newChaptersCount <= 0) { //Check if the chapters were deleted or updated.
                    newChaptersCount = 0
                }
                val newReleasesCount = novelToUpdate.newReleasesCount + newChaptersCount
                novelToUpdate.chaptersCount = chapters.size.toLong()
                novelToUpdate.newReleasesCount = newReleasesCount
                dbHelper.updateNovelMetaData(novelToUpdate)
                dbHelper.updateChaptersAndReleasesCount(novelToUpdate.id, chapters.size.toLong(), newReleasesCount)

                withContext(Dispatchers.Main) {
                    adapter.items.indexOfFirst { novel -> novel.id == novelToUpdate.id }.let { index ->
                        if (index != -1) adapter.updateItemAt(index, novelToUpdate)
                    }
                }

                try {
                    if (novelToUpdate.sourceId == Constants.SourceId.NOVEL_UPDATES) {
                        chapters = ArrayList(withContext(Dispatchers.IO) { sourceManager.get(novelToUpdate.sourceId)?.getChapterList(novelToUpdate) } ?: emptyList())
                    }
                    waitList.add(async {
                        dbHelper.writableDatabase.runTransaction { writableDatabase ->
                            //Don't auto delete chapters
                            //dbHelper.deleteWebPages(novelToUpdate.id, writableDatabase)
                            for (i in chapters.indices) {
                                dbHelper.createWebPage(chapters[i], writableDatabase)
                                dbHelper.createWebPageSettings(WebPageSettings(chapters[i].url, novelToUpdate.id), writableDatabase)
                            }
                        }
                        true
                    })

                } catch (e: Exception) {
                    Logs.error(TAG, "Novel: $it", e)
                    return@forEach
                }
            }

            waitList.awaitAll()
            snackBarView(SnackBarStatus.Dismiss)
        }
    }

    //endregion

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onResume() {
        super.onResume()
        setData()
    }

    override fun onPause() {
        super.onPause()
        updateOrderIds()
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
            dbHelper.updateNewReleasesCount(novel.id, 0L)
            (activity as? AppCompatActivity)?.startReaderDBPagerActivity(novel)
        } else {
            (activity as? AppCompatActivity)?.let {
                MaterialDialog(it).show {
                    title(R.string.no_bookmark_found_dialog_title)
                    message(R.string.no_bookmark_found_dialog_description, novel.name)
                    positiveButton(R.string.okay) { dialog ->
                        it.startChaptersActivity(
                            novel,
                            false
                        ); dialog.dismiss()
                    }
                    negativeButton(R.string.cancel)
                }
            }
        }
    }

    private fun startNovelDetailsActivity(novel: Novel) {
        val intent = Intent(activity, NovelDetailsActivity::class.java)
        val bundle = Bundle()
        bundle.putParcelable("novel", novel)
        intent.putExtras(bundle)
        activity?.startActivityForResult(intent, Constants.NOVEL_DETAILS_REQ_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Constants.NOVEL_DETAILS_RES_CODE) {
            val novelId = data?.extras?.getLong(Constants.NOVEL_ID)
            if (novelId != null) {
                lastDeletedId = novelId
                adapter.removeItemAt(adapter.items.indexOfFirst { it.id == novelId })
                Handler().postDelayed({ lastDeletedId = -1 }, 1200)
            }
            return
        } else if (requestCode == Constants.READER_ACT_REQ_CODE || requestCode == Constants.NOVEL_DETAILS_RES_CODE) {
            setData()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onItemDismiss(viewHolderPosition: Int) {
        activity?.let {
            MaterialDialog(it).show {
                title(R.string.confirm_remove)
                message(R.string.confirm_remove_description_novel)
                positiveButton(R.string.remove) { dialog ->
                    this@LibraryFragment.run {
                        val novel = adapter.items[viewHolderPosition]
                        Utils.deleteNovel(it, novel.id)
                        setData()
                        dialog.dismiss()
                    }
                }
                negativeButton(R.string.cancel) { dialog ->
                    this@LibraryFragment.run {
                        adapter.notifyDataSetChanged()
                        dialog.dismiss()
                    }
                }
            }
        }
    }

    override fun onItemMove(source: Int, target: Int) {
        adapter.onItemMove(source, target)
    }

    private fun updateOrderIds() {
        if (adapter.items.isNotEmpty())
            for (i in 0 until adapter.items.size) {
                dbHelper.updateNovelOrderId(adapter.items[i].id, i.toLong())
            }
    }

    private fun showNovelSectionsList(novels: List<Novel>) {
        val novelSections = ArrayList(dbHelper.getAllNovelSections())
        if (novelSections.isEmpty()) {
            MaterialDialog(requireActivity()).show {
                message(R.string.no_novel_sections_error)
            }
            return
        }
        novelSections.firstOrNull { it.id == novelSectionId }?.let { novelSections.remove(it) }
        val novelSectionsNames = ArrayList(novelSections.map { it.name ?: "" })
        if (novelSectionId != -1L)
            novelSectionsNames.add(0, getString(R.string.default_novel_section_name))

        MaterialDialog(requireActivity()).show {
            title(text = "Choose A Novel Section")
            listItems(items = novelSectionsNames.toList()) { _, which, _ ->
                var id = -1L
                if (novelSectionId == -1L)
                    id = novelSections[which].id
                else if (which != 0)
                    id = novelSections[which - 1].id

                withSnackBarStatus("Assigning") { novel ->
                    dbHelper.updateNovelSectionId(novel.id, id)
                    EventBus.getDefault().post(NovelSectionEvent(id))
                    NovelSync.getInstance(novel)?.applyAsync(lifecycleScope) { novelSync ->
                        if (dataCenter.getSyncAddNovels(novelSync.host)) {
                            novelSync.updateNovel(novel, novelSections.firstOrNull { section -> section.id == id })
                        }
                    }
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNovelSectionEvent(novelSectionEvent: NovelSectionEvent) {
        if (novelSectionEvent.novelSectionId == novelSectionId) {
            setData()
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
        dataSet.clear()
        actionMode = null
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        val novels = adapter.items
        when (item?.itemId) {
            R.id.action_select_interval -> {
                val firstIndex = novels.indexOf(dataSet.first())
                val lastIndex = novels.indexOf(dataSet.last())
                val subList = novels.subList(firstIndex, lastIndex)
                addToDataSet(subList)
            }
            R.id.action_select_all -> {
                addToDataSet(novels)
            }
            R.id.action_clear_selection -> {
                removeFromDataSet(novels)
            }
            R.id.action_novel_remove -> {
                confirmDialog(getString(R.string.remove_novels), {
                    withSnackBarStatus("Deleting") { novel ->
                        dbHelper.cleanupNovelData(novel)
                    }
                })
            }
            R.id.action_reset_novel -> {
                confirmDialog(getString(R.string.reset_novel), {
                    if (networkHelper.isConnectedToNetwork()) {
                        withSnackBarStatus("Resetting") { novel ->
                            dbHelper.resetNovel(novel)
                        }
                        setData()
                    } else {
                        showAlertDialog(message = "You need to be connected to Internet to Hard Reset.")
                        mode?.finish()
                    }
                })
            }
            R.id.action_novel_assign_novel_section -> {
                showNovelSectionsList(dataSet.toList())
            }
        }
        return false
    }

    private fun addToDataSet(novel: Novel) {
        dataSet.add(novel)
        if (actionMode == null) {
            actionMode = (activity as? AppCompatActivity)?.startSupportActionMode(this)
            adapter.notifyDataSetChanged()
        }
        actionMode?.title = dataSet.size.toString()
        adapter.notifyItemChanged(adapter.items.indexOf(novel))
    }

    private fun removeFromDataSet(novel: Novel) {
        dataSet.remove(novel)
        if (dataSet.isEmpty()) {
            actionMode?.finish()
            adapter.notifyDataSetChanged()
        } else {
            actionMode?.title = dataSet.size.toString()
            adapter.notifyItemChanged(adapter.items.indexOf(novel))
        }
    }

    private fun addToDataSet(novels: List<Novel>) {
        dataSet.addAll(novels)
        if (actionMode == null) {
            actionMode = (activity as? AppCompatActivity)?.startSupportActionMode(this)
        }
        actionMode?.title = dataSet.size.toString()
        adapter.notifyDataSetChanged()
    }

    private fun removeFromDataSet(novels: List<Novel>) {
        dataSet.removeAll(novels)
        if (dataSet.isEmpty()) {
            actionMode?.finish()
        } else {
            actionMode?.title = dataSet.size.toString()
        }
        adapter.notifyDataSetChanged()
    }

    //endregion

    //region Dialogs
    fun confirmDialog(content: String, positiveCallback: DialogCallback, negativeCallback: DialogCallback? = null) {
        if (confirmDialog == null || !confirmDialog!!.isShowing && activity != null) {
            MaterialDialog(requireActivity()).show {
                title(R.string.confirm_action)
                message(text = content)
                positiveButton(R.string.okay, click = positiveCallback)
                negativeButton(R.string.cancel, click = negativeCallback)

                lifecycleOwner(this@LibraryFragment)
            }
        }
    }

    /**
     * To Track SnackBar Status
     */
    enum class SnackBarStatus {
        Initialize,
        Update,
        Dismiss,
        MaxProgress
    }

    /**
     *  Consolidate the SnackBarManager & SnackBar interactions based on the operation being performed.
     */
    private suspend fun snackBarView(status: SnackBarStatus, message: String = "", maxProgress: Int = 1, progress: Int = 0) = withContext(Dispatchers.Main) {
        when (status) {
            SnackBarStatus.Initialize -> {
                if (this@LibraryFragment.syncSnackBar != null)
                    syncSnackBarManager.dismiss()
                this@LibraryFragment.syncSnackBar = SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, message)
                syncSnackBarManager.show(syncSnackBar!!, SnackProgressBarManager.LENGTH_INDEFINITE)
                actionMode?.finish()
                activity?.invalidateOptionsMenu()
            }

            SnackBarStatus.MaxProgress -> {
                syncSnackBar?.let { syncSnackBarManager.updateTo(it.setProgressMax(maxProgress)) }
            }

            SnackBarStatus.Update -> {
                syncSnackBar?.let {
                    syncSnackBarManager.updateTo(it.setMessage(message))
                    syncSnackBarManager.setProgress(progress)
                }
            }

            SnackBarStatus.Dismiss -> {
                syncSnackBarManager.dismiss()
                syncSnackBar = null
                actionMode?.finish()
                activity?.invalidateOptionsMenu()
                setData()
            }
        }
    }

    /**
     * Handy actionMode operations functions that wraps around updating the status for action being performed.
     */
    private fun withSnackBarStatus(action: String = "", operation: suspend (novel: Novel) -> Unit) {
        lifecycleScope.launch(Dispatchers.Main) {
            val novels = ArrayList(dataSet)
            snackBarView(SnackBarStatus.Initialize)
            snackBarView(SnackBarStatus.MaxProgress, maxProgress = novels.size)
            novels.forEachIndexed { index, novel ->
                val message = "$action: ${novel.name}\nChecking: $index/${novels.size}"
                snackBarView(SnackBarStatus.Update, message = message, progress = index + 1)
                withContext(Dispatchers.IO) { operation(novel) }
            }
            snackBarView(SnackBarStatus.Dismiss)
        }
    }

}

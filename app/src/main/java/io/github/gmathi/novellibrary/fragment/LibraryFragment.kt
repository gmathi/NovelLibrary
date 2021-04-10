package io.github.gmathi.novellibrary.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import com.afollestad.materialdialogs.MaterialDialog
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
import io.github.gmathi.novellibrary.util.*
import io.github.gmathi.novellibrary.util.system.*
import io.github.gmathi.novellibrary.util.view.SimpleItemTouchHelperCallback
import io.github.gmathi.novellibrary.util.view.SimpleItemTouchListener
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import kotlin.collections.ArrayList


class LibraryFragment : BaseFragment(), GenericAdapter.Listener<Novel>, SimpleItemTouchListener {

    lateinit var adapter: GenericAdapter<Novel>
    private lateinit var touchHelper: ItemTouchHelper

    private var novelSectionId: Long = -1L
    private var lastDeletedId: Long = -1
    private var isSorted = false

    private lateinit var syncSnackBarManager: SnackProgressBarManager
    private var syncSnackBar: SnackProgressBar? = null

    private lateinit var binding: ContentLibraryBinding

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
        }
    }


    //region Adapter Listener Methods - onItemClick(), viewBinder()

    override fun onItemClick(item: Novel, position: Int) {
        if (lastDeletedId != item.id)
            (activity as? AppCompatActivity)?.startChaptersActivity(item)
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
//                    R.id.action_novel_reorder -> {
//                        touchHelper.startDrag(recyclerView.getChildViewHolder(itemView))
//                        true
//                    }
                    R.id.action_novel_assign_novel_section -> {
                        showNovelSectionsList(position)
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
//                                GlobalScope.launch { dbHelper.resetNovel(novel) }.join()
//                                setData()
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
            else item.chaptersCount - bookmarkOrderId - 1;
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

            withContext(Dispatchers.Main) {
                if (this@LibraryFragment.syncSnackBar != null)
                    syncSnackBarManager.dismiss()
                this@LibraryFragment.syncSnackBar = SnackProgressBar(
                    SnackProgressBar.TYPE_HORIZONTAL,
                    getString(R.string.sync_in_progress) + " - " + getString(R.string.please_wait)
                )
                syncSnackBarManager.show(syncSnackBar!!, SnackProgressBarManager.LENGTH_INDEFINITE)
                activity?.invalidateOptionsMenu()
            }

            val totalCountMap: HashMap<Novel, Int> = HashMap()
            val totalChaptersMap: HashMap<Novel, ArrayList<WebPage>> = HashMap()
            val novels = if (novel == null) dbHelper.getAllNovels(novelSectionId) else listOf(novel)

            withContext(Dispatchers.Main) {
                syncSnackBarManager.updateTo(syncSnackBar!!.setProgressMax(novels.count()))
            }

            var counter = 0
            val waitList = LinkedList<Deferred<Boolean>>()
            novels.forEach {

                waitList.add(async {
                    try {

                        val totalChapters = withContext(Dispatchers.IO) { sourceManager.get(it.sourceId)?.getChapterList(it) } ?: ArrayList<WebPage>()
                        if (totalChapters.isNotEmpty() && totalChapters.size > it.chaptersCount.toInt()) {
                            totalCountMap[it] = totalChapters.size
                            totalChaptersMap[it] = ArrayList(totalChapters)
                        }

                        withContext(Dispatchers.Main) {
                            syncSnackBar?.let { snackBar ->
                                syncSnackBarManager.updateTo(snackBar.setMessage(getString(R.string.sync_done_fetching_chapters, it.name, (novels.count() - counter++))))
                                syncSnackBarManager.setProgress(counter)
                            }
                        }


                    } catch (e: Exception) {
                        Logs.error(TAG, "Novel: $it", e)
                    } finally {
                    }
                    true
                })
            }

            waitList.awaitAll()

            //Update DB with new chapters
            waitList.clear()
            counter = 0
            withContext(Dispatchers.Main) {
                syncSnackBarManager.updateTo(syncSnackBarManager.getLastShown()?.setProgressMax(totalChaptersMap.count())!!)
            }

            totalChaptersMap.forEach {
                val novelToUpdate = it.key
                val chapters = it.value

                counter++
                withContext(Dispatchers.Main) {
                    syncSnackBar?.let { progressBar ->
                        syncSnackBarManager.updateTo(
                            progressBar.setMessage(
                                getString(
                                    R.string.sync_fetching_chapter_list,
                                    counter, totalChaptersMap.count(), novelToUpdate.name
                                )
                            )
                        )
                        syncSnackBarManager.setProgress(counter)
                    }
                }

                novelToUpdate.metadata[Constants.MetaDataKeys.LAST_UPDATED_DATE] = Utils.getCurrentFormattedDate()
                novelToUpdate.newReleasesCount += (chapters.size - novelToUpdate.chaptersCount)
                novelToUpdate.chaptersCount = chapters.size.toLong()
                dbHelper.updateNovelMetaData(novelToUpdate)
                dbHelper.updateChaptersAndReleasesCount(novelToUpdate.id, chapters.size.toLong(), novelToUpdate.newReleasesCount + (chapters.size - novelToUpdate.chaptersCount))

                withContext(Dispatchers.Main) {
                    adapter.items.indexOfFirst { novel ->
                        novel.id == novelToUpdate.id
                    }.let { index ->
                        if (index != -1)
                            adapter.updateItemAt(index, novelToUpdate)
                    }
                }

                try {
                    waitList.add(async {
                        for (i in 0 until chapters.size) {
                            dbHelper.writableDatabase.runTransaction { writableDatabase ->
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

            withContext(Dispatchers.Main) {
                syncSnackBarManager.dismiss()
                syncSnackBar = null
            }

            activity?.invalidateOptionsMenu()
        }
    }

    //endregion

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onResume() {
        super.onResume()
        adapter.updateData(ArrayList(dbHelper.getAllNovels(novelSectionId)))
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
                        adapter.onItemDismiss(viewHolderPosition)
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

    private fun showNovelSectionsList(position: Int) {
        val novelSections = ArrayList(dbHelper.getAllNovelSections())
        if (novelSections.isEmpty()) {
            MaterialDialog(requireActivity()).show {
                message(R.string.no_novel_sections_error)
            }
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

                val novel = adapter.items[position]
                dbHelper.updateNovelSectionId(novel.id, id)
                EventBus.getDefault().post(NovelSectionEvent(id))
                NovelSync.getInstance(novel)?.applyAsync(lifecycleScope) {
                    if (dataCenter.getSyncAddNovels(it.host)) it.updateNovel(
                        novel,
                        novelSections.firstOrNull { section -> section.id == id })
                }
                setData()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNovelSectionEvent(novelSectionEvent: NovelSectionEvent) {
        if (novelSectionEvent.novelSectionId == novelSectionId) {
            setData()
        }
    }

}

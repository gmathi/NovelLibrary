package io.github.gmathi.novellibrary.fragment

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
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.databinding.ContentLibraryBinding
import io.github.gmathi.novellibrary.databinding.ListitemLibraryBinding
import io.github.gmathi.novellibrary.db
import io.github.gmathi.novellibrary.extensions.*
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.model.other.NovelEvent
import io.github.gmathi.novellibrary.model.other.NovelSectionEvent
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.network.getChapterCount
import io.github.gmathi.novellibrary.network.getChapterUrls
import io.github.gmathi.novellibrary.network.sync.NovelSync
import io.github.gmathi.novellibrary.util.*
import io.github.gmathi.novellibrary.util.lang.launchIO
import io.github.gmathi.novellibrary.util.lang.launchUI
import io.github.gmathi.novellibrary.util.system.*
import io.github.gmathi.novellibrary.util.view.SimpleItemTouchHelperCallback
import io.github.gmathi.novellibrary.util.view.SimpleItemTouchListener
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class LibraryFragment : BaseFragment(), GenericAdapter.Listener<Novel>, SimpleItemTouchListener {

    lateinit var adapter: GenericAdapter<Novel>
    private lateinit var touchHelper: ItemTouchHelper

    private var novelSectionId: Long = -1L
    private var lastDeletedId: Long = -1
    private var isSorted = false

    private lateinit var syncSnackbarManager: SnackProgressBarManager
    private var syncSnackbar: SnackProgressBar? = null

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

        syncSnackbarManager = SnackProgressBarManager(container!!, this)
        syncSnackbarManager
                .setViewToMove(container!!)
                .setProgressBarColor(R.color.colorAccent)
                .setBackgroundColor(SnackProgressBarManager.BACKGROUND_COLOR_DEFAULT)
                .setTextSize(14f)
                .setMessageMaxLines(2)

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        novelSectionId = requireArguments().getLong(NOVEL_SECTION_ID)

        setRecyclerView()
        binding.progressLayout.showLoading()
        setData(true)
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(ArrayList(), R.layout.listitem_library, this)
        val callback = SimpleItemTouchHelperCallback(this, longPressDragEnabled = true, itemViewSwipeEnabled = false)
        touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(binding.recyclerView)
        binding.recyclerView.setDefaults(adapter)
        binding.swipeRefreshLayout.setOnRefreshListener {
            setData(true)
        }
    }

    private fun setData(forceUpdate: Boolean = false) {
        updateOrderIds()
        if (db.novelDao().countByNovelSection(novelSectionId) != adapter.items.size || forceUpdate) {
            // Update from database
            lifecycleScope.launch {
                adapter.updateData(withContext(Dispatchers.IO) { ArrayList(db.novelDao().findByNovelSection(novelSectionId)) })
                if (binding.swipeRefreshLayout != null && binding.progressLayout != null) {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.progressLayout.showContent()
                }
                if (adapter.items.size == 0) {
                    binding.progressLayout.showEmpty(
                        resId = R.raw.no_data_blob,
                        isLottieAnimation = true,
                        emptyText = "Your Library is empty!\nLet's start adding some from search screen…"
                    )
                }
            }
        }
        else {
            adapter.updateData(adapter.items)
        }
    }

    //region Adapter Listener Methods - onItemClick(), viewBinder()

    override fun onItemClick(item: Novel, position: Int) {
        if (lastDeletedId != item.id)
            (activity as? AppCompatActivity)?.startChaptersActivity(item)
    }

    override fun bind(item: Novel, itemView: View, position: Int) {
        lifecycleScope.launch {
            val itemBinding = ListitemLibraryBinding.bind(itemView)
            itemBinding.novelImageView.setImageResource(android.R.color.transparent)

            if (!item.imageUrl.isNullOrBlank()) {
                async(Dispatchers.IO) {
                    val glide = Glide.with(this@LibraryFragment)
                            .load(item.imageUrl?.getGlideUrl())
                            .apply(RequestOptions.circleCropTransform())
                    withContext (Dispatchers.Main) {
                        glide.into(itemBinding.novelImageView)
                    }
                }
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
                                startNovelDetailsActivity(item, false)
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
                            if (Utils.isConnectedToNetwork(context)) {
                                val novel: Novel = adapter.items[position]
                                // We cannot block the main thread since we end up using Network methods later in dbHelper.resetNovel()
                                // Instead of using async{} which is deprecated, we can use GlobalScope.Launch {} which uses the Kotlin Coroutines
                                // We run resetNovel in GlobalScope, wait for it with .join() (which is why we need runBlocking{})
                                // then we syncNovels() so that it shows in Library
                                runBlocking {
                                    GlobalScope.launch { db.resetNovel(novel) }.join()
                                    db.novelDao().findOneById(novel.id)?.let { novel ->
                                        adapter.items[position] = novel
                                        adapter.updateItem(novel)
                                    }
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


            async {
                if (item.newReleasesCount != 0L) {
                    val shape = GradientDrawable()
                    shape.cornerRadius = 99f
                    activity?.let { ContextCompat.getColor(it, R.color.Black) }?.let { shape.setStroke(1, it) }
                    activity?.let { ContextCompat.getColor(it, R.color.DarkRed) }?.let { shape.setColor(it) }
                    itemBinding.newChapterCount.background = shape
                    itemBinding.newChapterCount.applyFont(activity?.assets).text = item.newReleasesCount.toString()
                    itemBinding.newChapterCount.visibility = View.VISIBLE
                } else {
                    itemBinding.newChapterCount.visibility = View.GONE
                }
            }

            if (item.currentChapterUrl != null) {
                val orderId = db.webPageDao().findOneByUrl(item.currentChapterUrl!!)?.orderId
                if (orderId != null) {
                    val progress = "${orderId + 1} / ${item.chaptersCount}"
                    itemBinding.novelProgressText.text = progress
                }
                else {
                    itemBinding.novelProgressText.text = ""
                }
            } else {
                itemBinding.novelProgressText.text = getString(R.string.no_bookmark)
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
            menu.getItem(0).isVisible = (syncSnackbar == null)
        super.onPrepareOptionsMenu(menu)
    }

    fun isSyncing(): Boolean = syncSnackbar != null

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sync -> {
                activity?.let {
                    if (Utils.isConnectedToNetwork(it))
                        syncNovels()
                    else {
                        showAlertDialog(message = getString(R.string.no_internet))
                    }
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
        lifecycleScope.launch syncing@{

            if (activity == null) return@syncing

            if (this@LibraryFragment.syncSnackbar != null)
                syncSnackbarManager.dismiss()

            withContext(Dispatchers.Main) {
                this@LibraryFragment.syncSnackbar = SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL,
                        getString(R.string.sync_in_progress) + " - " + getString(R.string.please_wait))
                syncSnackbarManager.show(syncSnackbar!!, SnackProgressBarManager.LENGTH_INDEFINITE)
            }

            activity?.invalidateOptionsMenu()

            val totalCountMap: HashMap<Novel, Int> = HashMap()

            val novels = if (novel == null) db.novelDao().findByNovelSection(novelSectionId) else listOf(novel)

            syncSnackbarManager.updateTo(syncSnackbar!!.setProgressMax(novels.count()))

            var counter = 0
            novels.forEach {
                try {
                    async(Dispatchers.Main) {
                        syncSnackbar?.setMessage(
                                getString(
                                        R.string.sync_fetching_chapter_counts,
                                        counter++,
                                        novels.count(),
                                        it.name
                                )
                        )?.let { snackbar ->
                            syncSnackbarManager.updateTo(snackbar)
                            syncSnackbarManager.setProgress(counter)
                        }
                    }

                    val totalChapters = withContext(Dispatchers.IO) { NovelApi.getChapterCount(it) }
                    if (totalChapters != 0 && totalChapters > it.chaptersCount.toInt()) {
                        totalCountMap[it] = totalChapters
                    }
                } catch (e: Exception) {
                    Logs.error(TAG, "Novel: $it", e)
                    return@forEach
                }
            }

            //Update DB with new chapters
            counter = 0
            syncSnackbarManager.updateTo(syncSnackbarManager.getLastShown()?.setProgressMax(totalCountMap.count())!!)
            totalCountMap.forEach {
                val updatedNovel = it.key
                counter++
                async (Dispatchers.Main) {
                    syncSnackbarManager.updateTo(
                        syncSnackbar!!.setMessage(
                            getString(
                                R.string.sync_fetching_chapter_list,
                                counter,
                                totalCountMap.count(),
                                updatedNovel.name
                            )
                        )
                    )
                    syncSnackbarManager.setProgress(counter)
                }
                updatedNovel.metadata[Constants.MetaDataKeys.LAST_UPDATED_DATE] = Utils.getCurrentFormattedDate()
                updatedNovel.chaptersCount = it.value.toLong()
                updatedNovel.newReleasesCount += (it.value - updatedNovel.chaptersCount)
                db.novelDao().update(updatedNovel)

                try {
                    //TODO: Handle Empty State
                    val chapters = withContext(Dispatchers.IO) { NovelApi.getChapterUrls(updatedNovel) } ?: ArrayList()
                    for (i in 0 until chapters.size) {
                        if (db.webPageDao().findOneByUrl(chapters[i].url) == null)
                            db.webPageDao().insert(chapters[i])
                        if (db.webPageSettingsDao().findOneByUrl(chapters[i].url) == null)
                            db.webPageSettingsDao().insert(WebPageSettings(chapters[i].url, updatedNovel.id))
                    }

                } catch (e: Exception) {
                    Logs.error(TAG, "Novel: $it", e)
                    return@forEach
                }
            }

            setData(true)

            async (Dispatchers.Main) {
                syncSnackbarManager.dismiss()
                syncSnackbar = null
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
        launchIO {
            val novels = ArrayList(db.novelDao().findByNovelSection(novelSectionId))
            launchUI {
                adapter.updateData(novels)
            }
        }
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
            novel.newReleasesCount = 0L
            db.novelDao().update(novel)
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

    private fun startNovelDetailsActivity(novel: Novel, jumpToReader: Boolean) {
        val intent = Intent(activity, NovelDetailsActivity::class.java)
        val bundle = Bundle()
        bundle.putSerializable("novel", novel)
        if (jumpToReader)
            bundle.putBoolean(Constants.JUMP, true)
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
            setData(true)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onItemDismiss(viewHolderPosition: Int) {
        activity?.let {
            MaterialDialog(it).show {
                title(R.string.confirm_remove)
                message(R.string.confirm_remove_description_novel)
                positiveButton(R.string.remove) { dialog ->
                    this@LibraryFragment.run {
                        val novel = adapter.items[viewHolderPosition]
                        launchIO {
                            Utils.deleteNovel(it, novel.id)
                        }
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
        if (adapter.items.isNotEmpty()) {
            for (i in 0 until adapter.items.size) {
                val novel = adapter.items[i]
                novel.orderId = i.toLong()
            }

            launchIO {
                db.novelDao().updateAll(adapter.items)
            }
        }
    }

    private fun showNovelSectionsList(position: Int) {
        val novelSections = ArrayList(db.novelSectionDao().getAll())
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
                if (novel.novelSectionId == id) return@listItems // Should not happen

                novel.novelSectionId = id
                db.novelDao().update(novel)
                EventBus.getDefault().post(NovelSectionEvent(id))
                NovelSync.getInstance(novel)?.applyAsync(lifecycleScope) {
                    if (dataCenter.getSyncAddNovels(it.host)) it.updateNovel(
                        novel,
                        novelSections.firstOrNull { section -> section.id == id })
                }
                adapter.removeItemAt(position)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNovelSectionEvent(novelSectionEvent: NovelSectionEvent) {
        if (novelSectionEvent.novelSectionId == novelSectionId) {
            setData(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}

package io.github.gmathi.novellibrary.fragment

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.ChaptersPagerActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapterSelectTitleProvider
import io.github.gmathi.novellibrary.databinding.FragmentSourceChaptersBinding
import io.github.gmathi.novellibrary.databinding.ListitemChapterBinding
import io.github.gmathi.novellibrary.extensions.*
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.model.other.ChapterActionModeEvent
import io.github.gmathi.novellibrary.model.other.EventType
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Constants.ALL_TRANSLATOR_SOURCES
import io.github.gmathi.novellibrary.util.system.startReaderDBPagerActivity
import io.github.gmathi.novellibrary.util.system.startWebViewActivity
import io.github.gmathi.novellibrary.util.system.updateNovelBookmark
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.view.setDefaultsNoAnimation
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class ChaptersFragment : BaseFragment(),
    GenericAdapterSelectTitleProvider.Listener<WebPage>, CompoundButton.OnCheckedChangeListener {

    companion object {

        private const val NOVEL = "novel"
        private const val TRANSLATOR_SOURCE_NAME = "translatorSourceName"

        fun newInstance(novel: Novel, translatorSourceName: String): ChaptersFragment {
            val bundle = Bundle()
            bundle.putParcelable(NOVEL, novel)
            bundle.putString(TRANSLATOR_SOURCE_NAME, translatorSourceName)
            val fragment = ChaptersFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    lateinit var novel: Novel
    private lateinit var translatorSourceName: String
    lateinit var adapter: GenericAdapterSelectTitleProvider<WebPage>

    private var lastKnownRecyclerState: Parcelable? = null

    private lateinit var binding: FragmentSourceChaptersBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val result = inflater.inflate(R.layout.fragment_source_chapters, container, false)
            ?: return null
        binding = FragmentSourceChaptersBinding.bind(result)
        return result
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        novel = requireArguments().getParcelable<Novel>(NOVEL) as Novel
        translatorSourceName = requireArguments().getString(TRANSLATOR_SOURCE_NAME)
            ?: ALL_TRANSLATOR_SOURCES
        binding.progressLayout.showLoading()
        setRecyclerView()
        setData()
    }

    private fun setRecyclerView() {
        adapter = GenericAdapterSelectTitleProvider(items = ArrayList(), layoutResId = R.layout.listitem_chapter, listener = this)
        binding.recyclerView.isVerticalScrollBarEnabled = true
        binding.recyclerView.setDefaultsNoAnimation(adapter)
        this.context?.let { binding.recyclerView.addItemDecoration(CustomDividerItemDecoration(it, DividerItemDecoration.VERTICAL)) }
        binding.swipeRefreshLayout.isEnabled = false
        attachSwipeActions()
    }

    /**
     * Swipe-to-act on chapter rows (issues #199, #200):
     *  - Swipe RIGHT (start -> end): toggle this chapter's read/unread state.
     *  - Swipe LEFT  (end -> start): toggle the novel bookmark on this chapter.
     * The swiped row is never dismissed; it snaps back and rebinds to show the new state.
     * Disabled while a selection action-mode is active to avoid conflicting with multi-select.
     */
    private fun attachSwipeActions() {
        val ctx = context ?: return
        val readBg = ColorDrawable(ContextCompat.getColor(ctx, R.color.DarkGreen))
        val bookmarkBg = ColorDrawable(ContextCompat.getColor(ctx, R.color.Orange))
        val readIcon = ContextCompat.getDrawable(ctx, R.drawable.ic_check_circle_white_vector)
        val bookmarkIcon = ContextCompat.getDrawable(ctx, R.drawable.ic_bookmark_orange_vector)

        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun getSwipeDirs(rv: RecyclerView, vh: RecyclerView.ViewHolder): Int {
                // Don't allow swipe while the novel isn't in the library (no persisted state)
                // or while a multi-select action mode is active.
                if (novel.id == -1L) return 0
                if ((activity as? ChaptersPagerActivity)?.dataSet?.isNotEmpty() == true) return 0
                return super.getSwipeDirs(rv, vh)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position == RecyclerView.NO_POSITION) return
                when (direction) {
                    ItemTouchHelper.RIGHT -> toggleReadStatus(position)
                    ItemTouchHelper.LEFT -> toggleBookmark(position)
                }
            }

            override fun onChildDraw(
                c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = vh.itemView
                val iconMargin = (itemView.height - (readIcon?.intrinsicHeight ?: 0)) / 2
                when {
                    dX > 0 -> { // swipe right -> read toggle
                        readBg.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
                        readBg.draw(c)
                        readIcon?.let {
                            val top = itemView.top + (itemView.height - it.intrinsicHeight) / 2
                            it.setBounds(itemView.left + iconMargin, top, itemView.left + iconMargin + it.intrinsicWidth, top + it.intrinsicHeight)
                            it.draw(c)
                        }
                    }
                    dX < 0 -> { // swipe left -> bookmark toggle
                        bookmarkBg.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                        bookmarkBg.draw(c)
                        bookmarkIcon?.let {
                            val top = itemView.top + (itemView.height - it.intrinsicHeight) / 2
                            it.setBounds(itemView.right - iconMargin - it.intrinsicWidth, top, itemView.right - iconMargin, top + it.intrinsicHeight)
                            it.draw(c)
                        }
                    }
                }
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.recyclerView)
    }

    /** Toggle read/unread for the chapter at [position] and rebind. (#199) */
    private fun toggleReadStatus(position: Int) {
        val webPage = adapter.items.getOrNull(position) ?: return
        val activity = (activity as? ChaptersPagerActivity) ?: return
        val currentlyRead = activity.vm.chapterSettings?.firstOrNull { it.url == webPage.url }?.isRead ?: return
        val newRead = !currentlyRead
        activity.vm.toggleChapterReadStatus(webPage, newRead) {
            adapter.notifyItemChanged(adapter.items.indexOf(webPage))
        }
        showSwipeSnackbar(
            if (newRead) R.string.chapter_marked_read else R.string.chapter_marked_unread
        ) {
            activity.vm.toggleChapterReadStatus(webPage, currentlyRead) {
                adapter.notifyItemChanged(adapter.items.indexOf(webPage))
            }
        }
    }

    /** Toggle the novel bookmark on the chapter at [position] and rebind all tabs. (#200) */
    private fun toggleBookmark(position: Int) {
        val webPage = adapter.items.getOrNull(position) ?: return
        val activity = (activity as? ChaptersPagerActivity) ?: return
        val previousBookmark = novel.currentChapterUrl
        val wasBookmarked = previousBookmark == webPage.url
        val newBookmark = if (wasBookmarked) null else webPage.url
        activity.vm.setBookmark(newBookmark) {
            novel.currentChapterUrl = newBookmark
            // Rebind every source tab so the moved bookmark marker updates everywhere.
            EventBus.getDefault().post(ChapterActionModeEvent(eventType = EventType.COMPLETE))
        }
        showSwipeSnackbar(
            if (wasBookmarked) R.string.chapter_bookmark_cleared else R.string.chapter_bookmark_set
        ) {
            activity.vm.setBookmark(previousBookmark) {
                novel.currentChapterUrl = previousBookmark
                EventBus.getDefault().post(ChapterActionModeEvent(eventType = EventType.COMPLETE))
            }
        }
    }

    private fun showSwipeSnackbar(messageRes: Int, undoAction: () -> Unit) {
        Snackbar.make(binding.recyclerView, messageRes, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo) { undoAction() }
            .show()
    }

    private fun setData(shouldScrollToBookmark: Boolean = true, shouldScrollToFirstUnread: Boolean = true) {
        (activity as? ChaptersPagerActivity)?.let { activity ->

            val chapters = (
                    if (translatorSourceName == ALL_TRANSLATOR_SOURCES)
                        activity.vm.chapters
                    else
                        activity.vm.chapters?.filter { it.translatorSourceName == translatorSourceName }
                    ) ?: ArrayList()

            if (chapters.isNotEmpty()) {
                adapter.updateData(if (novel.metadata["chapterOrder"] == "des") ArrayList(chapters.reversed()) else ArrayList(chapters))
                binding.progressLayout.showContent()
                if (shouldScrollToBookmark)
                    scrollToBookmark()
                else if (shouldScrollToFirstUnread)
                    scrollToFirstUnread(
                        activity.vm.chapterSettings
                            ?: throw Error("Invalid Chapter Settings")
                    )
                if (activity.dataSet.isNotEmpty()) {
                    lastKnownRecyclerState?.let { binding.recyclerView.layoutManager?.onRestoreInstanceState(it) }
                }
            } else
                binding.progressLayout.showEmpty(emptyText = "No Chapters Found!")
        }
    }

    private fun scrollToBookmark() {
        if (novel.currentChapterUrl != null) {
            val index = adapter.items.indexOfFirst { it.url == novel.currentChapterUrl }
            if (index != -1)
                binding.recyclerView.scrollToPosition(index)
        }
    }

    private fun scrollToFirstUnread(chaptersSettings: ArrayList<WebPageSettings>) {
        if (novel.currentChapterUrl != null) {
            val index = if (novel.metadata["chapterOrder"] == "des")
                adapter.items.indexOfLast { chapter -> chaptersSettings.firstOrNull { it.url == chapter.url && !it.isRead } != null }
            else
                adapter.items.indexOfFirst { chapter -> chaptersSettings.firstOrNull { it.url == chapter.url && !it.isRead } != null }
            if (index != -1)
                binding.recyclerView.scrollToPosition(index)
        }
    }

    //region Adapter Listener Methods - onItemClick(), viewBinder()

    override fun getSectionTitle(position: Int): String {
        return adapter.items[position].chapterName
    }

    override fun onItemClick(item: WebPage, position: Int) {
        if (novel.id != -1L) {
            updateNovelBookmark(novel, item)
            startReaderDBPagerActivity(novel, translatorSourceName)
        } else
            startWebViewActivity(item.url)
    }

    @SuppressLint("SetTextI18n")
    override fun bind(item: WebPage, itemView: View, position: Int) {
        val itemBinding = ListitemChapterBinding.bind(itemView)

        val webPageSettings = (activity as? ChaptersPagerActivity)?.vm?.chapterSettings?.firstOrNull { it.url == item.url }

        if (webPageSettings?.filePath != null) {
            itemBinding.availableOfflineImageView.visibility = View.VISIBLE
            itemBinding.availableOfflineImageView.animation = null
        } else {
            itemBinding.availableOfflineImageView.visibility = View.GONE
        }

        itemBinding.isReadView.visibility = if (webPageSettings?.isRead == true) View.VISIBLE else View.GONE
        itemBinding.bookmarkView.visibility = if (item.url == novel.currentChapterUrl) View.VISIBLE else View.GONE

        itemBinding.chapterTitle.text = item.chapterName

        webPageSettings?.title?.let {
            if (it.contains(item.chapterName))
                itemBinding.chapterTitle.text = it
            else
                itemBinding.chapterTitle.text = "${item.chapterName}: $it"
        }
        itemBinding.chapterCheckBox.setOnCheckedChangeListener(null)
        itemBinding.chapterCheckBox.isChecked = (activity as? ChaptersPagerActivity)?.dataSet?.any { it.orderId == item.orderId }
            ?: false
        itemBinding.chapterCheckBox.tag = item
        itemBinding.chapterCheckBox.setOnCheckedChangeListener(this@ChaptersFragment)


        itemView.setOnLongClickListener {
            itemBinding.chapterCheckBox.isChecked = true
            true
        }

        if (webPageSettings?.metadata?.get(Constants.MetaDataKeys.IS_FAVORITE)?.toBoolean() == true) {
            itemBinding.favoriteView.visibility = View.VISIBLE
        } else {
            itemBinding.favoriteView.visibility = View.GONE
        }
    }


    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        val webPage = (buttonView.tag as WebPage?) ?: return
        val chaptersPagerActivity = (activity as? ChaptersPagerActivity) ?: return

        // If Novel is not in Library
        if (novel.id == -1L) {
            if (isChecked)
                chaptersPagerActivity.confirmDialog(getString(R.string.add_to_library_dialog_content, novel.name), { dialog ->
                    chaptersPagerActivity.vm.addNovelToLibrary()
                    chaptersPagerActivity.invalidateOptionsMenu()
                    chaptersPagerActivity.addToDataSet(webPage)
                    dialog.dismiss()
                }, { dialog ->
                    chaptersPagerActivity.removeFromDataSet(webPage)
                    adapter.notifyItemChanged(adapter.items.indexOf(webPage))
                    dialog.dismiss()
                })
        }

        //If Novel is already in library
        else {
            if (isChecked)
                chaptersPagerActivity.addToDataSet(webPage)
            else
                chaptersPagerActivity.removeFromDataSet(webPage)
        }
    }

    //endregion

    //region Event Bus

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        EventBus.getDefault().unregister(this)
        super.onPause()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onChapterActionModeEvent(chapterActionModeEvent: ChapterActionModeEvent) {
        if (chapterActionModeEvent.eventType == EventType.COMPLETE || (chapterActionModeEvent.eventType == EventType.UPDATE && (chapterActionModeEvent.translatorSourceName == translatorSourceName || translatorSourceName == ALL_TRANSLATOR_SOURCES))) {
            // Pick up a bookmark change made on another source tab (swipe #200), since each
            // tab holds its own Novel copy while the VM holds the source of truth.
            (activity as? ChaptersPagerActivity)?.vm?.novel?.let {
                novel.currentChapterUrl = it.currentChapterUrl
            }
            lastKnownRecyclerState = binding.recyclerView.layoutManager?.onSaveInstanceState()
            setData()
            return
        }
        if (chapterActionModeEvent.eventType == EventType.DOWNLOAD) {
            adapter.items.firstOrNull { it.url == chapterActionModeEvent.url }?.let { adapter.updateItem(it) }
        }
    }

    //endregion
}
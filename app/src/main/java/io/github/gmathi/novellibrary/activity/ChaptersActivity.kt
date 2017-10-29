package io.github.gmathi.novellibrary.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.view.ActionMode
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import co.metalab.asyncawait.async
import com.afollestad.materialdialogs.MaterialDialog
import com.github.johnpersano.supertoasts.library.Style
import com.github.johnpersano.supertoasts.library.SuperActivityToast
import com.github.johnpersano.supertoasts.library.utils.PaletteUtils
import com.hanks.library.AnimateCheckBox
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapterWithDragListener
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.model.EventType
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.NovelEvent
import io.github.gmathi.novellibrary.model.WebPage
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.network.getChapterUrls
import io.github.gmathi.novellibrary.service.download.DownloadChapterService
import io.github.gmathi.novellibrary.service.download.DownloadNovelService
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.setDefaultsNoAnimation
import kotlinx.android.synthetic.main.activity_chapters.*
import kotlinx.android.synthetic.main.content_chapters.*
import kotlinx.android.synthetic.main.listitem_chapter_ultimate.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class ChaptersActivity :
    BaseActivity(),
    GenericAdapterWithDragListener.Listener<WebPage>,
    ActionMode.Callback,
    AnimateCheckBox.OnCheckedChangeListener {

    lateinit var novel: Novel
    lateinit var adapter: GenericAdapterWithDragListener<WebPage>

    private var isSortedAsc: Boolean = true
    private var updateSet: HashSet<WebPage> = HashSet()
    private var removeDownloadMenuIcon: Boolean = false
    private var actionMode: ActionMode? = null
    private var confirmDialog: MaterialDialog? = null

    private var counter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chapters)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        novel = intent.getSerializableExtra("novel") as Novel
        val dbNovel = dbHelper.getNovel(novel.name!!)
        if (dbNovel != null) novel.copyFrom(dbNovel)

        setRecyclerView()
        if (novel.id != -1L)
            setDownloadStatus()
        setData()
    }

    private fun setRecyclerView() {
        adapter = GenericAdapterWithDragListener(items = ArrayList(), layoutResId = R.layout.listitem_chapter_ultimate, listener = this)
        dragSelectRecyclerView.setDefaultsNoAnimation(adapter)
        dragSelectRecyclerView.addItemDecoration(object : DividerItemDecoration(this, VERTICAL) {

            override fun getItemOffsets(outRect: Rect?, view: View?, parent: RecyclerView?, state: RecyclerView.State?) {
                val position = parent?.getChildAdapterPosition(view)
                if (position == parent?.adapter?.itemCount?.minus(1)) {
                    outRect?.setEmpty()
                } else {
                    super.getItemOffsets(outRect, view, parent, state)
                }
            }
        })
        swipeRefreshLayout.isEnabled = false
        swipeRefreshLayout.setOnRefreshListener { getChapters() }
    }

    private fun setData() {
        progressLayout.showLoading()
        getChaptersFromDB()
    }

    private fun getChaptersFromDB() {
        async {
            val chapters = await { ArrayList(dbHelper.getAllWebPages(novel.id)) }
            adapter.updateData(chapters)
            scrollToBookmark()

            if (adapter.items.isEmpty()) {
                progressLayout.showLoading()
                getChapters()
            } else {
                swipeRefreshLayout.isRefreshing = false
                progressLayout.showContent()
                if (adapter.items.size < novel.chapterCount.toInt()) {
                    swipeRefreshLayout.isRefreshing = true
                    getChapters()
                }
            }
        }
    }

    private fun getChapters() {
        async chapters@ {

            if (!Utils.checkNetwork(this@ChaptersActivity)) {
                if (adapter.items.isEmpty())
                    progressLayout.showError(ContextCompat.getDrawable(this@ChaptersActivity, R.drawable.ic_warning_white_vector), getString(R.string.no_internet), getString(R.string.try_again), {
                        progressLayout.showLoading()
                        getChapters()
                    })
                return@chapters
            }

            //Download latest chapters from network
            try {
                val chapterList = await { NovelApi().getChapterUrls(novel)?.reversed() }
                if (chapterList != null) {
                    if (novel.id != -1L) {
                        await {
                            novel.chapterCount = chapterList.size.toLong()
                            novel.newChapterCount = chapterList.size.toLong()
                            dbHelper.updateNovel(novel)
                        }
                        await { dbHelper.addWebPages(chapterList, novel) }
                        getChaptersFromDB()
                    } else {
                        adapter.updateData(ArrayList(chapterList))
                        swipeRefreshLayout.isRefreshing = false
                        progressLayout.showContent()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (progressLayout.isLoading)
                    progressLayout.showError(ContextCompat.getDrawable(this@ChaptersActivity, R.drawable.ic_warning_white_vector), getString(R.string.failed_to_load_url), getString(R.string.try_again), {
                        progressLayout.showLoading()
                        getChapters()
                    })
            }
        }
    }

    private fun scrollToBookmark() {
        if (novel.currentWebPageId != -1L) {
            val index = adapter.items.indexOfFirst { it.id == novel.currentWebPageId }
            if (index != -1)
                dragSelectRecyclerView.scrollToPosition(index)
        }
    }

    //region Adapter Listener Methods - onItemClick(), viewBinder()

    override fun onItemSelected(position: Int, selected: Boolean) {
        if (dragSelectRecyclerView.findViewHolderForAdapterPosition(position) != null) {
            @Suppress("UNCHECKED_CAST")
            (dragSelectRecyclerView.findViewHolderForAdapterPosition(position) as GenericAdapterWithDragListener.ViewHolder<WebPage>)
                .itemView?.chapterCheckBox?.isChecked = selected
        } else {
            val webPage = adapter.items[position]
            if (selected)
                addToUpdateSet(webPage)
            else
                removeFromUpdateSet(webPage)
        }
    }

    override fun onItemClick(item: WebPage) {
        if (novel.id != -1L) {
            novel.currentWebPageId = item.id
            dbHelper.updateNovel(novel)
            startReaderPagerDBActivity(novel)
        } else
            startReaderPagerActivity(novel, item)
    }

    @SuppressLint("SetTextI18n")
    override fun bind(item: WebPage, itemView: View, position: Int) {

        if (item.filePath != null) {
            itemView.greenView.visibility = View.VISIBLE
            itemView.greenView.setBackgroundColor(ContextCompat.getColor(this@ChaptersActivity, R.color.DarkGreen))
            itemView.greenView.animation = null
        } else {
            if (Constants.STATUS_DOWNLOAD.toString() == item.metaData[Constants.DOWNLOADING]) {
                if (item.id != -1L && DownloadChapterService.chapters.contains(item)) {
                    itemView.greenView.visibility = View.VISIBLE
                    itemView.greenView.setBackgroundColor(ContextCompat.getColor(this@ChaptersActivity, R.color.white))
                    itemView.greenView.startAnimation(AnimationUtils.loadAnimation(this@ChaptersActivity, R.anim.alpha_animation))
                } else {
                    itemView.greenView.visibility = View.VISIBLE
                    itemView.greenView.setBackgroundColor(ContextCompat.getColor(this@ChaptersActivity, R.color.Red))
                    itemView.greenView.animation = null
                }
            } else
                itemView.greenView.visibility = View.GONE
        }

        itemView.isReadView.visibility = if (item.isRead == 1) View.VISIBLE else View.GONE
        itemView.bookmarkView.visibility = if (item.id != -1L && item.id == novel.currentWebPageId) View.VISIBLE else View.INVISIBLE

        if (item.chapter != null)
            itemView.chapterTitle.text = item.chapter

        if (item.title != null) {
            if ((item.chapter != null) && item.title!!.contains(item.chapter!!))
                itemView.chapterTitle.text = item.title
            else
                itemView.chapterTitle.text = "${item.chapter}: ${item.title}"
        }

        //itemView.chapterCheckBox.visibility = if (novel.id != -1L) View.VISIBLE else View.GONE
        itemView.chapterCheckBox.isChecked = updateSet.contains(item)
        itemView.chapterCheckBox.tag = item
        itemView.chapterCheckBox.setOnCheckedChangeListener(this@ChaptersActivity)

//        itemView.chapterCheckBox.setOnLongClickListener {
//            dragSelectRecyclerView.setDragSelectActive(true, position)
//        }

        itemView.setOnLongClickListener {
            dragSelectRecyclerView.setDragSelectActive(true, position)

//            if (item.redirectedUrl != null)
//                this@ChaptersActivity.shareUrl(item.redirectedUrl!!)
//            else
//                this@ChaptersActivity.shareUrl(item.url!!)
            true
        }
    }

    override fun onCheckedChanged(buttonView: View?, isChecked: Boolean) {
        val webPage = (buttonView?.tag as WebPage?) ?: return

        // If Novel is not in Library
        if (novel.id == -1L) {
            if (isChecked)
                confirmDialog(getString(R.string.add_to_library_dialog_content, novel.name), MaterialDialog.SingleButtonCallback { dialog, _ ->
                    addNovelToLibrary()
                    invalidateOptionsMenu()
                    addToUpdateSet(webPage)
                    dialog.dismiss()
                }, MaterialDialog.SingleButtonCallback { dialog, _ ->
                    removeFromUpdateSet(webPage)
                    adapter.notifyDataSetChanged()
                    dialog.dismiss()
                })

        }

        //If Novel is already in library
        else {
            if (isChecked)
                addToUpdateSet(webPage)
            else
                removeFromUpdateSet(webPage)
        }
    }

    //endregion

    //region ActionMode Callback

    private fun selectAll() {
        adapter.items.filter { it.id != -1L }.forEach {
            addToUpdateSet(it)
        }
        adapter.notifyDataSetChanged()
    }

    private fun clearSelection() {
        adapter.items.filter { it.id != -1L }.forEach {
            removeFromUpdateSet(it)
        }
        adapter.notifyDataSetChanged()
    }

    private fun addToUpdateSet(webPage: WebPage) {
        if (webPage.id != -1L)
            updateSet.add(webPage)
        if (updateSet.isNotEmpty() && actionMode == null) {
            actionMode = startSupportActionMode(this)
        }
        actionMode?.title = getString(R.string.chapters_selected, updateSet.size)
    }

    private fun removeFromUpdateSet(webPage: WebPage) {
        updateSet.remove(webPage)
        actionMode?.title = getString(R.string.chapters_selected, updateSet.size)
        if (updateSet.isEmpty()) {
            actionMode?.finish()
        }
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_unread -> {
                confirmDialog(getString(R.string.mark_chapters_unread_dialog_content), MaterialDialog.SingleButtonCallback { dialog, _ ->
                    updateSet.forEach {
                        it.isRead = 0
                        dbHelper.updateWebPageReadStatus(it)
                    }
                    dialog.dismiss()
                    mode?.finish()
                })
            }
            R.id.action_read -> {
                confirmDialog(getString(R.string.mark_chapters_read_dialog_content), MaterialDialog.SingleButtonCallback { dialog, _ ->
                    updateSet.forEach {
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
                        val listToDownload = ArrayList(updateSet.filter { it.filePath == null })
                        if (listToDownload.isNotEmpty()) {
                            startChapterDownloadService(novel, ArrayList(updateSet.filter { it.filePath == null }))
                            SuperActivityToast.create(this@ChaptersActivity, Style(), Style.TYPE_STANDARD)
                                .setText(getString(R.string.background_chapter_downloads))
                                .setDuration(Style.DURATION_LONG)
                                .setFrame(Style.FRAME_KITKAT)
                                .setColor(PaletteUtils.getSolidColor(PaletteUtils.MATERIAL_GREEN))
                                .setAnimations(Style.ANIMATIONS_POP).show()
                        }
                        dialog.dismiss()
                        mode?.finish()
                    }
                })
            }
            R.id.action_select_all -> {
                selectAll()
            }
            R.id.action_clear_selection -> {
                clearSelection()
            }
            R.id.action_share_chapter -> {
                val urls = updateSet.map {
                    if (it.redirectedUrl != null)
                        it.redirectedUrl!!
                    else
                        it.url!!
                }.joinToString(separator = ", ")
                shareUrl(urls)
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
        updateSet.clear()
        actionMode = null
        adapter.notifyDataSetChanged()
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

    private fun confirmDialog(content: String, positiveCallback: MaterialDialog.SingleButtonCallback, negativeCallback: MaterialDialog.SingleButtonCallback) {
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


    //region OptionsMenu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_activity_chapters, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.getItem(1)?.isVisible = (novel.id == -1L)
        menu?.getItem(2)?.isVisible = (novel.id != -1L) && !removeDownloadMenuIcon
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when {
            item?.itemId == android.R.id.home -> finish()
            item?.itemId == R.id.action_download -> {
                confirmDialog(if (novel.id != -1L) getString(R.string.download_all_new_chapters_dialog_content) else getString(R.string.download_all_chapters_dialog_content), MaterialDialog.SingleButtonCallback { dialog, _ ->
                    addNovelToLibrary()
                    dbHelper.createDownloadQueue(novel.id)
                    dbHelper.updateDownloadQueueStatus(Constants.STATUS_DOWNLOAD, novel.id)
                    startNovelDownloadService(novelId = novel.id)
                    setDownloadStatus()
                    dialog.dismiss()
                })
            }
            item?.itemId == R.id.action_sort -> {
                isSortedAsc = !isSortedAsc
                adapter.updateData(ArrayList(adapter.items.asReversed()))
                checkData()
            }
            item?.itemId == R.id.action_add_to_library -> {
                addNovelToLibrary()
                invalidateOptionsMenu()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun checkData() {
        counter++
        if (counter >= 20) {
            dataCenter.lockRoyalRoad = false
        }
    }
    //endregion

    //region Update Download Status

    private fun setDownloadStatus() {
        statusCard.setOnClickListener { manageDownloadsDialog() }
        async {
            val dq = await { dbHelper.getDownloadQueue(novel.id) } ?: return@async
            removeDownloadMenuIcon = true
            invalidateOptionsMenu()
            val allWebPages = await { dbHelper.getAllWebPages(novel.id) }
            val readableWebPages = await { dbHelper.getAllReadableWebPages(novel.id) }
            var removeIcon = false

            when (dq.status) {
                Constants.STATUS_STOPPED -> {
                    //     statusCard.animation = null
                    statusCard.visibility = View.VISIBLE
                    statusText.text = getString(R.string.downloading_status, getString(R.string.downloading), getString(R.string.status), getString(R.string.download_paused))
                    removeIcon = true
                }

                Constants.STATUS_DOWNLOAD -> {
                    // statusCard.startAnimation(AnimationUtils.loadAnimation(this@ChaptersNewActivity, R.anim.alpha_animation))
                    statusCard.visibility = View.VISIBLE
                    if (DownloadNovelService.novelId == novel.id)
                        if (allWebPages.size == novel.chapterCount.toInt())
                            statusText.text = getString(R.string.downloading_status, getString(R.string.downloading), getString(R.string.status), getString(R.string.chapter_count, readableWebPages.size, allWebPages.size))
                        else
                            statusText.text = getString(R.string.downloading_status, getString(R.string.downloading), getString(R.string.status), getString(R.string.collection_chapters_info))
                    else
                        statusText.text = getString(R.string.downloading_status, getString(R.string.downloading), getString(R.string.status), getString(R.string.download_paused))
                    removeIcon = true
                }

                Constants.STATUS_COMPLETE -> {
                    //    statusCard.animation = null
                    statusCard.visibility = View.GONE
                    removeIcon = novel.chapterCount.toInt() == readableWebPages.size
                }
            }

            if (statusText.text.contains("aused"))
                statusCard.setCardBackgroundColor(ContextCompat.getColor(this@ChaptersActivity, R.color.DarkRed))
            else
                statusCard.setCardBackgroundColor(ContextCompat.getColor(this@ChaptersActivity, R.color.DarkGreen))

            if (removeIcon != removeDownloadMenuIcon) {
                removeDownloadMenuIcon = removeIcon
                invalidateOptionsMenu()
            }
        }
    }

    private fun updateDownloadStatus(orderId: Long?) {
        statusCard.setCardBackgroundColor(ContextCompat.getColor(this@ChaptersActivity, R.color.DarkGreen))
        if (orderId != null)
            statusText.text = getString(R.string.downloading_status, getString(R.string.downloading), getString(R.string.status), getString(R.string.chapter_count, dbHelper.getAllReadableWebPages(novel.id).size, novel.chapterCount))
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
    fun onNovelEvent(event: NovelEvent) {
        val webPage = event.webPage
        if (webPage != null) {
            adapter.updateItem(webPage)
        }
        if ((event.type == EventType.UPDATE && event.webPage == null) || event.type == EventType.COMPLETE)
            setDownloadStatus()
        else if (event.type == EventType.UPDATE && event.webPage != null) {
            updateDownloadStatus(event.webPage?.orderId)
        }
    }

    //endregion

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.READER_ACT_REQ_CODE) {
            if (novel.id != -1L) {
                novel = dbHelper.getNovel(novel.id)!!
                getChaptersFromDB()
            }
        }
    }

    private fun addNovelToLibrary() {
        async {
            if (novel.id == -1L) {
                progressLayout.showLoading()
                novel.id = await { dbHelper.insertNovel(novel) }
                if (isSortedAsc) await { dbHelper.addWebPages(adapter.items, novel) }
                else await { dbHelper.addWebPages(adapter.items.asReversed(), novel) }
                getChaptersFromDB()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        async.cancelAll()
    }


}
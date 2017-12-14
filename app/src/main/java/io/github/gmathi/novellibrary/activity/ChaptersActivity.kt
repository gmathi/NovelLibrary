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
import co.metalab.asyncawait.async
import com.afollestad.materialdialogs.MaterialDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hanks.library.AnimateCheckBox
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapterWithDragListener
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.model.Download
import io.github.gmathi.novellibrary.model.DownloadWebPageEvent
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.WebPage
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.network.getChapterUrls
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.setDefaultsNoAnimation
import kotlinx.android.synthetic.main.activity_chapters.*
import kotlinx.android.synthetic.main.content_chapters.*
import kotlinx.android.synthetic.main.listitem_chapter_ultimate.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

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
        val dbNovel = dbHelper.getNovel(novel.name)
        if (dbNovel != null) novel.copyFrom(dbNovel)

        setRecyclerView()
        setData()
    }

    private fun setRecyclerView() {
        adapter = GenericAdapterWithDragListener(items = ArrayList(), layoutResId = R.layout.listitem_chapter_ultimate, listener = this)
        dragSelectRecyclerView.isVerticalScrollBarEnabled = true
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
                if (adapter.items.size < novel.newChapterCount.toInt()) {
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
                        await {
                            for (i in 0 until chapterList.size) {
                                val webPage = dbHelper.getWebPage(novel.id, i.toLong())
                                if (webPage == null) {
                                    chapterList[i].orderId = i.toLong()
                                    chapterList[i].novelId = novel.id
                                    chapterList[i].id = dbHelper.createWebPage(chapterList[i])
                                } else
                                    chapterList[i].copyFrom(webPage)
                            }
                        }
                    } else {
                        swipeRefreshLayout.isRefreshing = false
                    }
                    adapter.updateData(ArrayList(chapterList))
                    progressLayout.showContent()
                    swipeRefreshLayout.isRefreshing = false
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
            startReaderDBPagerActivity(novel)
        } else
            startWebViewActivity(item.url)
    }

    @SuppressLint("SetTextI18n")
    override fun bind(item: WebPage, itemView: View, position: Int) {

        if (item.filePath != null) {
            itemView.greenView.visibility = View.VISIBLE
            itemView.greenView.setBackgroundColor(ContextCompat.getColor(this@ChaptersActivity, R.color.DarkGreen))
            itemView.greenView.animation = null
        } else {
            if (Download.STATUS_IN_QUEUE.toString() == item.metaData[Constants.DOWNLOADING]) {
//                if (item.id != -1L && DownloadService.chapters.contains(item)) {
//                    itemView.greenView.visibility = View.VISIBLE
//                    itemView.greenView.setBackgroundColor(ContextCompat.getColor(this@ChaptersActivity, R.color.white))
//                    itemView.greenView.startAnimation(AnimationUtils.loadAnimation(this@ChaptersActivity, R.anim.alpha_animation))
//                } else {
//                    itemView.greenView.visibility = View.VISIBLE
//                    itemView.greenView.setBackgroundColor(ContextCompat.getColor(this@ChaptersActivity, R.color.Red))
//                    itemView.greenView.animation = null
//                }
            } else
                itemView.greenView.visibility = View.GONE
        }

        itemView.isReadView.visibility = if (item.isRead == 1) View.VISIBLE else View.GONE
        itemView.bookmarkView.visibility = if (item.id != -1L && item.id == novel.currentWebPageId) View.VISIBLE else View.INVISIBLE

        itemView.chapterTitle.text = item.chapter

        if (item.title != null) {
            if (item.title!!.contains(item.chapter))
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
                            addWebPagesToDownload(ArrayList(updateSet.filter { it.filePath == null }))
                        }
                        dialog.dismiss()
                        manageDownloadsDialog()
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
                val urls = updateSet.joinToString(separator = ", ") {
                    if (it.redirectedUrl != null)
                        it.redirectedUrl!!
                    else
                        it.url
                }
                shareUrl(urls)
            }
            R.id.action_delete -> {
                updateSet.forEach {
                    deleteWebPage(it)
                }
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
                    addWebPagesToDownload(adapter.items)
                    dialog.dismiss()
                    //manageDownloadsDialog()
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
    fun onDownloadEvent(webPageEvent: DownloadWebPageEvent) {
        if (webPageEvent.download.novelName == novel.name) {
            adapter.items.firstOrNull { it.id == webPageEvent.webPageId }?.let { adapter.updateItem(it) }
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

                await {

                    val db = dbHelper.writableDatabase
                    db.beginTransaction()

                    try {
                        if (isSortedAsc) {
                            for (i in 0 until adapter.items.size) {
                                val webPage = dbHelper.getWebPage(novel.id, i.toLong())
                                if (webPage == null) {
                                    adapter.items[i].orderId = i.toLong()
                                    adapter.items[i].novelId = novel.id
                                    adapter.items[i].id = dbHelper.createWebPage(adapter.items[i], db)
                                } else adapter.items[i].copyFrom(webPage)
                            }
                        } else {
                            for (i in 0 until adapter.items.size) {
                                val size = adapter.items.size - 1
                                val webPage = dbHelper.getWebPage(novel.id, i.toLong())
                                if (webPage == null) {
                                    adapter.items[size - i].orderId = i.toLong()
                                    adapter.items[size - i].novelId = novel.id
                                    adapter.items[size - i].id = dbHelper.createWebPage(adapter.items[size - i], db)
                                } else adapter.items[size - i].copyFrom(webPage)
                            }
                        }
                    } finally {
                        db.endTransaction()
                    }
                }
                progressLayout.showContent()
            }
            progressLayout.showContent()
            startDownloadService()
        }
    }

    private fun addWebPagesToDownload(webPages: List<WebPage>) {
        async {
            progressLayout.showLoading()
            await {
                webPages.forEach {
                    val download = Download(it.id, novel.name, it.chapter)
                    download.orderId = it.orderId.toInt()
                    dbHelper.createDownload(download)
                }
            }
            progressLayout.showContent()
            startDownloadNovelService(novel.name)
        }
    }

    private fun deleteWebPage(webPage: WebPage) {
        if (webPage.filePath != null)
            try {
                val file = File(webPage.filePath)
                file.delete()
                webPage.filePath = null
                if (webPage.metaData.containsKey(Constants.MD_OTHER_LINKED_WEB_PAGES)) {
                    val linkedPages: ArrayList<WebPage> = Gson().fromJson(webPage.metaData[Constants.MD_OTHER_LINKED_WEB_PAGES], object : TypeToken<java.util.ArrayList<WebPage>>() {}.type)
                    linkedPages.forEach {
                        if (it.filePath != null) {
                            val linkedFile = File(it.filePath)
                            linkedFile.delete()
                        }
                        it.filePath = null
                    }
                    webPage.metaData.put(Constants.MD_OTHER_LINKED_WEB_PAGES, Gson().toJson(linkedPages))
                }
                dbHelper.updateWebPage(webPage)
            } catch (e: Exception) {
                Utils.error("ChaptersActivity", e.localizedMessage)
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        async.cancelAll()
    }


}
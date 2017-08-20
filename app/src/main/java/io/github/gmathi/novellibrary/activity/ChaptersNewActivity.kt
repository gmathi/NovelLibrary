package io.github.gmathi.novellibrary.activity

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import co.metalab.asyncawait.async
import com.afollestad.materialdialogs.MaterialDialog
import com.github.johnpersano.supertoasts.library.Style
import com.github.johnpersano.supertoasts.library.SuperActivityToast
import com.github.johnpersano.supertoasts.library.utils.PaletteUtils
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.ChaptersListPageListener
import io.github.gmathi.novellibrary.adapter.GenericFragmentStatePagerAdapter
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.fragment.ChaptersListFragment
import io.github.gmathi.novellibrary.model.*
import io.github.gmathi.novellibrary.service.DownloadNovelService
import io.github.gmathi.novellibrary.util.Constants
import kotlinx.android.synthetic.main.activity_chapters_new.*
import kotlinx.android.synthetic.main.content_chapters_new.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class ChaptersNewActivity : AppCompatActivity(), ActionMode.Callback {

    lateinit var novel: Novel

    var pageCount: Int = 0
    var updateSet: HashSet<WebPage> = HashSet()

    var removeMenuIcon: Boolean = false
    var actionMode: ActionMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chapters_new)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        novel = intent.getSerializableExtra("novel") as Novel
        pageCount = (novel.chapterCount / Constants.CHAPTER_PAGE_SIZE).toInt()
        if ((novel.chapterCount % Constants.CHAPTER_PAGE_SIZE).toInt() != 0) pageCount++

        setViewPager()
        setCurrentPage()
        setPageButtons()
        if (novel.id != -1L)
            setDownloadStatus()
    }

    private fun setViewPager() {
        val chapterPageAdapter = GenericFragmentStatePagerAdapter(supportFragmentManager, null, pageCount, ChaptersListPageListener(novel))
        viewPager.offscreenPageLimit = 3
        viewPager.adapter = chapterPageAdapter
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                val pageNum = position + 1
                pageButton.setText("$pageNum/$pageCount")

                //Toggle the 'Next' & 'Previous' Button
                nextButton.isClickable = pageNum != pageCount
                previousButton.isClickable = pageNum != 1
            }
        })
    }

    private fun setCurrentPage() {
        if (novel.id != -1L && novel.currentWebPageId != -1L) {
            val webPage = dbHelper.getWebPageByWebPageId(novel.currentWebPageId) ?: return
            val bookmarkPageNum = (novel.chapterCount - webPage.orderId - 1) / 15
            viewPager.currentItem = bookmarkPageNum.toInt()
        }
    }

    private fun setPageButtons() {
        nextButton.setOnClickListener { viewPager.setCurrentItem(viewPager.currentItem + 1, true) }
        previousButton.setOnClickListener { viewPager.setCurrentItem(viewPager.currentItem - 1, true) }

        nextButton.setOnLongClickListener { viewPager.setCurrentItem(pageCount - 1, true); true }
        previousButton.setOnLongClickListener { viewPager.setCurrentItem(0, true); true }

        pageButton.setText("${viewPager.currentItem + 1}/$pageCount")
        pageButton.setOnClickListener { openPageSelectDialog() }
    }

    //region ActionMode Callback

    fun addToUpdateSet(webPage: WebPage) {
        updateSet.add(webPage)
        if (actionMode == null)
            actionMode = startSupportActionMode(this)
        actionMode?.title = getString(R.string.chapters_selected, updateSet.size)
    }

    fun removeFromUpdateSet(webPage: WebPage) {
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
                            SuperActivityToast.create(this@ChaptersNewActivity, Style(), Style.TYPE_STANDARD)
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
                (viewPager.adapter.instantiateItem(viewPager, viewPager.currentItem) as ChaptersListFragment?)?.selectAll()
            }
            R.id.action_clear_selection -> {
                (viewPager.adapter.instantiateItem(viewPager, viewPager.currentItem) as ChaptersListFragment?)?.clearSelection()
            }
        }
        return false
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode?.menuInflater?.inflate(R.menu.menu_chapters_action_mode, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        menu?.findItem(R.id.action_unread)?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu?.findItem(R.id.action_read)?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu?.findItem(R.id.action_download)?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        updateSet.clear()
        actionMode = null
        EventBus.getDefault().post(ChapterEvent(novel))
    }

    //endregion

    //region Dialogs

    private fun openPageSelectDialog() {
        MaterialDialog.Builder(this@ChaptersNewActivity)
            .title("Page Selection")
            .content("Enter the page number")
            .inputRange(1, pageCount.toString().length)
            .inputType(InputType.TYPE_CLASS_NUMBER)
            .input(null, (viewPager.currentItem + 1).toString()) { dialog, input ->
                try {
                    val pageNum = input.toString().toInt()
                    if (pageNum in 1..pageCount) {
                        viewPager.currentItem = pageNum - 1
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    dialog.dismiss()
                }
            }.show()
    }

    private fun confirmDialog(content: String, callback: MaterialDialog.SingleButtonCallback) {
        MaterialDialog.Builder(this)
            .title(getString(R.string.confirm_action))
            .content(content)
            .positiveText(getString(R.string.okay))
            .negativeText(R.string.cancel)
            .onPositive(callback)
            .onNegative { dialog, _ -> dialog.dismiss() }
            .show()
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
        menuInflater.inflate(R.menu.menu_chapters, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.getItem(0)?.isVisible = !removeMenuIcon
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) finish()
        if (item?.itemId == R.id.action_download)
            confirmDialog(if (novel.id != -1L) getString(R.string.download_all_new_chapters_dialog_content) else getString(R.string.download_all_chapters_dialog_content), MaterialDialog.SingleButtonCallback { dialog, _ ->
                if (novel.id == -1L) {
                    novel.id = dbHelper.insertNovel(novel)
                    EventBus.getDefault().post(ChapterEvent(novel))
                }
                dbHelper.createDownloadQueue(novel.id)
                dbHelper.updateDownloadQueueStatus(Constants.STATUS_DOWNLOAD, novel.id)
                startNovelDownloadService(novelId = novel.id)
                setDownloadStatus()
                dialog.dismiss()
            })
        return super.onOptionsItemSelected(item)
    }
    //endregion

    //region Update Download Status

    private fun setDownloadStatus() {
        statusCard.setOnClickListener { manageDownloadsDialog() }
        async {
            val dq = await { dbHelper.getDownloadQueue(novel.id) } ?: return@async
            removeMenuIcon = true
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
                statusCard.setCardBackgroundColor(ContextCompat.getColor(this@ChaptersNewActivity, R.color.DarkRed))
            else
                statusCard.setCardBackgroundColor(ContextCompat.getColor(this@ChaptersNewActivity, R.color.DarkGreen))

            if (removeIcon != removeMenuIcon) {
                removeMenuIcon = removeIcon
                invalidateOptionsMenu()
            }
        }
    }

    private fun updateDownloadStatus(orderId: Long?) {
//        if (statusCard.animation == null)
//            statusCard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.alpha_animation))

//        if (statusText.text.contains("aused"))
//            statusCard.setCardBackgroundColor(ContextCompat.getColor(this@ChaptersNewActivity, R.color.DarkRed))
//        else
            statusCard.setCardBackgroundColor(ContextCompat.getColor(this@ChaptersNewActivity, R.color.DarkGreen))

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
        if (event.novelId != novel.id) return

        if ((event.type == EventType.UPDATE && event.webPage == null) || event.type == EventType.COMPLETE)
            setDownloadStatus()
        else if (event.type == EventType.UPDATE && event.webPage != null) {
            updateDownloadStatus(event.webPage?.orderId)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    //endRegion


}

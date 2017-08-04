package io.github.gmathi.novellibrary.activity

import android.os.Bundle
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import com.afollestad.materialdialogs.MaterialDialog
import com.github.johnpersano.supertoasts.library.Style
import com.github.johnpersano.supertoasts.library.SuperActivityToast
import com.github.johnpersano.supertoasts.library.utils.PaletteUtils
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.ChapterPageListener
import io.github.gmathi.novellibrary.adapter.GenericFragmentStatePagerAdapter
import io.github.gmathi.novellibrary.database.createDownloadQueue
import io.github.gmathi.novellibrary.database.getWebPageByWebPageId
import io.github.gmathi.novellibrary.database.updateDownloadQueueStatus
import io.github.gmathi.novellibrary.database.updateWebPageReadStatus
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.model.ChapterEvent
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.WebPage
import io.github.gmathi.novellibrary.util.Constants
import kotlinx.android.synthetic.main.activity_chapters_new.*
import kotlinx.android.synthetic.main.content_chapters_new.*
import org.greenrobot.eventbus.EventBus


class ChaptersNewActivity : AppCompatActivity(), ActionMode.Callback {

    lateinit var novel: Novel

    var pageCount: Int = 0
    var updateSet: HashSet<WebPage> = HashSet()

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
    }

    private fun setViewPager() {
        val chapterPageAdapter = GenericFragmentStatePagerAdapter(supportFragmentManager, null, pageCount, ChapterPageListener(novel))
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

    //endregion


    //region OptionsMenu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_chapters, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) finish()
        if (item?.itemId == R.id.action_download)
            confirmDialog(getString(R.string.download_all_chapters_dialog_content), MaterialDialog.SingleButtonCallback { dialog, _ ->
                if (novel.id == -1L) {
                    novel.id = dbHelper.insertNovel(novel)
                    EventBus.getDefault().post(ChapterEvent(novel))
                }
                dbHelper.createDownloadQueue(novel.id)
                dbHelper.updateDownloadQueueStatus(Constants.STATUS_DOWNLOAD, novel.id)
                startNovelDownloadService(novelId = novel.id)
                dialog.dismiss()
            })
        return super.onOptionsItemSelected(item)
    }
    //endregion

}

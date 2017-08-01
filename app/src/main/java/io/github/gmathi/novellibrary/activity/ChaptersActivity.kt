package io.github.gmathi.novellibrary.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import co.metalab.asyncawait.async
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.event.EventType
import io.github.gmathi.novellibrary.event.NovelEvent
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.WebPage
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.network.getChapterUrls
import io.github.gmathi.novellibrary.service.DownloadService
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.applyFont
import io.github.gmathi.novellibrary.util.setDefaults
import kotlinx.android.synthetic.main.activity_chapters.*
import kotlinx.android.synthetic.main.content_chapters.*
import kotlinx.android.synthetic.main.listitem_chapter.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class ChaptersActivity : AppCompatActivity(), GenericAdapter.Listener<WebPage> {


    lateinit var novel: Novel
    lateinit var adapter: GenericAdapter<WebPage>
    lateinit var chapters: ArrayList<WebPage>

    var sorted: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chapters)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        novel = intent.getSerializableExtra("novel") as Novel
        val dbNovel = dbHelper.getNovel(novel.name!!)
        if (dbNovel != null) novel.copyFrom(dbNovel)

        setRecyclerView()
        getChaptersFromDB()
        getChapters()

    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = ArrayList<WebPage>(), layoutResId = R.layout.listitem_chapter, listener = this)
        recyclerView.setDefaults(adapter)
        recyclerView.addItemDecoration(object : DividerItemDecoration(this, DividerItemDecoration.VERTICAL) {

            override fun getItemOffsets(outRect: Rect?, view: View?, parent: RecyclerView?, state: RecyclerView.State?) {
                val position = parent?.getChildAdapterPosition(view)
                if (position == parent?.adapter?.itemCount?.minus(1)) {
                    outRect?.setEmpty()
                } else {
                    super.getItemOffsets(outRect, view, parent, state)
                }
            }
        })
        swipeRefreshLayout.setOnRefreshListener { getChapters() }
    }

    private fun getChaptersFromDB() {
        async {
            progressLayout.showLoading()
            chapters = await { ArrayList(dbHelper.getAllWebPages(novel.id)) }
            if (chapters.isNotEmpty()) {
                updateData()
                progressLayout.showContent()
            }
        }
    }

    private fun getChapters() {
        async chapters@ {
            //Network check and show error only if loading spinner is visible
            if (!Utils.checkNetwork(this@ChaptersActivity)) {
                if (progressLayout.isLoading)
                    progressLayout.showError(ContextCompat.getDrawable(this@ChaptersActivity, R.drawable.ic_warning_white_vector), getString(R.string.no_internet), getString(R.string.try_again), {
                        progressLayout.showLoading()
                        getChapters()
                    })
                return@chapters
            } else {
                if (progressLayout.isLoading)
                    Toast.makeText(this@ChaptersActivity, getString(R.string.chapters_loading_message), Toast.LENGTH_LONG).show()
            }

            //Download latest chapters from network
            try {
                val chapterList = await { NovelApi().getChapterUrls(novel.url!!) }
                if (chapterList != null)
                    chapters = chapterList
            } catch (e: Exception) {
                if (progressLayout.isLoading)
                    progressLayout.showError(ContextCompat.getDrawable(this@ChaptersActivity, R.drawable.ic_warning_white_vector), getString(R.string.failed_to_load_url), getString(R.string.try_again), {
                        progressLayout.showLoading()
                        getChapters()
                    })
            }

            if (novel.id != -1L) await { syncWithChaptersFromDB() }
            updateData()
        }
    }

    private fun syncWithChaptersFromDB() {
        chapters.forEach {
            val webPage = dbHelper.getWebPage(novel.id, it.url!!)
            if (webPage != null) it.copyFrom(webPage)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateData() {
        if (sorted)
            adapter.updateData(ArrayList(chapters.asReversed()))
        else
            adapter.updateData(chapters)

        scrollToBookmark()
        progressLayout.showContent()
        swipeRefreshLayout.isRefreshing = false

        downloadLabel.applyFont(assets).text = getString(R.string.downloaded) + ": "
        updateChapterCount()
        updateDownloadButtonState()
        invalidateOptionsMenu()
    }

    //region DownloadButton States

    private fun updateDownloadButtonState() {
        chaptersDownloadButton.setOnClickListener { downloadChapters() }
        setToDownloadButton()

        if (novel.id != -1L) {

            async {
                val dq = await { dbHelper.getDownloadQueue(novel.id) }
                val readableWebPages = await { dbHelper.getAllReadableWebPages(novel.id) }

                if (dq != null) {
                    if (dq.status == Constants.STATUS_DOWNLOAD) {
                        setToDownloadingButton()
                    } else if (dq.status == Constants.STATUS_COMPLETE) {
                        if (readableWebPages.size != chapters.size)
                            setToSyncButton()
                        else
                            setToDownloadCompleteButton()
                    }
                }
            }
        }
    }


    private fun setToDownloadButton() {
        chaptersDownloadButton.isClickable = true
        chaptersDownloadButton.setBackgroundColor(ContextCompat.getColor(this@ChaptersActivity, R.color.DodgerBlue))
        chaptersDownloadButton.setIconResource(R.drawable.ic_cloud_download_white_vector)
        chaptersDownloadButton.setIconColor(ContextCompat.getColor(this@ChaptersActivity, R.color.white))
        chaptersDownloadButton.setText(getString(R.string.download))
    }


    private fun setToDownloadingButton() {
        chaptersDownloadButton.isClickable = false
        chaptersDownloadButton.setBackgroundColor(ContextCompat.getColor(this@ChaptersActivity, R.color.black_transparent))
        chaptersDownloadButton.setIconResource(R.drawable.ic_trending_down_white_vector)
        chaptersDownloadButton.setIconColor(ContextCompat.getColor(this@ChaptersActivity, R.color.DodgerBlue))
        chaptersDownloadButton.setText(getString(R.string.downloading))
    }

    private fun setToDownloadCompleteButton() {
        chaptersDownloadButton.isClickable = false
        chaptersDownloadButton.setBackgroundColor(ContextCompat.getColor(this@ChaptersActivity, R.color.Green))
        chaptersDownloadButton.setIconColor(ContextCompat.getColor(this@ChaptersActivity, R.color.white))
        chaptersDownloadButton.setIconResource(R.drawable.ic_check_circle_white_vector)
        chaptersDownloadButton.setText(getString(R.string.downloaded))
    }

    private fun setToSyncButton() {
        chaptersDownloadButton.isClickable = true
        chaptersDownloadButton.setBackgroundColor(ContextCompat.getColor(this@ChaptersActivity, R.color.DodgerBlue))
        chaptersDownloadButton.setIconColor(ContextCompat.getColor(this@ChaptersActivity, R.color.white))
        chaptersDownloadButton.setIconResource(R.drawable.ic_sync_white_vector)
        chaptersDownloadButton.setText(getString(R.string.sync))
    }

    //endregion

    @SuppressLint("SetTextI18n")
    override fun bind(item: WebPage, itemView: View, position: Int) {

        if (item.filePath != null) {

            if ((item.title != null) && (item.chapter != null) && item.title!!.contains(item.chapter!!))
                itemView.chapterTitleTextView.text = item.title
            else itemView.chapterTitleTextView.text = "${item.chapter}: ${item.title}"

            itemView.chapterStatusImage.setImageResource(R.drawable.ic_beenhere_white_vector)
        } else {
            itemView.chapterTitleTextView.text = item.chapter
            itemView.chapterStatusImage.setImageResource(R.drawable.ic_chrome_reader_mode_white_vector)
        }

        if (novel.id != -1L) {
            val dq = dbHelper.getDownloadQueue(novel.id)
            if (dq != null && dq.status == Constants.STATUS_DOWNLOAD && item.filePath == null) {
                itemView.chapterStatusImage.setImageResource(R.drawable.ic_queue_white_vector)
            }
        }

        if (item.isRead == 0) // Is not read
            itemView.chapterOverlay.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
        else
            itemView.chapterOverlay.setBackgroundColor(ContextCompat.getColor(this, R.color.black))

        if (novel.currentWebPageId != -1L && novel.currentWebPageId == item.id) {
            itemView.chapterOverlay.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
            itemView.chapterStatusImage.setImageResource(R.drawable.ic_bookmark_white_vector)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                itemView.chapterStatusImage.drawable?.mutate()?.setTint(ContextCompat.getColor(this, R.color.Orange))
            }
        }

        itemView.setOnLongClickListener {
            if (item.redirectedUrl != null)
                shareUrl(item.redirectedUrl!!)
            else
                shareUrl(item.url!!)
            true
        }
    }

    override fun onItemClick(item: WebPage) {
        val index = chapters.indexOf(item)
        val smallerData = ArrayList<WebPage>()

        val transferLimit = 30
        (index - transferLimit-1..index + transferLimit)
            .asSequence()
            .filter { it >= 0 && it < chapters.size }
            .mapTo(smallerData) { chapters[it] }

        startReaderPagerActivity(novel, item, smallerData)
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        EventBus.getDefault().unregister(this)
        super.onPause()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWebPageDownload(event: NovelEvent) {
        if (novel.id == event.novelId) {
            if (EventType.UPDATE == event.type) {
                if (event.webPage != null) {
                    val index = chapters.indexOf(event.webPage!!)
                    if (index != -1) {
                        chapters.removeAt(index)
                        chapters.add(index, event.webPage!!)
                    }
                    adapter.updateItem(event.webPage!!)
                    updateChapterCount()
                }

            } else if (event.type == EventType.COMPLETE) {
                adapter.notifyDataSetChanged()
                setToDownloadCompleteButton()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateChapterCount() {
        if (novel.id != -1L) {
            val webPages = chapters.filter { it.filePath != null }.count()
            chaptersDownloadStatus.applyFont(assets).text = "${webPages}/${chapters.size}"
            if (chapters.size == webPages) setToDownloadCompleteButton()
        } else {
            chaptersDownloadStatus.applyFont(assets).text = "0/${chapters.size}"
        }
    }

    private fun downloadChapters() {
        async {
            progressLayout.showLoading()
            setToDownloadingButton()

            //Add Chapters to database
            await {
                if (novel.id == -1L)
                    novel.id = dbHelper.insertNovel(novel)
                dbHelper.createDownloadQueue(novel.id)
                dbHelper.updateDownloadQueueStatus(Constants.STATUS_DOWNLOAD, novel.id)

                chapters.asReversed().forEach {
                    it.novelId = novel.id
                    val dbWebPage = dbHelper.getWebPage(it.novelId, it.url!!)
                    if (dbWebPage == null)
                        it.id = dbHelper.createWebPage(it)
                    else
                        it.copyFrom(dbWebPage)
                }
            }

            //Add to Downloads by calling Download Service
            if (!isFinishing) {
                adapter.notifyDataSetChanged()
                startDownloadService(novel.id)
                progressLayout.showContent()
            }
        }
    }

    private fun startDownloadService(novelId: Long) {
        val serviceIntent = Intent(this, DownloadService::class.java)
        serviceIntent.putExtra(Constants.NOVEL_ID, novelId)
        startService(serviceIntent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_activity_chapters, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.getItem(0)?.isVisible = recyclerView.adapter != null
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) finish()
        else if (item?.itemId == R.id.action_sort) {
            sorted = !sorted
            val newChapters = ArrayList<WebPage>(adapter.items.asReversed())
            adapter.updateData(newChapters)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            if (requestCode == Constants.READER_ACT_REQ_CODE) {
                if (novel.id != -1L) novel = dbHelper.getNovel(novel.id)!!
                syncWithChaptersFromDB()
                if (sorted)
                    adapter.updateData(ArrayList(chapters.reversed()))
                else
                    adapter.updateData(chapters)
                adapter.notifyDataSetChanged()
                scrollToBookmark()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun scrollToBookmark() {
        if (novel.currentWebPageId != -1L) {
            val index = adapter.items.indexOfFirst { it.id == novel.currentWebPageId }
            if (index != -1)
                recyclerView.scrollToPosition(index)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        async.cancelAll()
    }


}

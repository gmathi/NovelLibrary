package com.mgn.bingenovelreader.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.bumptech.glide.Glide
import com.mgn.bingenovelreader.R
import com.mgn.bingenovelreader.adapter.GenericAdapter
import com.mgn.bingenovelreader.database.createDownloadQueue
import com.mgn.bingenovelreader.database.getAllNovels
import com.mgn.bingenovelreader.dbHelper
import com.mgn.bingenovelreader.model.Novel
import com.mgn.bingenovelreader.service.DownloadService
import com.mgn.bingenovelreader.util.Constants
import com.mgn.bingenovelreader.util.enableLoadingView
import com.mgn.bingenovelreader.util.setDefaults
import com.mgn.bingenovelreader.util.setLoadingView
import kotlinx.android.synthetic.main.activity_library.*
import kotlinx.android.synthetic.main.content_library.*
import kotlinx.android.synthetic.main.listitem_novel.view.*
import org.jsoup.helper.StringUtil
import java.io.File


class LibraryActivity : BaseActivity(), GenericAdapter.Listener<Novel> {

    lateinit var adapter: GenericAdapter<Novel>
    var lastDeletedId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.title_activity_library)

//        fabSearch.setOnClickListener { startSearchActivity() }
//        fabDownload.setOnClickListener { startDownloadQueueActivity() }
        setRecyclerView()

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent != null) {
                    manageBroadcasts(intent)
                }
            }
        }
        //startDownloadService(-1L)
        setLoadingView(R.drawable.no_library, "Waiting??\nGet something else to read! {•̃̾_•̃̾}")
        enableLoadingView(adapter.items.isEmpty(), recyclerView)

    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = getAllNovels(), layoutResId = R.layout.listitem_novel, listener = this)
        recyclerView.setDefaults(adapter)
        swipeRefreshLayout.setOnRefreshListener {
            adapter.updateData(getAllNovels())
            enableLoadingView(adapter.items.isEmpty(), recyclerView)
            swipeRefreshLayout.isRefreshing = false
        }
    }

    //region Adapter Listener Methods - onItemClick(), viewBinder()

    override fun onItemClick(item: Novel) {
        // toast("${item.name} Clicked")
        if (lastDeletedId != item.id)
            startNovelDetailsActivity(item.id)
    }

    override fun bind(item: Novel, itemView: View) {
        if (item.imageFilePath != null)
            Glide.with(this).load(File(item.imageFilePath)).into(itemView.novelImageView)
        itemView.novelTitleTextView.text = item.name
        if (item.rating != null) {
            try {
                itemView.novelRatingBar.rating = item.rating!!.toFloat()
            } catch (e: Exception) {
                Log.w("Library Activity", "Rating: " + item.rating, e)
            }
            val ratingText = "(" + item.rating + ")"
            itemView.novelRatingTextView.text = ratingText
        }
        var genresText = item.genres?.joinToString { it }
        if (StringUtil.isBlank(genresText)) genresText = "N/A"
        itemView.novelGenreTextView.text = genresText
        itemView.novelDescriptionTextView.text = item.shortDescription
    }

    //endregion

    //region Flow to Activities & Services
    private fun startSearchActivity() {
        startActivityForResult(Intent(this, SearchActivity::class.java), Constants.SEARCH_REQ_CODE)
    }

    private fun startDownloadQueueActivity() {
        startActivityForResult(Intent(this, DownloadQueueActivity::class.java), Constants.DOWNLOAD_QUEUE_ACT_RES_CODE)
    }

    fun startNovelDetailsActivity(novelId: Long) {
//        val intent = Intent(this, NovelDetailsActivity::class.java)
//        intent.putExtra(Constants.NOVEL_ID, novelId)
//        startActivityForResult(intent, Constants.NOVEL_DETAILS_REQ_CODE)
    }

    //endregion

    override fun onPostResume() {
        super.onPostResume()
//        if (fabMenu.isOpened)
//            fabMenu.close(true)
    }

    private fun getAllNovels(): ArrayList<Novel> {
        return ArrayList(dbHelper.getAllNovels())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.SEARCH_REQ_CODE) {
            adapter.updateData(getAllNovels())
            enableLoadingView(adapter.items.isEmpty(), recyclerView)
            return
        }

        if (resultCode == Constants.NOVEL_DETAILS_RES_CODE) {
            val novelId = data?.extras?.getLong(Constants.NOVEL_ID)
            if (novelId != null) {
                lastDeletedId = novelId
                adapter.removeItemAt(adapter.items.indexOfFirst { it.id == novelId })
                Handler().postDelayed({ lastDeletedId = -1 }, 1500)
                enableLoadingView(adapter.items.isEmpty(), recyclerView)
            }
            return
        }

        if (resultCode == Constants.DOWNLOAD_QUEUE_ACT_RES_CODE) {
            adapter.updateData(getAllNovels())
            enableLoadingView(adapter.items.isEmpty(), recyclerView)
        }

    }

    override fun manageBroadcasts(intent: Intent) {
        // No Intents to handle
    }

    override fun getBroadcastIntentActions(): ArrayList<String> {
        val actions = ArrayList<String>()
        actions.add(Constants.NOVEL_DELETED)
        return actions
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_library, menu)
        val drawable = DrawableCompat.wrap(menu.findItem(R.id.action_sync).icon)
        DrawableCompat.setTint(drawable, ContextCompat.getColor(this, R.color.white))
        menu.findItem(R.id.action_sync).icon = drawable
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_sync -> {
                syncNovels()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun syncNovels() {
        getAllNovels().forEach { dbHelper.createDownloadQueue(it.id) }
        startDownloadService(1L)
    }

    private fun startDownloadService(novelId: Long) {
        val serviceIntent = Intent(this, DownloadService::class.java)
        serviceIntent.putExtra(Constants.NOVEL_ID, novelId)
        startService(serviceIntent)
    }

}

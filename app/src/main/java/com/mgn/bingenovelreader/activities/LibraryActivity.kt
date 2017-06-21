package com.mgn.bingenovelreader.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.bumptech.glide.Glide
import com.mgn.bingenovelreader.R
import com.mgn.bingenovelreader.adapters.GenericAdapter
import com.mgn.bingenovelreader.dbHelper
import com.mgn.bingenovelreader.models.Novel
import com.mgn.bingenovelreader.services.DownloadService
import com.mgn.bingenovelreader.utils.Constants
import com.mgn.bingenovelreader.utils.setDefaults
import com.mgn.bingenovelreader.utils.toast
import kotlinx.android.synthetic.main.activity_library.*
import kotlinx.android.synthetic.main.content_library.*
import kotlinx.android.synthetic.main.listitem_novel.view.*
import java.io.File


class LibraryActivity : AppCompatActivity(), GenericAdapter.Listener<Novel> {

    lateinit var adapter: GenericAdapter<Novel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)
        setSupportActionBar(toolbar)

        fabSearch.setOnClickListener { startSearchActivity() }
        fabDownload.setOnClickListener { startDownloadQueueActivity() }
        setRecyclerView()
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = ArrayList(getAllNovels()), layoutResId = R.layout.listitem_novel, listener = this)
        recyclerView.setDefaults(adapter)
        swipeRefreshLayout.setOnRefreshListener {
            adapter.updateData(ArrayList(getAllNovels()))
            swipeRefreshLayout.isRefreshing = false
        }
    }

    //region Adapter Listener Methods - onItemClick(), viewBinder()

    override fun onItemClick(item: Novel) {
        toast("${item.name} Clicked")
        dbHelper.createDownloadQueue(item.id)
        startDownloadService(item.id)
    }

    override fun bind(item: Novel, itemView: View) {
        if (item.imageFilePath != null)
            Glide.with(this).load(File(item.imageFilePath)).into(itemView.novelImageView)
        itemView.novelTitleTextView.text = item.name
        if (item.rating != null) {
            itemView.novelRatingBar.rating = item.rating!!.toFloat()
            itemView.novelRatingTextView.text = "(" + item.rating + ")"
        }
        itemView.novelGenreTextView.text = item.genres?.joinToString { it }
        itemView.novelDescriptionTextView.text = item.shortDescription
    }

    //endregion

    //region Flow to Activities & Services
    private fun startSearchActivity() {
        startActivity(Intent(this, SearchActivity::class.java))
    }

    private fun startDownloadQueueActivity() {
        startActivity(Intent(this, DownloadQueueActivity::class.java))
    }

    private fun startDownloadService(novelId: Long) {
        val serviceIntent = Intent(this, DownloadService::class.java)
        serviceIntent.putExtra(Constants.NOVEL_ID, novelId)
        startService(serviceIntent)
    }
    //endregion

    override fun onPostResume() {
        super.onPostResume()
        if (fabMenu.isOpened)
            fabMenu.close(true)
    }

    private fun getAllNovels(): MutableList<Novel>? {
        val novels = dbHelper.allNovels
//        novels.forEach { it.genres = dbHelper.getGenres(it.id) }
        return novels
    }

}

package com.mgn.bingenovelreader.fragment

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.*
import com.bumptech.glide.Glide
import com.mgn.bingenovelreader.R
import com.mgn.bingenovelreader.activities.NovelDetailsActivity
import com.mgn.bingenovelreader.adapter.GenericAdapter
import com.mgn.bingenovelreader.database.createDownloadQueue
import com.mgn.bingenovelreader.database.getAllNovels
import com.mgn.bingenovelreader.dbHelper
import com.mgn.bingenovelreader.event.NovelEvent
import com.mgn.bingenovelreader.model.Novel
import com.mgn.bingenovelreader.service.DownloadService
import com.mgn.bingenovelreader.util.Constants
import com.mgn.bingenovelreader.util.enableLoadingView
import com.mgn.bingenovelreader.util.setDefaults
import kotlinx.android.synthetic.main.activity_library.*
import kotlinx.android.synthetic.main.content_library.*
import kotlinx.android.synthetic.main.listitem_novel.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jsoup.helper.StringUtil
import java.io.File


class LibraryFragment : BaseFragment(), GenericAdapter.Listener<Novel> {

    lateinit var adapter: GenericAdapter<Novel>
    var lastDeletedId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.activity_library, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        toolbar.title = getString(R.string.title_library)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        setRecyclerView()
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(ArrayList(dbHelper.getAllNovels()), R.layout.listitem_novel, this)
        recyclerView.setDefaults(adapter)
        swipeRefreshLayout.setOnRefreshListener {
            adapter.updateData(ArrayList(dbHelper.getAllNovels()))
            enableLoadingView(adapter.items.isEmpty(), recyclerView)
            swipeRefreshLayout.isRefreshing = false
        }
    }


    //region Adapter Listener Methods - onItemClick(), viewBinder()

    override fun onItemClick(item: Novel) {
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

    //region Sync Code
    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_library, menu)
        val drawable = DrawableCompat.wrap(menu.findItem(R.id.action_sync).icon)
        DrawableCompat.setTint(drawable, ContextCompat.getColor(context, R.color.white))
        menu.findItem(R.id.action_sync).icon = drawable
        super.onCreateOptionsMenu(menu, menuInflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        //menu.findItem(R.id.action_newItem).isVisible = true
        super.onPrepareOptionsMenu(menu)

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
        dbHelper.getAllNovels().forEach { dbHelper.createDownloadQueue(it.id) }
        startDownloadService(1L)
    }

    private fun startDownloadService(novelId: Long) {
        val serviceIntent = Intent(activity, DownloadService::class.java)
        serviceIntent.putExtra(Constants.NOVEL_ID, novelId)
        activity.startService(serviceIntent)
    }
    //endregion

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onResume() {
        super.onResume()
        adapter.updateData(ArrayList(dbHelper.getAllNovels()))
        enableLoadingView(adapter.items.isEmpty(), recyclerView)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNovelEvent(event: NovelEvent) {
    }


    fun startNovelDetailsActivity(novelId: Long) {
        val intent = Intent(activity, NovelDetailsActivity::class.java)
        intent.putExtra(Constants.NOVEL_ID, novelId)
        activity.startActivityForResult(intent, Constants.NOVEL_DETAILS_REQ_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Constants.NOVEL_DETAILS_RES_CODE) {
            val novelId = data?.extras?.getLong(Constants.NOVEL_ID)
            if (novelId != null) {
                lastDeletedId = novelId
                adapter.removeItemAt(adapter.items.indexOfFirst { it.id == novelId })
                Handler().postDelayed({ lastDeletedId = -1 }, 1200)
                enableLoadingView(adapter.items.isEmpty(), recyclerView)
            }
            return
        }
    }
}

package com.mgn.bingenovelreader.activities

import android.animation.Animator
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.mgn.bingenovelreader.R
import com.mgn.bingenovelreader.adapters.GenericAdapter
import com.mgn.bingenovelreader.dbHelper
import com.mgn.bingenovelreader.models.Novel
import com.mgn.bingenovelreader.network.NovelApi
import com.mgn.bingenovelreader.services.DownloadService
import com.mgn.bingenovelreader.utils.*
import com.mgn.bingenovelreader.utils.Constants.IMAGES_DIR_NAME
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.android.synthetic.main.content_search.*
import kotlinx.android.synthetic.main.listitem_novel.view.*
import org.cryse.widget.persistentsearch.PersistentSearchView
import java.io.File
import java.io.FileOutputStream


class SearchActivity : AppCompatActivity(), GenericAdapter.Listener<Novel> {

    lateinit var adapter: GenericAdapter<Novel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        setRecyclerView()
        setSearchView()

        loadingImageView.visibility = View.INVISIBLE
        Glide.with(this).load("https://media.giphy.com/media/ADyQEh474eu0o/giphy.gif").into(loadingImageView)
        //searchNovels("Realms")
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = ArrayList<Novel>(), layoutResId = R.layout.listitem_novel, listener = this)
        searchRecyclerView.setDefaults(adapter)
    }

    private fun setSearchView() {
        searchView.setHomeButtonListener { finish() }
        searchView.setSuggestionBuilder(SuggestionsBuilder())
        searchView.setSearchListener(object : PersistentSearchView.SearchListener {

            override fun onSearch(searchTerm: String?) {
                searchTerm?.addToSearchHistory()
                searchNovels(searchTerm)
            }

            override fun onSearchEditOpened() {
                searchViewBgTint.visibility = View.VISIBLE
                searchViewBgTint
                        .animate()
                        .alpha(1.0f)
                        .setDuration(300)
                        .setListener(SimpleAnimationListener())
                        .start()
            }

            override fun onSearchEditClosed() {
                searchViewBgTint
                        .animate()
                        .alpha(0.0f)
                        .setDuration(300)
                        .setListener(object : SimpleAnimationListener() {
                            override fun onAnimationEnd(animation: Animator) {
                                super.onAnimationEnd(animation)
                                searchViewBgTint.visibility = View.GONE
                            }
                        })
                        .start()
            }

            override fun onSearchExit() {}
            override fun onSearchCleared() {}
            override fun onSearchTermChanged(searchTerm: String?) {}
            override fun onSearchEditBackPressed(): Boolean = false
        })

    }

    private fun searchNovels(searchTerm: String?) {
        searchRecyclerView.visibility = View.INVISIBLE
        loadingImageView.visibility = View.VISIBLE

        Thread(Runnable {
            searchTerm?.let {
                val newList = ArrayList(NovelApi().search(it)["novel-updates"])
                Handler(Looper.getMainLooper()).post {
                    loadingImageView.visibility = View.GONE
                    searchRecyclerView.visibility = View.VISIBLE
                    adapter.updateData(newList)
                }
            }
        }).start()
    }

    //region Adapter Listener Methods - onItemClick(), viewBinder()

    override fun onItemClick(item: Novel) {
        toast("${item.name} Clicked")
        val novelId = dbHelper.insertNovel(item)
        dbHelper.createDownloadQueue(novelId)
        startDownloadService(novelId)
    }

    override fun bind(item: Novel, itemView: View) {
        if (item.imageFilePath == null) {
            Glide.with(this).asBitmap().load(item.imageUrl).into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(bitmap: Bitmap?, transition: Transition<in Bitmap>?) {
                    itemView.novelImageView.setImageBitmap(bitmap)
                    Thread(Runnable {
                        val file = File(filesDir, IMAGES_DIR_NAME + "/" + Uri.parse(item.imageUrl).getFileName())
                        val os = FileOutputStream(file)
                        bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, os)
                        item.imageFilePath = file.path
                    }).start()
                }
            })
        } else {
            Glide.with(this).load(File(item.imageFilePath)).into(itemView.novelImageView)
        }

        itemView.novelTitleTextView.text = item.name
        if (item.rating != null) {
            itemView.novelRatingBar.rating = item.rating!!.toFloat()
            itemView.novelRatingTextView.text = "(" + item.rating + ")"
        }
        itemView.novelGenreTextView.text = item.genres?.joinToString { it }
        itemView.novelDescriptionTextView.text = item.shortDescription
    }

    //endregion

    private fun startDownloadService(novelId: Long) {
        val serviceIntent = Intent(this, DownloadService::class.java)
        serviceIntent.putExtra(Constants.NOVEL_ID, novelId)
        startService(serviceIntent)
    }

    override fun onBackPressed() {
        if (searchView.isEditing) {
            searchView.cancelEditing()
        } else {
            super.onBackPressed()
        }
    }

}

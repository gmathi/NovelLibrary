package com.mgn.bingenovelreader.fragment

import android.animation.Animator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.mgn.bingenovelreader.R
import com.mgn.bingenovelreader.adapter.GenericAdapter
import com.mgn.bingenovelreader.database.createDownloadQueue
import com.mgn.bingenovelreader.database.getNovel
import com.mgn.bingenovelreader.dbHelper
import com.mgn.bingenovelreader.model.Novel
import com.mgn.bingenovelreader.network.NovelApi
import com.mgn.bingenovelreader.service.DownloadService
import com.mgn.bingenovelreader.util.*
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.android.synthetic.main.content_search.*
import kotlinx.android.synthetic.main.listitem_novel_search.view.*
import org.cryse.widget.persistentsearch.PersistentSearchView
import org.jetbrains.anko.support.v4.toast
import org.jsoup.helper.StringUtil
import java.io.File
import java.io.FileOutputStream


class SearchFragment : BaseFragment(), GenericAdapter.Listener<Novel> {

    lateinit var adapter: GenericAdapter<Novel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.activity_search, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setRecyclerView()
        setSearchView()

        setLoadingView(R.drawable.loading_search, null)
        enableLoadingView(false, null)
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = ArrayList<Novel>(), layoutResId = R.layout.listitem_novel_search, listener = this)
        recyclerView.setDefaults(adapter)
    }

    private fun setSearchView() {
        searchView.setHomeButtonVisibility(View.GONE)
        searchView.setSuggestionBuilder(SuggestionsBuilder())
        searchView.setSearchListener(object : PersistentSearchView.SearchListener {

            override fun onSearch(searchTerm: String?) {
                if (!Util.checkNetwork(context)) {
                    toast("No Active Internet! (⋋▂⋌)")
                } else {
                    searchTerm?.addToSearchHistory()
                    searchNovels(searchTerm)
                }
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
        recyclerView.visibility = View.INVISIBLE
        enableLoadingView(true, recyclerView)

        Thread(Runnable {
            searchTerm?.let {
                val newList = ArrayList(NovelApi().search(it)[Constants.NovelSites.NOVEL_UPDATES])
                newList.addAll(ArrayList(NovelApi().search(it)[Constants.NovelSites.ROYAL_ROAD]))
                Handler(Looper.getMainLooper()).post {
                    enableLoadingView(false, recyclerView)
                    adapter.updateData(newList)
                    if (adapter.items.isEmpty())
                        toast("Found nothing for the search - $it. Try Again!")
                }
            }
        }).start()
    }

    //region Adapter Listener Methods - onItemClick(), viewBinder()

    override fun onItemClick(item: Novel) {
        //Do Nothing
        addToDownloads(item)
    }

    override fun bind(item: Novel, itemView: View) {
        if (item.imageFilePath == null) {
            Glide.with(this).asBitmap().load(item.imageUrl).into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(bitmap: Bitmap?, transition: Transition<in Bitmap>?) {
                    itemView.novelImageView.setImageBitmap(bitmap)
                    Thread(Runnable {
                        val file = File(activity.filesDir, Constants.IMAGES_DIR_NAME + "/" + Uri.parse(item.imageUrl).getFileName())
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
        itemView.novelDescriptionTextView.text = item.shortDescription

        var genresText = item.genres?.joinToString { it }
        if (StringUtil.isBlank(genresText)) genresText = "N/A"
        itemView.novelGenreTextView.text = genresText

        if (item.rating != null) {
            try {
                itemView.novelRatingBar.rating = item.rating!!.toFloat()
            } catch (e: Exception) {
                Log.w("Library Activity", "Rating: " + item.rating, e)
            }
            val ratingText = "(" + item.rating + ")"
            itemView.novelRatingTextView.text = ratingText
        }

        if (dbHelper.getNovel(item.name!!) != null) {
            itemView.downloadButton.setImageResource(R.drawable.ic_playlist_add_check_black_vector)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                itemView.downloadButton.imageTintList = ColorStateList(arrayOf(intArrayOf()), intArrayOf(ContextCompat.getColor(context, R.color.LimeGreen)))
            }
        } else {
            itemView.downloadButton.setImageResource(R.drawable.ic_file_download_black_vector)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                itemView.downloadButton.imageTintList = ColorStateList(arrayOf(intArrayOf()), intArrayOf(ContextCompat.getColor(context, R.color.SlateGray)))
            }
        }

    }

//endregion

    private fun addToDownloads(item: Novel) {
        if (dbHelper.getNovel(item.name!!) == null) {
            val novelId = dbHelper.insertNovel(item)
            dbHelper.createDownloadQueue(novelId)
            startDownloadService(novelId)
            adapter.updateItem(item)
        }
    }

    private fun startDownloadService(novelId: Long) {
        val serviceIntent = Intent(activity, DownloadService::class.java)
        serviceIntent.putExtra(Constants.NOVEL_ID, novelId)
        activity.startService(serviceIntent)
    }

//    fun enableLoadingView(enable: Boolean, otherView: View?) {
//            loadingLayout.visibility = if (enable) View.VISIBLE else View.INVISIBLE
//            otherView?.visibility = if (enable) View.INVISIBLE else View.VISIBLE
//    }

}

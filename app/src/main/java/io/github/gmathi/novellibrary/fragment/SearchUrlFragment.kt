package io.github.gmathi.novellibrary.fragment

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
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
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.NovelDetailsActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.extension.getFileName
import io.github.gmathi.novellibrary.extension.setDefaults
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Utils
import kotlinx.android.synthetic.main.content_recycler_view.*
import kotlinx.android.synthetic.main.listitem_novel.view.*
import java.io.File
import java.io.FileOutputStream


class SearchUrlFragment : BaseFragment(), GenericAdapter.Listener<Novel> {

    lateinit var searchUrl: String
    var downloadThread: Thread? = null

    companion object {
        fun newInstance(url: String): SearchUrlFragment {
            val bundle = Bundle()
            bundle.putString("url", url)
            val fragment = SearchUrlFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    lateinit var adapter: GenericAdapter<Novel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.content_recycler_view, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        //(activity as AppCompatActivity).setSupportActionBar(null)
        searchUrl = arguments.getString("url")
        setRecyclerView()
        progressLayout.showLoading()
        searchNovels()
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = ArrayList<Novel>(), layoutResId = R.layout.listitem_novel, listener = this)
        recyclerView.setDefaults(adapter)
        swipeRefreshLayout.setOnRefreshListener { searchNovels() }
    }

    private fun searchNovels() {
        if (!Utils.checkNetwork(activity)) {
            progressLayout.showError(ContextCompat.getDrawable(context, R.drawable.ic_warning_white_vector), getString(R.string.no_internet), getString(R.string.try_again), {
                progressLayout.showLoading()
                searchNovels()
            })
            return
        }

        if (downloadThread != null && downloadThread!!.isAlive && !downloadThread!!.isInterrupted)
            downloadThread!!.interrupt()
        downloadThread = Thread(Runnable {
            val results = NovelApi().searchUrl(searchUrl)
            if (results != null) {
                Handler(Looper.getMainLooper()).post {
                    if (isVisible && (!isDetached || !isRemoving)) {
                        loadSearchResults(results)
                        swipeRefreshLayout.isRefreshing = false
                    }
                }
            }
        })
        downloadThread!!.start()
    }

    fun loadSearchResults(results: ArrayList<Novel>) {
        adapter.updateData(results)
        if (adapter.items.isEmpty()) {
            progressLayout.showError(ContextCompat.getDrawable(context, R.drawable.ic_youtube_searched_for_white_vector), "No Novels Found!", "Try Again", {
                progressLayout.showLoading()
                searchNovels()
            })
        } else {
            progressLayout.showContent()
        }
    }

    //region Adapter Listener Methods - onItemClick(), viewBinder()

    override fun onItemClick(item: Novel) {
        startNovelDetailsActivity(item)
        //addToDownloads(item)
    }

    override fun bind(item: Novel, itemView: View, position: Int) {
        itemView.novelImageView.setImageResource(android.R.color.transparent)
        val file = File(activity.filesDir, Constants.IMAGES_DIR_NAME + "/" + Uri.parse(item.imageUrl).getFileName())
        if (file.exists())
            item.imageFilePath = file.path

        if (item.imageFilePath == null) {
            Glide.with(this).asBitmap().load(item.imageUrl).into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(bitmap: Bitmap?, transition: Transition<in Bitmap>?) {
                    itemView.novelImageView.setImageBitmap(bitmap)
                    Thread(Runnable {
                        try {
                            val os = FileOutputStream(file)
                            bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, os)
                            item.imageFilePath = file.path
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }).start()
                }
            })
        } else {
            Glide.with(this).load(File(item.imageFilePath)).into(itemView.novelImageView)
        }

        //Other Data Fields
        itemView.novelTitleTextView.text = item.name
        if (item.rating != null) {
            var ratingText = "(N/A)"
            try {
                val rating = item.rating!!.toFloat()
                itemView.novelRatingBar.rating = rating
                ratingText = "(" + String.format("%.1f", rating) + ")"
            } catch (e: Exception) {
                Log.w("Library Activity", "Rating: " + item.rating, e)
            }
            itemView.novelRatingText.text = ratingText
        }
    }

//endregion

//    private fun addToDownloads(item: Novel) {
//        if (dbHelper.getNovel(item.name!!) == null) {
//            val novelId = dbHelper.insertNovel(item)
//            dbHelper.createDownloadQueue(novelId)
//            startDownloadService(novelId)
//            adapter.updateItem(item)
//        }
//    }

//    private fun startDownloadService(novelId: Long) {
//        val serviceIntent = Intent(activity, DownloadService::class.java)
//        serviceIntent.putExtra(Constants.NOVEL_ID, novelId)
//        activity.startService(serviceIntent)
//    }

    fun startNovelDetailsActivity(novel: Novel) {
        val intent = Intent(activity, NovelDetailsActivity::class.java)
        val bundle = Bundle()
        bundle.putSerializable("novel", novel)
        intent.putExtras(bundle)
        activity.startActivityForResult(intent, Constants.NOVEL_DETAILS_REQ_CODE)
    }

    override fun onPause() {
        super.onPause()
        downloadThread?.interrupt()
    }


}

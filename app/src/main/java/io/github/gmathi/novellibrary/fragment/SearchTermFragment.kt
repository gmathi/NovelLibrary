package io.github.gmathi.novellibrary.fragment

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import co.metalab.asyncawait.async
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.extensions.startNovelDetailsActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.extensions.isFragmentActive
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.network.*
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.getGlideUrl
import io.github.gmathi.novellibrary.util.setDefaults
import kotlinx.android.synthetic.main.content_recycler_view.*
import kotlinx.android.synthetic.main.listitem_novel.view.*
import java.net.URLEncoder


class SearchTermFragment : BaseFragment(), GenericAdapter.Listener<Novel> {

    private lateinit var searchTerm: String
    private lateinit var resultType: String

    companion object {
        fun newInstance(searchTerms: String, resultType: String): SearchTermFragment {
            val bundle = Bundle()
            bundle.putString("searchTerm", searchTerms)
            bundle.putString("resultType", resultType)
            val fragment = SearchTermFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    lateinit var adapter: GenericAdapter<Novel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.content_recycler_view, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        //(activity as AppCompatActivity).setSupportActionBar(null)

        searchTerm = arguments?.getString("searchTerm")!!
        resultType = arguments?.getString("resultType")!!

        setRecyclerView()

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("results")) {
                @Suppress("UNCHECKED_CAST")
                adapter.updateData(savedInstanceState.getSerializable("results") as java.util.ArrayList<Novel>)
                return
            }
        }

        progressLayout.showLoading()
        searchNovels()
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = ArrayList(), layoutResId = R.layout.listitem_novel, listener = this)
        recyclerView.setDefaults(adapter)
        swipeRefreshLayout.setOnRefreshListener { searchNovels() }
    }

    private fun searchNovels() {

        async search@ {

            if (!Utils.isConnectedToNetwork(activity)) {
                progressLayout.showError(ContextCompat.getDrawable(context!!, R.drawable.ic_warning_white_vector), getString(R.string.no_internet), getString(R.string.try_again)) {
                    progressLayout.showLoading()
                    searchNovels()
                }
                return@search
            }

            val searchTerms = URLEncoder.encode(searchTerm, "UTF-8")
            var results: ArrayList<Novel>? = null

            when (resultType) {
                HostNames.NOVEL_UPDATES -> results = await { NovelApi.searchNovelUpdates(searchTerms) }
                HostNames.ROYAL_ROAD -> results = await { NovelApi.searchRoyalRoad(searchTerms) }
                HostNames.NOVEL_FULL -> results = await { NovelApi.searchNovelFull(searchTerms) }
                HostNames.WLN_UPDATES -> results = await { NovelApi.searchWlnUpdates(searchTerms) }
            }

            if (results == null) results = ArrayList()
            if (isFragmentActive() && progressLayout != null) {
                loadSearchResults(results)
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun loadSearchResults(results: ArrayList<Novel>) {
        adapter.updateData(results)
        if (adapter.items.isEmpty()) {
            progressLayout.showError(ContextCompat.getDrawable(context!!, R.drawable.ic_youtube_searched_for_white_vector), "No Novels Found!", "Try Again") {
                progressLayout.showLoading()
                searchNovels()
            }
        } else {
            progressLayout.showContent()
        }
    }

    //region Adapter Listener Methods - onItemClick(), viewBinder()

    override fun onItemClick(item: Novel) {
        (activity as? AppCompatActivity)?.startNovelDetailsActivity(item, false)
        //addToDownloads(item)
    }

    override fun bind(item: Novel, itemView: View, position: Int) {
        itemView.novelImageView.setImageResource(android.R.color.transparent)

        if (!item.imageUrl.isNullOrBlank()) {
            Glide.with(this)
                    .load(item.imageUrl?.getGlideUrl())
                    .apply(RequestOptions.circleCropTransform())
                    .into(itemView.novelImageView)
        }

        //Other Data Fields
        itemView.novelTitleTextView.text = item.name
        if (item.rating != null && item.rating != "N/A") {
            var ratingText = "(N/A)"
            try {
                val rating = item.rating!!.toFloat()
                itemView.novelRatingBar.rating = rating
                ratingText = "(" + String.format("%.1f", rating) + ")"
            } catch (e: Exception) {
                Logs.warning("Library Activity", "Rating: " + item.rating, e)
            }
            itemView.novelRatingText.text = ratingText
        }
    }

//endregion

    override fun onDestroy() {
        super.onDestroy()
        async.cancelAll()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (adapter.items.isNotEmpty())
            outState.putSerializable("results", adapter.items)
    }

}

package io.github.gmathi.novellibrary.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.databinding.ContentRecyclerViewBinding
import io.github.gmathi.novellibrary.databinding.ListitemNovelBinding
import io.github.gmathi.novellibrary.extensions.*
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.network.*
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.getGlideUrl
import io.github.gmathi.novellibrary.util.setDefaults
import io.github.gmathi.novellibrary.util.system.isFragmentActive
import io.github.gmathi.novellibrary.util.system.startNovelDetailsActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.util.concurrent.atomic.AtomicBoolean


class SearchTermFragment : BaseFragment(), GenericAdapter.Listener<Novel>, GenericAdapter.LoadMoreListener {


    override var currentPageNumber: Int = 1
    override val preloadCount: Int = 50
    override val isPageLoading: AtomicBoolean = AtomicBoolean(false)
    private lateinit var searchTerm: String
    private lateinit var resultType: String
    
    private lateinit var binding: ContentRecyclerViewBinding

    companion object {
        private const val TAG = "SearchTermFragment"

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
        val view = inflater.inflate(R.layout.content_recycler_view, container, false) ?: return null
        binding = ContentRecyclerViewBinding.bind(view)
        return view
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

        binding.progressLayout.showLoading()
        searchNovels()
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = ArrayList(), layoutResId = R.layout.listitem_novel, listener = this, loadMoreListener = if (resultType != HostNames.WLN_UPDATES) this else null)
        binding.recyclerView.setDefaults(adapter)
        binding.swipeRefreshLayout.setOnRefreshListener { searchNovels() }
    }

    private fun searchNovels() {

        lifecycleScope.launch search@{

            if (!Utils.isConnectedToNetwork(activity)) {
                binding.progressLayout.noInternetError(View.OnClickListener {
                    binding.progressLayout.showLoading()
                    currentPageNumber = 1
                    searchNovels()
                })
                return@search
            }

            val searchTerms = URLEncoder.encode(searchTerm, "UTF-8")
            var results: ArrayList<Novel>? = null

            try {
                when (resultType) {
                    HostNames.NOVEL_UPDATES -> results = withContext(Dispatchers.IO) { NovelApi.searchNovelUpdates(searchTerms, currentPageNumber) }
                    HostNames.ROYAL_ROAD -> results = withContext(Dispatchers.IO) { NovelApi.searchRoyalRoad(searchTerms, currentPageNumber) }
                    HostNames.NOVEL_FULL -> results = withContext(Dispatchers.IO) { NovelApi.searchNovelFull(searchTerms, currentPageNumber) }
                    HostNames.WLN_UPDATES -> results = withContext(Dispatchers.IO) { NovelApi.searchWlnUpdates(searchTerms) }
                    HostNames.SCRIBBLE_HUB -> results = withContext(Dispatchers.IO) { NovelApi.searchScribbleHub(searchTerms, currentPageNumber) }
                    HostNames.LNMTL -> results = withContext(Dispatchers.IO) { NovelApi.searchLNMTL(searchTerms) }
                    HostNames.NEOVEL -> results =  withContext(Dispatchers.IO) { NovelApi.searchNeovel(searchTerms) }
                }
            } catch (e: Exception) {
                Logs.error(TAG, "Search Failed!", e)
            }

            if (results != null) {
                if (isVisible && (!isDetached || !isRemoving)) {
                    loadSearchResults(results)
                    isPageLoading.lazySet(false)
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            } else {
                if (isFragmentActive() && binding.progressLayout != null)
                    binding.progressLayout.showError(errorText = getString(R.string.connection_error), buttonText = getString(R.string.try_again), onClickListener = View.OnClickListener {
                        binding.progressLayout.showLoading()
                        currentPageNumber = 1
                        searchNovels()
                    })
            }
        }
    }

    private fun loadSearchResults(results: ArrayList<Novel>) {
        if (results.isNotEmpty() && !adapter.items.containsAll(results)) {
            if (currentPageNumber == 1) {
                adapter.updateData(results)
            } else {
                adapter.addItems(results)
            }
        } else {
            adapter.loadMoreListener = null
            adapter.notifyDataSetChanged()
        }

        if (adapter.items.isEmpty()) {
            if (isFragmentActive() && binding.progressLayout != null)
                binding.progressLayout.showEmpty(
                    resId = R.raw.empty_search,
                    isLottieAnimation = true,
                    emptyText = "No Novels Found!",
                    buttonText = getString(R.string.try_again),
                    onClickListener = View.OnClickListener {
                        binding.progressLayout.showLoading()
                        currentPageNumber = 1
                        searchNovels()
                    })
        } else {
            if (isFragmentActive() && binding.progressLayout != null)
                binding.progressLayout.showContent()
        }
    }

    override fun loadMore() {
        if (isPageLoading.compareAndSet(false, true)) {
            currentPageNumber++
            searchNovels()
        }
    }

    //region Adapter Listener Methods - onItemClick(), viewBinder()

    override fun onItemClick(item: Novel, position: Int) {
        (activity as? AppCompatActivity)?.startNovelDetailsActivity(item, false)
        //addToDownloads(item)
    }

    override fun bind(item: Novel, itemView: View, position: Int) {
        val itemBinding = ListitemNovelBinding.bind(itemView)
        itemBinding.novelImageView.setImageResource(android.R.color.transparent)

        if (!item.imageUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(item.imageUrl?.getGlideUrl())
                .apply(RequestOptions.circleCropTransform())
                .into(itemBinding.novelImageView)
        }

        //Other Data Fields
        itemBinding.novelTitleTextView.text = item.name
        itemBinding.novelTitleTextView.isSelected = dataCenter.enableScrollingText

        if (item.rating != null && item.rating != "N/A") {
            var ratingText = "(N/A)"
            try {
                val rating = item.rating!!.toFloat()
                itemBinding.novelRatingBar.rating = rating
                ratingText = "(" + String.format("%.1f", rating) + ")"
            } catch (e: Exception) {
                Logs.warning(TAG, "Rating: " + item.rating, e)
            }
            itemBinding.novelRatingText.text = ratingText
        }
    }

//endregion

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::adapter.isInitialized && adapter.items.isNotEmpty())
            outState.putSerializable("results", adapter.items)
    }

}

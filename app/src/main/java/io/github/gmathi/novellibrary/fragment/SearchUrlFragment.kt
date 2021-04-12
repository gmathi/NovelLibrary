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
import io.github.gmathi.novellibrary.databinding.ContentRecyclerViewBinding
import io.github.gmathi.novellibrary.databinding.ListitemNovelBinding
import io.github.gmathi.novellibrary.extensions.*
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.source.NovelUpdatesSource
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.extensions.getGlideUrl
import io.github.gmathi.novellibrary.extensions.setDefaults
import io.github.gmathi.novellibrary.util.system.isFragmentActive
import io.github.gmathi.novellibrary.util.system.startNovelDetailsActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean


class SearchUrlFragment : BaseFragment(), GenericAdapter.Listener<Novel>, GenericAdapter.LoadMoreListener {

    override var currentPageNumber: Int = 1
    override val preloadCount: Int = 50
    override val isPageLoading: AtomicBoolean = AtomicBoolean(false)

    private var rank: String? = null
    private var url: String? = null

    private lateinit var binding: ContentRecyclerViewBinding

    companion object {
        private const val TAG = "SearchUrlFragment"

        fun newInstance(rank: String?, url: String?): SearchUrlFragment {
            val bundle = Bundle()
            bundle.putString("rank", rank)
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.content_recycler_view, container, false) ?: return null
        binding = ContentRecyclerViewBinding.bind(view)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        //(activity as AppCompatActivity).setSupportActionBar(null)
        rank = arguments?.getString("rank")
        url = arguments?.getString("url")

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
        adapter = GenericAdapter(items = ArrayList(), layoutResId = R.layout.listitem_novel, listener = this, loadMoreListener = this)
        binding.recyclerView.setDefaults(adapter)
        binding.swipeRefreshLayout.setOnRefreshListener { searchNovels() }
    }

    private fun searchNovels() {

        lifecycleScope.launch search@{

            if (!networkHelper.isConnectedToNetwork()) {
                binding.progressLayout.noInternetError {
                    binding.progressLayout.showLoading()
                    currentPageNumber = 1
                    searchNovels()
                }
                return@search
            }
            try {
                val novelsPage = withContext(Dispatchers.IO) { NovelUpdatesSource().getPopularNovels(rank, url, currentPageNumber) }
                if (isFragmentActive()) {
                    loadSearchResults(ArrayList(novelsPage.novels))
                    isPageLoading.lazySet(false)
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            } catch (e: Exception) {
                if (isFragmentActive())
                    binding.progressLayout.showError(errorText = getString(R.string.connection_error), buttonText = getString(R.string.try_again)) {
                        binding.progressLayout.showLoading()
                        currentPageNumber = 1
                        searchNovels()
                    }
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
        }

        if (adapter.items.isEmpty()) {
            if (isFragmentActive())
                binding.progressLayout.showEmpty(
                    resId = R.raw.empty_search,
                    isLottieAnimation = true,
                    emptyText = "No Novels Found!",
                    buttonText = getString(R.string.try_again)
                ) {
                    binding.progressLayout.showLoading()
                    currentPageNumber = 1
                    searchNovels()
                }
        } else {
            if (isFragmentActive())
                binding.progressLayout.showContent()
        }
    }

//region Adapter Listener Methods - onItemClick(), viewBinder()

    override fun onItemClick(item: Novel, position: Int) {
        (activity as? AppCompatActivity)?.startNovelDetailsActivity(item, false)
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

        if (item.metadata.containsKey("OriginMarker")) {
            itemBinding.novelLanguageText.text = item.metadata["OriginMarker"]
            itemBinding.novelLanguageText.visibility = View.VISIBLE
        }

        if (item.rating != null) {
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

    override fun loadMore() {
        if (isPageLoading.compareAndSet(false, true)) {
            currentPageNumber++
            searchNovels()
        }
    }

//endregion

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::adapter.isInitialized && adapter.items.isNotEmpty())
            outState.putSerializable("results", adapter.items)
    }


}

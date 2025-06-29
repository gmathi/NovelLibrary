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
import io.github.gmathi.novellibrary.model.other.NovelsPage
import io.github.gmathi.novellibrary.model.source.CatalogueSource
import io.github.gmathi.novellibrary.network.*
import io.github.gmathi.novellibrary.util.*
import io.github.gmathi.novellibrary.util.Exceptions.MISSING_SOURCE_ID
import io.github.gmathi.novellibrary.util.lang.getGlideUrl
import io.github.gmathi.novellibrary.util.system.isFragmentActive
import io.github.gmathi.novellibrary.util.system.startNovelDetailsActivity
import io.github.gmathi.novellibrary.util.view.setDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean


class SearchTermFragment : BaseFragment(), GenericAdapter.Listener<Novel>, GenericAdapter.LoadMoreListener {


    override var currentPageNumber: Int = 1
    override val preloadCount: Int = 25
    override val isPageLoading: AtomicBoolean = AtomicBoolean(false)
    private lateinit var searchTerm: String
    private var sourceId: Long = 0L

    private lateinit var binding: ContentRecyclerViewBinding

    companion object {
        private const val TAG = "SearchTermFragment"

        fun newInstance(searchTerms: String, sourceId: Long): SearchTermFragment {
            val bundle = Bundle()
            bundle.putString("searchTerm", searchTerms)
            bundle.putLong("sourceId", sourceId)
            val fragment = SearchTermFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    lateinit var adapter: GenericAdapter<Novel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setHasOptionsMenu is deprecated, using modern menu approach
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.content_recycler_view, container, false) ?: return null
        binding = ContentRecyclerViewBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchTerm = arguments?.getString("searchTerm")!!
        sourceId = arguments?.getLong("sourceId")!!

        setRecyclerView()

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("results")) {
                @Suppress("UNCHECKED_CAST")
                val results = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    savedInstanceState.getSerializable("results", ArrayList::class.java) as? ArrayList<Novel>
                } else {
                    @Suppress("DEPRECATION")
                    savedInstanceState.getSerializable("results") as? ArrayList<Novel>
                }
                results?.let {
                    adapter.updateData(it)
                    return
                }
            }
        }

        // Ensure fragment is active before starting search
        if (isFragmentActive()) {
            binding.progressLayout.showLoading()
            searchNovels()
        } else {
            // Delay the search until the fragment is active
            view.post {
                if (isFragmentActive()) {
                    binding.progressLayout.showLoading()
                    searchNovels()
                }
            }
        }
    }

    private fun setRecyclerView() {
        val enableLoadMoreListener = (sourceId != Constants.SourceId.WLN_UPDATES && sourceId != Constants.SourceId.LNMTL)
        adapter = GenericAdapter(items = ArrayList(), layoutResId = R.layout.listitem_novel, listener = this, loadMoreListener = if (enableLoadMoreListener) this else null)
        binding.recyclerView.setDefaults(adapter)
        binding.swipeRefreshLayout.setOnRefreshListener { searchNovels() }
    }

    private fun searchNovels() {

        lifecycleScope.launch search@{

            if (!networkHelper.isConnectedToNetwork()) {
                binding.swipeRefreshLayout.isRefreshing = false
                binding.progressLayout.noInternetError {
                    binding.progressLayout.showLoading()
                    currentPageNumber = 1
                    searchNovels()
                }
                return@search
            }

            try {
                val source = sourceManager.get(sourceId) as? CatalogueSource ?: throw Exception("$MISSING_SOURCE_ID: $sourceId")
                
                val novelsPage = withContext(Dispatchers.IO) { source.getSearchNovels(currentPageNumber, searchTerm) }
                
                if (isFragmentActive()) {
                    loadSearchResults(novelsPage)
                    isPageLoading.lazySet(false)
                } else {
                    // Fragment not active, skipping results update
                }

            } catch (e: Exception) {
                if (isFragmentActive()) {
                    val errorMessage = Exceptions.getErrorMessage(e, requireContext())
                    binding.progressLayout.showError(errorText = errorMessage, buttonText = getString(R.string.try_again)) {
                        binding.progressLayout.showLoading()
                        currentPageNumber = 1
                        searchNovels()
                    }
                }
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun loadSearchResults(novelsPage: NovelsPage) {
        val results: ArrayList<Novel> = ArrayList(novelsPage.novels)
        
        if (!novelsPage.hasNextPage) {
            adapter.loadMoreListener = null
        }

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
                binding.progressLayout.showEmpty(R.raw.empty_search, true, "No Novels Found!", getString(R.string.try_again)) {
                    binding.progressLayout.showLoading()
                    currentPageNumber = 1
                    searchNovels()
                }
        } else {
            if (isFragmentActive())
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

        if (item.rating != null && item.rating != "N/A") {
            var ratingText = "(N/A)"
            try {
                val rating = item.rating!!.toFloat()
                itemBinding.novelRatingBar.rating = rating
                ratingText = "(" + String.format("%.1f", rating) + ")"
            } catch (e: Exception) {
                // Rating: " + item.rating, e)
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

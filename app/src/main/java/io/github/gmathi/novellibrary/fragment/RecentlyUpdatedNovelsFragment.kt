package io.github.gmathi.novellibrary.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.databinding.ContentRecyclerViewBinding
import io.github.gmathi.novellibrary.databinding.ListitemTitleSubtitleBinding
import io.github.gmathi.novellibrary.extensions.noInternetError
import io.github.gmathi.novellibrary.extensions.showLoading
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.other.RecentlyUpdatedItem
import io.github.gmathi.novellibrary.network.WebPageDocumentFetcher
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.system.startNovelDetailsActivity
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.view.extensions.applyFont
import io.github.gmathi.novellibrary.util.view.setDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecentlyUpdatedNovelsFragment : BaseFragment(), GenericAdapter.Listener<RecentlyUpdatedItem> {

    companion object {
        fun newInstance() = RecentlyUpdatedNovelsFragment()
    }

    private lateinit var binding: ContentRecyclerViewBinding
    private lateinit var adapter: GenericAdapter<RecentlyUpdatedItem>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.content_recycler_view, container, false)
        binding = ContentRecyclerViewBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.progressLayout.showLoading()
        setRecyclerView()
        getRecentlyUpdatedNovels()
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = ArrayList(), layoutResId = R.layout.listitem_title_subtitle, listener = this)
        binding.recyclerView.setDefaults(adapter)
        binding.recyclerView.addItemDecoration(CustomDividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL))
        binding.swipeRefreshLayout.setOnRefreshListener { getRecentlyUpdatedNovels() }
    }

    private fun getRecentlyUpdatedNovels() {
        lifecycleScope.launch {
            if (!networkHelper.isConnectedToNetwork()) {
                if (adapter.items.isEmpty())
                    binding.progressLayout.noInternetError {
                        binding.progressLayout.showLoading()
                        getRecentlyUpdatedNovels()
                    }
                return@launch
            }

            val items = withContext(Dispatchers.IO) { fetchRecentlyUpdatedNovels() } ?: return@launch
            adapter.updateData(items)
            binding.progressLayout.showContent()
            binding.swipeRefreshLayout.isRefreshing = false

        }
    }

    private fun fetchRecentlyUpdatedNovels(): ArrayList<RecentlyUpdatedItem>? {
        var searchResults: ArrayList<RecentlyUpdatedItem>? = null
        try {
            searchResults = ArrayList()
            val document = WebPageDocumentFetcher.document("https://www.novelupdates.com/")
            document.body().select("table#myTable > tbody > tr").forEach { element ->
                val item = RecentlyUpdatedItem()
                item.novelUrl = element.selectFirst("a[href]")?.attr("abs:href")
                item.novelName = element.child(0).text()
                item.chapterName = element.child(1).text()
                item.publisherName = element.child(2).text()
                searchResults.add(item)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return searchResults
    }

    @SuppressLint("SetTextI18n")
    override fun bind(item: RecentlyUpdatedItem, itemView: View, position: Int) {
        val itemBinding = ListitemTitleSubtitleBinding.bind(itemView)
        itemBinding.chevron.visibility = View.VISIBLE

        itemBinding.title.applyFont(requireActivity().assets).text = item.novelName
        itemBinding.subtitle.applyFont(requireActivity().assets).text = "${item.chapterName} [ ${item.publisherName} ]"

        itemView.setBackgroundColor(
            if (position % 2 == 0) ContextCompat.getColor(requireContext(), R.color.black_transparent)
            else ContextCompat.getColor(requireContext(), android.R.color.transparent)
        )
    }

    override fun onItemClick(item: RecentlyUpdatedItem, position: Int) {
        if (item.novelName != null && item.novelUrl != null) {
            requireActivity().startNovelDetailsActivity(Novel(item.novelName!!, item.novelUrl!!, Constants.SourceId.NOVEL_UPDATES))
        }
    }

}
package io.github.gmathi.novellibrary.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.databinding.ActivityRecentlyUpdatedNovelsBinding
import io.github.gmathi.novellibrary.databinding.ListitemTitleSubtitleBinding
import io.github.gmathi.novellibrary.extensions.noInternetError
import io.github.gmathi.novellibrary.extensions.showLoading
import io.github.gmathi.novellibrary.util.system.startNovelDetailsActivity
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.other.RecentlyUpdatedItem
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.network.getRecentlyUpdatedNovels
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.applyFont
import io.github.gmathi.novellibrary.util.setDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecentlyUpdatedNovelsActivity : BaseActivity(), GenericAdapter.Listener<RecentlyUpdatedItem> {

    lateinit var adapter: GenericAdapter<RecentlyUpdatedItem>

    private lateinit var binding: ActivityRecentlyUpdatedNovelsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRecentlyUpdatedNovelsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.contentRecyclerView.progressLayout.showLoading()
        setRecyclerView()
        getRecentlyUpdatedNovels()
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = ArrayList(), layoutResId = R.layout.listitem_title_subtitle, listener = this)
        binding.contentRecyclerView.recyclerView.setDefaults(adapter)
        binding.contentRecyclerView.recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.contentRecyclerView.swipeRefreshLayout.setOnRefreshListener { getRecentlyUpdatedNovels() }
    }

    private fun getRecentlyUpdatedNovels() {
        lifecycleScope.launch {
            if (!Utils.isConnectedToNetwork(this@RecentlyUpdatedNovelsActivity)) {
                if (adapter.items.isEmpty())
                    binding.contentRecyclerView.progressLayout.noInternetError {
                        binding.contentRecyclerView.progressLayout.showLoading()
                        getRecentlyUpdatedNovels()
                    }
                return@launch
            }

            val items = withContext(Dispatchers.IO) { NovelApi.getRecentlyUpdatedNovels() } ?: return@launch
            adapter.updateData(items)
            binding.contentRecyclerView.progressLayout.showContent()
            binding.contentRecyclerView.swipeRefreshLayout.isRefreshing = false

        }
    }

    @SuppressLint("SetTextI18n")
    override fun bind(item: RecentlyUpdatedItem, itemView: View, position: Int) {
        val itemBinding = ListitemTitleSubtitleBinding.bind(itemView)
        itemBinding.chevron.visibility = View.VISIBLE

        itemBinding.title.applyFont(assets).text = item.novelName
        itemBinding.subtitle.applyFont(assets).text = "${item.chapterName} [ ${item.publisherName} ]"

        itemView.setBackgroundColor(
            if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
            else ContextCompat.getColor(this, android.R.color.transparent)
        )
    }

    override fun onItemClick(item: RecentlyUpdatedItem, position: Int) {
        if (item.novelName != null && item.novelUrl != null) {
            startNovelDetailsActivity(Novel(item.novelName!!, item.novelUrl!!))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
    //endregion

}

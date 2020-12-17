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
import kotlinx.android.synthetic.main.activity_recently_updated_novels.*
import kotlinx.android.synthetic.main.content_recycler_view.*
import kotlinx.android.synthetic.main.listitem_title_subtitle.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecentlyUpdatedNovelsActivity : BaseActivity(), GenericAdapter.Listener<RecentlyUpdatedItem> {

    lateinit var adapter: GenericAdapter<RecentlyUpdatedItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recently_updated_novels)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        progressLayout.showLoading()
        setRecyclerView()
        getRecentlyUpdatedNovels()
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = ArrayList(), layoutResId = R.layout.listitem_title_subtitle, listener = this)
        recyclerView.setDefaults(adapter)
        recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        swipeRefreshLayout.setOnRefreshListener { getRecentlyUpdatedNovels() }
    }

    private fun getRecentlyUpdatedNovels() {
        lifecycleScope.launch {
            if (!Utils.isConnectedToNetwork(this@RecentlyUpdatedNovelsActivity)) {
                if (adapter.items.isEmpty())
                    progressLayout.noInternetError {
                        progressLayout.showLoading()
                        getRecentlyUpdatedNovels()
                    }
                return@launch
            }

            val items = withContext(Dispatchers.IO) { NovelApi.getRecentlyUpdatedNovels() } ?: return@launch
            adapter.updateData(items)
            progressLayout.showContent()
            swipeRefreshLayout.isRefreshing = false

        }
    }

    @SuppressLint("SetTextI18n")
    override fun bind(item: RecentlyUpdatedItem, itemView: View, position: Int) {
        itemView.chevron.visibility = View.VISIBLE

        itemView.title.applyFont(assets).text = item.novelName
        itemView.subtitle.applyFont(assets).text = "${item.chapterName} [ ${item.publisherName} ]"

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

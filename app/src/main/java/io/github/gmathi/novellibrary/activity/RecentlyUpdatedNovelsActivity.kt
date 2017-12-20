package io.github.gmathi.novellibrary.activity

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.view.MenuItem
import android.view.View
import co.metalab.asyncawait.async
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.RecenlytUpdatedItem
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.network.getRecentlyUpdatedNovels
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.applyFont
import io.github.gmathi.novellibrary.util.setDefaults
import kotlinx.android.synthetic.main.activity_recently_updated_novels.*
import kotlinx.android.synthetic.main.content_recycler_view.*
import kotlinx.android.synthetic.main.listitem_title_subtitle.view.*

class RecentlyUpdatedNovelsActivity : AppCompatActivity(), GenericAdapter.Listener<RecenlytUpdatedItem> {

    lateinit var adapter: GenericAdapter<RecenlytUpdatedItem>

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
        recyclerView.addItemDecoration(object : DividerItemDecoration(this, DividerItemDecoration.VERTICAL) {

            override fun getItemOffsets(outRect: Rect?, view: View?, parent: RecyclerView?, state: RecyclerView.State?) {
                val position = parent?.getChildAdapterPosition(view)
                if (position == parent?.adapter?.itemCount?.minus(1)) {
                    outRect?.setEmpty()
                } else {
                    super.getItemOffsets(outRect, view, parent, state)
                }
            }
        })
        swipeRefreshLayout.setOnRefreshListener { getRecentlyUpdatedNovels() }
    }

    private fun getRecentlyUpdatedNovels() {
        async {

            if (!Utils.checkNetwork(this@RecentlyUpdatedNovelsActivity)) {
                if (adapter.items.isEmpty())
                    progressLayout.showError(ContextCompat.getDrawable(this@RecentlyUpdatedNovelsActivity, R.drawable.ic_warning_white_vector), getString(R.string.no_internet), getString(R.string.try_again), {
                        progressLayout.showLoading()
                        getRecentlyUpdatedNovels()
                    })
                return@async
            }

            val items = await { NovelApi().getRecentlyUpdatedNovels() } ?: return@async
            adapter.updateData(items)
            progressLayout.showContent()
            swipeRefreshLayout.isRefreshing = false

        }
    }

    @SuppressLint("SetTextI18n")
    override fun bind(item: RecenlytUpdatedItem, itemView: View, position: Int) {
        itemView.chevron.visibility = View.VISIBLE

        itemView.title.applyFont(assets).text = item.novelName
        itemView.subtitle.applyFont(assets).text = "${item.chapterName} [ ${item.publisherName} ]"

        itemView.setBackgroundColor(if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
        else ContextCompat.getColor(this, android.R.color.transparent))
    }

    override fun onItemClick(item: RecenlytUpdatedItem) {
        if (item.novelName != null && item.novelUrl != null) {
            startNovelDetailsActivity(Novel(item.novelName!!, item.novelUrl!!))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
    //endregion

}

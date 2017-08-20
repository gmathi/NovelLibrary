package io.github.gmathi.novellibrary.activity

import android.graphics.Rect
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.view.MenuItem
import android.view.View
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.util.applyFont
import io.github.gmathi.novellibrary.util.setDefaults
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.content_recycler_view.*
import kotlinx.android.synthetic.main.listitem_settings.view.*

class MentionSettingsActivity : BaseActivity(), GenericAdapter.Listener<String> {

    lateinit var adapter: GenericAdapter<String>
    private lateinit var mentionSettingsItems: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setRecyclerView()
    }

    private fun setRecyclerView() {
        mentionSettingsItems = ArrayList(resources.getStringArray(R.array.mention_settings_list).asList())
        adapter = GenericAdapter(items = mentionSettingsItems, layoutResId = R.layout.listitem_settings, listener = this)
        recyclerView.setDefaults(adapter)
        recyclerView.addItemDecoration(object : DividerItemDecoration(this, VERTICAL) {

            override fun getItemOffsets(outRect: Rect?, view: View?, parent: RecyclerView?, state: RecyclerView.State?) {
                val position = parent?.getChildAdapterPosition(view)
                if (position == parent?.adapter?.itemCount?.minus(1)) {
                    outRect?.setEmpty()
                } else {
                    super.getItemOffsets(outRect, view, parent, state)
                }
            }
        })
        swipeRefreshLayout.isEnabled = false
    }

    override fun bind(item: String, itemView: View, position: Int) {
        itemView.settingsTitle.applyFont(assets).text = item
        itemView.chevron.visibility = View.VISIBLE
        itemView.setBackgroundColor(if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
        else ContextCompat.getColor(this, android.R.color.transparent))
    }

    override fun onItemClick(item: String) {
        when (item) {
            getString(R.string.languages_supported) -> startLanguagesActivity()
            getString(R.string.copyright_notice) -> startCopyrightActivity()
            getString(R.string.libraries_used) -> startLibrariesUsedActivity()
            getString(R.string.contributions) -> startContributionsActivity()
        }
    }

    //region OptionsMenu
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
    //endregion

}
package io.github.gmathi.novellibrary.activity.settings

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.extensions.openInBrowser
import io.github.gmathi.novellibrary.model.Library
import io.github.gmathi.novellibrary.util.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.applyFont
import io.github.gmathi.novellibrary.util.setDefaults
import kotlinx.android.synthetic.main.activity_libraries_used.*
import kotlinx.android.synthetic.main.content_recycler_view.*
import kotlinx.android.synthetic.main.listitem_title_subtitle.view.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class ContributionsActivity : BaseActivity(), GenericAdapter.Listener<Library> {

    lateinit var adapter: GenericAdapter<Library>
    private lateinit var contributors: ArrayList<Library>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_libraries_used)
        setSupportActionBar(toolbar)
        val contributorsList = getContributorsData()
        if (contributorsList == null) finish()
        contributors = ArrayList(contributorsList!!.filter { it != null }.map { it!! })
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setRecyclerView()
    }

    private fun getContributorsData(): ArrayList<Library?>? {
        val reader: BufferedReader
        val stringBuilder = StringBuilder()
        try {
            reader = BufferedReader(InputStreamReader(assets.open("contributors.json")))
            var mLine = reader.readLine()
            while (mLine != null) {
                stringBuilder.append(mLine)
                mLine = reader.readLine()
            }
            return Gson().fromJson(stringBuilder.toString(), object : TypeToken<ArrayList<Library>>() {}.type)
        } catch (e: IOException) {
            Logs.error("ContributionsActivity", e.localizedMessage, e)
        }
        return null
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = contributors, layoutResId = R.layout.listitem_title_subtitle, listener = this)
        recyclerView.setDefaults(adapter)
        recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        swipeRefreshLayout.isEnabled = false
    }

    override fun bind(item: Library, itemView: View, position: Int) {
        itemView.title.applyFont(assets).text = item.name
        itemView.subtitle.applyFont(assets).text = item.description
        itemView.chevron.visibility = if (!item.link.isNullOrBlank()) View.VISIBLE else View.INVISIBLE
        itemView.setBackgroundColor(if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
        else ContextCompat.getColor(this, android.R.color.transparent))
    }

    override fun onItemClick(item: Library, position: Int) {
        if (!item.link.isNullOrBlank())
            openInBrowser(item.link!!)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

}

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
import io.github.gmathi.novellibrary.databinding.ActivityLibrariesUsedBinding
import io.github.gmathi.novellibrary.databinding.ListitemTitleSubtitleBinding
import io.github.gmathi.novellibrary.util.system.openInBrowser
import io.github.gmathi.novellibrary.model.other.GenericJsonMappedModel
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.extensions.applyFont
import io.github.gmathi.novellibrary.extensions.setDefaults
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class ContributionsActivity : BaseActivity(), GenericAdapter.Listener<GenericJsonMappedModel> {

    lateinit var adapter: GenericAdapter<GenericJsonMappedModel>
    private lateinit var contributors: ArrayList<GenericJsonMappedModel>
    
    private lateinit var binding: ActivityLibrariesUsedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityLibrariesUsedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        val contributorsList = getContributorsData()
        if (contributorsList == null) finish()
        contributors = ArrayList(contributorsList!!.filter { it != null }.map { it!! })
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setRecyclerView()
    }

    private fun getContributorsData(): ArrayList<GenericJsonMappedModel?>? {
        val reader: BufferedReader
        val stringBuilder = StringBuilder()
        try {
            reader = BufferedReader(InputStreamReader(assets.open("contributors.json")))
            var mLine = reader.readLine()
            while (mLine != null) {
                stringBuilder.append(mLine)
                mLine = reader.readLine()
            }
            return Gson().fromJson(stringBuilder.toString(), object : TypeToken<ArrayList<GenericJsonMappedModel>>() {}.type)
        } catch (e: IOException) {
            Logs.error("ContributionsActivity", e.localizedMessage, e)
        }
        return null
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = contributors, layoutResId = R.layout.listitem_title_subtitle, listener = this)
        binding.contentRecyclerView.recyclerView.setDefaults(adapter)
        binding.contentRecyclerView.recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.contentRecyclerView.swipeRefreshLayout.isEnabled = false
    }

    override fun bind(item: GenericJsonMappedModel, itemView: View, position: Int) {
        val itemBinding = ListitemTitleSubtitleBinding.bind(itemView)
        itemBinding.title.applyFont(assets).text = item.name
        itemBinding.subtitle.applyFont(assets).text = item.description
        itemBinding.chevron.visibility = if (!item.link.isNullOrBlank()) View.VISIBLE else View.INVISIBLE
        itemView.setBackgroundColor(if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
        else ContextCompat.getColor(this, android.R.color.transparent))
    }

    override fun onItemClick(item: GenericJsonMappedModel, position: Int) {
        if (!item.link.isNullOrBlank())
            openInBrowser(item.link!!)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

}

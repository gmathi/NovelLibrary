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
import io.github.gmathi.novellibrary.model.other.GenericJsonMappedModel
import io.github.gmathi.novellibrary.util.logging.Logs
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.view.extensions.applyFont
import io.github.gmathi.novellibrary.util.view.setDefaults
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * "What's Changed" screen (About section). Renders the bundled `assets/changelog.json`
 * as a list of versions (title = version, subtitle = the changes for that version).
 * Add a new object at the TOP of changelog.json for each release.
 */
class WhatsChangedActivity : BaseActivity(), GenericAdapter.Listener<GenericJsonMappedModel> {

    lateinit var adapter: GenericAdapter<GenericJsonMappedModel>
    private lateinit var changelog: ArrayList<GenericJsonMappedModel>

    private lateinit var binding: ActivityLibrariesUsedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLibrariesUsedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.whats_changed)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val entries = getChangelogData()
        changelog = ArrayList(entries?.filterNotNull() ?: emptyList())
        setRecyclerView()
    }

    private fun getChangelogData(): ArrayList<GenericJsonMappedModel?>? {
        val stringBuilder = StringBuilder()
        try {
            val reader = BufferedReader(InputStreamReader(assets.open("changelog.json")))
            var line = reader.readLine()
            while (line != null) {
                stringBuilder.append(line)
                line = reader.readLine()
            }
            return Gson().fromJson(stringBuilder.toString(), object : TypeToken<ArrayList<GenericJsonMappedModel>>() {}.type)
        } catch (e: IOException) {
            Logs.error("WhatsChangedActivity", e.localizedMessage, e)
        }
        return null
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = changelog, layoutResId = R.layout.listitem_title_subtitle, listener = this)
        binding.contentRecyclerView.recyclerView.setDefaults(adapter)
        binding.contentRecyclerView.recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.contentRecyclerView.swipeRefreshLayout.isEnabled = false
    }

    override fun bind(item: GenericJsonMappedModel, itemView: View, position: Int) {
        val itemBinding = ListitemTitleSubtitleBinding.bind(itemView)
        itemBinding.title.applyFont(assets).text = getString(R.string.whats_changed_version, item.name ?: "")
        itemBinding.subtitle.applyFont(assets).text = item.description
        itemBinding.chevron.visibility = View.INVISIBLE
        itemView.setBackgroundColor(
            if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
            else ContextCompat.getColor(this, android.R.color.transparent)
        )
    }

    override fun onItemClick(item: GenericJsonMappedModel, position: Int) {
        // Display-only.
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
}

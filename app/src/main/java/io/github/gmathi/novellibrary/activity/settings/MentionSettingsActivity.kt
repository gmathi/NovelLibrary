package io.github.gmathi.novellibrary.activity.settings

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import android.view.MenuItem
import android.view.View
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.*
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.databinding.ActivitySettingsBinding
import io.github.gmathi.novellibrary.databinding.ListitemSettingsBinding
import io.github.gmathi.novellibrary.util.system.startContributionsActivity
import io.github.gmathi.novellibrary.util.system.startCopyrightActivity
import io.github.gmathi.novellibrary.util.system.startLanguagesActivity
import io.github.gmathi.novellibrary.util.system.startLibrariesUsedActivity
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.extensions.applyFont
import io.github.gmathi.novellibrary.extensions.setDefaults

class MentionSettingsActivity : BaseActivity(), GenericAdapter.Listener<String> {

    lateinit var adapter: GenericAdapter<String>
    private lateinit var mentionSettingsItems: ArrayList<String>
    
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setRecyclerView()
    }

    private fun setRecyclerView() {
        mentionSettingsItems = ArrayList(resources.getStringArray(R.array.mention_settings_list).asList())
        adapter = GenericAdapter(items = mentionSettingsItems, layoutResId = R.layout.listitem_settings, listener = this)
        binding.contentRecyclerView.recyclerView.setDefaults(adapter)
        binding.contentRecyclerView.recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.contentRecyclerView.swipeRefreshLayout.isEnabled = false
    }

    override fun bind(item: String, itemView: View, position: Int) {
        val itemBinding = ListitemSettingsBinding.bind(itemView)
        itemBinding.settingsTitle.applyFont(assets).text = item
        itemBinding.chevron.visibility = View.VISIBLE
        itemView.setBackgroundColor(if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
        else ContextCompat.getColor(this, android.R.color.transparent))
    }

    override fun onItemClick(item: String, position: Int) {
        when (item) {
            getString(R.string.languages_supported) -> startLanguagesActivity()
            getString(R.string.copyright_notice) -> startCopyrightActivity()
            getString(R.string.libraries_used) -> startLibrariesUsedActivity()
            getString(R.string.contributions) -> startContributionsActivity()
        }
    }

    //region OptionsMenu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
    //endregion

}
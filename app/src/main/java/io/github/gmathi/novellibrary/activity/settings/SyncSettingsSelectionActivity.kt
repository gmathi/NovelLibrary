//package io.github.gmathi.novellibrary.activity.settings
//
//import android.os.Bundle
//import android.view.MenuItem
//import android.view.View
//import androidx.core.content.ContextCompat
//import androidx.recyclerview.widget.DividerItemDecoration
//import io.github.gmathi.novellibrary.R
//import io.github.gmathi.novellibrary.activity.BaseActivity
//import io.github.gmathi.novellibrary.adapter.GenericAdapter
//import io.github.gmathi.novellibrary.dataCenter
//import io.github.gmathi.novellibrary.databinding.ActivitySettingsBinding
//import io.github.gmathi.novellibrary.databinding.ListitemTitleSubtitleBinding
//import io.github.gmathi.novellibrary.util.system.startSyncSettingsActivity
//import io.github.gmathi.novellibrary.network.HostNames
//import io.github.gmathi.novellibrary.network.sync.NovelSync
//import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
//import io.github.gmathi.novellibrary.util.applyFont
//import io.github.gmathi.novellibrary.util.setDefaults
//import java.util.ArrayList
//
//class SyncSettingsSelectionActivity : BaseActivity(), GenericAdapter.Listener<String> {
//
//    lateinit var adapter: GenericAdapter<String>
//    private lateinit var settingsItems: ArrayList<String>
//
//    private lateinit var binding: ActivitySettingsBinding
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        binding = ActivitySettingsBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//        setSupportActionBar(binding.toolbar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        setRecyclerView()
//    }
//
//    override fun onResume() {
//        super.onResume()
//        adapter.notifyDataSetChanged()
//    }
//
//    private fun setRecyclerView() {
//        settingsItems = ArrayList(resources.getStringArray(R.array.sync_list).asList())
//        adapter = GenericAdapter(items = settingsItems, layoutResId = R.layout.listitem_title_subtitle, listener = this)
//        binding.contentRecyclerView.recyclerView.setDefaults(adapter)
//        binding.contentRecyclerView.recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
//        binding.contentRecyclerView.swipeRefreshLayout.isEnabled = false
//    }
//
//    override fun bind(item: String, itemView: View, position: Int) {
//        val itemBinding = ListitemTitleSubtitleBinding.bind(itemView)
//        itemBinding.title.applyFont(assets).text = item
//        val host = when(item) {
//            getString(R.string.source_novel_updates) -> HostNames.NOVEL_UPDATES
//            else -> HostNames.NOVEL_UPDATES
//        }
//
//        if (position == 0) {
//            // Disclaimer
//            itemBinding.subtitle.applyFont(assets).text = getString(R.string.experimental_sync_description)
//            itemBinding.chevron.setImageResource(R.drawable.ic_info_white_vector)
//            itemBinding.chevron.imageTintList = ContextCompat.getColorStateList(this, R.color.colorStateBlue)
//        } else {
//            val loggedIn = getString(if (NovelSync.getInstance(host, true)?.loggedIn() == true) R.string.logged_in else R.string.not_logged_in)
//            val enabled = getString(if(dataCenter.getSyncEnabled(host)) R.string.enabled else R.string.disabled)
//
//            itemBinding.subtitle.applyFont(assets).text = getString(R.string.sync_status_description, loggedIn, enabled)
//        }
//
//        itemView.setBackgroundColor(if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
//        else ContextCompat.getColor(this, android.R.color.transparent))
//    }
//
//    override fun onItemClick(item: String, position: Int) {
//        when(item) {
//            getString(R.string.source_novel_updates) -> startSyncSettingsActivity(HostNames.NOVEL_UPDATES)
//        }
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        if (item.itemId == android.R.id.home) finish()
//        return super.onOptionsItemSelected(item)
//    }
//}
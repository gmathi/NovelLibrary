package io.github.gmathi.novellibrary.activity.settings

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.databinding.ActivitySettingsBinding
import io.github.gmathi.novellibrary.databinding.ListitemTitleSubtitleWidgetBinding
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.util.view.extensions.applyFont
import io.github.gmathi.novellibrary.util.view.setDefaults
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import kotlin.math.ceil

/**
 * Lists the domains currently forced onto the WebView fetcher (because the API path failed
 * twice). Each row shows the host and how long until the flag self-heals; toggling the switch
 * off clears the forced-WebView flag for that host, re-enabling the API fetch path for it.
 */
class ForcedWebViewDomainsActivity : BaseActivity(), GenericAdapter.Listener<String> {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var adapter: GenericAdapter<String>

    // host -> flaggedAtMillis snapshot for the currently-displayed rows.
    private var domainTimestamps: Map<String, Long> = emptyMap()
    private lateinit var hosts: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.forced_webview_domains)
        setRecyclerView()
    }

    private fun setRecyclerView() {
        domainTimestamps = dataCenter.getForceWebViewDomains()
        hosts = ArrayList(domainTimestamps.keys.sorted())

        adapter = GenericAdapter(items = hosts, layoutResId = R.layout.listitem_title_subtitle_widget, listener = this)
        binding.contentRecyclerView.recyclerView.setDefaults(adapter)
        binding.contentRecyclerView.recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.contentRecyclerView.swipeRefreshLayout.isEnabled = false
    }

    override fun bind(item: String, itemView: View, position: Int) {
        val itemBinding = ListitemTitleSubtitleWidgetBinding.bind(itemView)
        itemBinding.widgetChevron.visibility = View.INVISIBLE
        itemBinding.currentValue.visibility = View.INVISIBLE
        itemBinding.blackOverlay.visibility = View.INVISIBLE

        itemBinding.title.applyFont(assets).text = item
        itemBinding.subtitle.applyFont(assets).text = autoClearText(item)

        itemBinding.widgetSwitch.setOnCheckedChangeListener(null)
        itemBinding.widgetSwitch.visibility = View.VISIBLE
        // On = still forced. Toggling off clears the flag and removes the row.
        itemBinding.widgetSwitch.isChecked = true
        itemBinding.widgetSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                dataCenter.clearWebViewForced(item)
                adapter.removeItem(item)
            }
        }
    }

    /** Human-readable remaining time before the 15-day self-heal clears this host. */
    private fun autoClearText(host: String): String {
        val flaggedAt = domainTimestamps[host] ?: return ""
        val remainingMs = DataCenter.FORCE_WEBVIEW_EXPIRY_MS - (System.currentTimeMillis() - flaggedAt)
        if (remainingMs <= 0) return getString(R.string.forced_webview_domain_expires_today)
        val days = ceil(remainingMs / (24.0 * 60 * 60 * 1000)).toInt()
        return if (days <= 0) getString(R.string.forced_webview_domain_expires_today)
        else getString(R.string.forced_webview_domain_auto_clear, days)
    }

    override fun onItemClick(item: String, position: Int) {
        // No drill-in; the switch is the only interaction.
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(menuItem)
    }
}

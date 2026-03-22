package io.github.gmathi.novellibrary.activity.settings

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.analytics.FirebaseAnalytics
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.common.R as CommonR
import io.github.gmathi.novellibrary.common.adapter.GenericAdapter
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.databinding.ActivitySettingsBinding
import io.github.gmathi.novellibrary.common.databinding.ListitemTitleSubtitleWidgetBinding
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.common.model.ListitemSetting
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.lang.LocaleManager
import io.github.gmathi.novellibrary.util.system.bindSettingListitemDefaults
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.view.setDefaults
import uy.kohesive.injekt.injectLazy

open class BaseSettingsActivity<V, T: ListitemSetting<V>>(val options: List<T>) : io.github.gmathi.novellibrary.core.activity.settings.BaseSettingsActivity(), GenericAdapter.Listener<T> {

    lateinit var adapter: GenericAdapter<T>

    protected lateinit var binding: ActivitySettingsBinding

    // Implement DataAccessor dependencies
    override val firebaseAnalytics: FirebaseAnalytics by injectLazy()
    override val dataCenter: DataCenter by injectLazy()
    override val dbHelper: DBHelper by injectLazy()
    override val sourceManager: SourceManager by injectLazy()
    override val networkHelper: NetworkHelper by injectLazy()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Call setupSettingsRecyclerView after binding is initialized
        setupSettingsRecyclerView()
    }

    override fun setupEdgeToEdge() {
        // Enable edge-to-edge display
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    override fun applyWindowInsets() {
        // No-op for settings activities - they handle insets through toolbar
    }

    override fun getLocaleContext(context: Context): Context {
        return LocaleManager.updateContextLocale(context)
    }

    override fun getSettingsItems(): List<Any> {
        return options
    }

    override fun setupSettingsRecyclerView() {
        adapter = GenericAdapter(items = ArrayList(options), layoutResId = CommonR.layout.listitem_title_subtitle_widget, listener = this)
        binding.contentRecyclerView.recyclerView.setDefaults(adapter)
        binding.contentRecyclerView.recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.contentRecyclerView.swipeRefreshLayout.isEnabled = false
    }

    override fun onSettingsItemClick(item: Any, position: Int) {
        @Suppress("UNCHECKED_CAST")
        val typedItem = item as T
        options.getOrNull(position)?.clickCallback?.let { it(this as V, typedItem, position) }
    }

    @Suppress("UNCHECKED_CAST")
    override fun bind(item: T, itemView: View, position: Int) {
        val itemBinding = ListitemTitleSubtitleWidgetBinding.bind(itemView)
        bindSettingListitemDefaults(itemBinding, resources.getString(item.name), resources.getString(item.description), position)

        options.getOrNull(position)?.bindCallback?.let { it(this as V, item, itemBinding, position) }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onItemClick(item: T, position: Int) {
        options.getOrNull(position)?.clickCallback?.let { it(this as V, item, position) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
}

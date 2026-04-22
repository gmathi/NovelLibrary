package io.github.gmathi.novellibrary.activity

import android.os.Bundle
import android.text.Html
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.databinding.ActivityMetaDataBinding
import io.github.gmathi.novellibrary.databinding.ListitemMetadataBinding
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.util.system.startSearchResultsActivity
import io.github.gmathi.novellibrary.util.system.getParcelableExtraCompat
import io.github.gmathi.novellibrary.util.view.TextViewLinkHandler
import io.github.gmathi.novellibrary.util.view.extensions.applyFont
import io.github.gmathi.novellibrary.util.view.applyInsets
import io.github.gmathi.novellibrary.util.view.setDefaults
import io.github.gmathi.novellibrary.viewmodel.MetadataEvent
import io.github.gmathi.novellibrary.viewmodel.MetadataUiState
import io.github.gmathi.novellibrary.viewmodel.MetadataViewModel
import kotlinx.coroutines.launch
import java.util.Locale

class MetaDataActivity : BaseActivity(), GenericAdapter.Listener<Map.Entry<String, String?>>, TextViewLinkHandler.OnClickListener {

    private val viewModel: MetadataViewModel by viewModels()

    private lateinit var adapter: GenericAdapter<Map.Entry<String, String?>>

    private lateinit var binding: ActivityMetaDataBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMetaDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val novel = intent.getParcelableExtraCompat<Novel>("novel") ?: run {
            finish()
            return
        }
        viewModel.init(novel)

        setRecyclerView()
        observeViewModel()
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(
            items = arrayListOf(),
            layoutResId = R.layout.listitem_metadata,
            listener = this
        )
        binding.contentRecyclerView.recyclerView.setDefaults(adapter)
        binding.contentRecyclerView.recyclerView.clipToPadding = false
        binding.contentRecyclerView.recyclerView.applyInsets { view, insets ->
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, insets.bottom)
        }
        binding.contentRecyclerView.swipeRefreshLayout.setOnRefreshListener { binding.contentRecyclerView.swipeRefreshLayout.isRefreshing = false }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is MetadataUiState.Loading -> { /* no-op for this simple screen */ }
                            is MetadataUiState.Success -> {
                                adapter.updateData(ArrayList(state.entries))
                            }
                            is MetadataUiState.Empty -> {
                                adapter.updateData(arrayListOf())
                            }
                        }
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is MetadataEvent.NavigateToSearch -> {
                                startSearchResultsActivity(event.title, event.url)
                                viewModel.onEventConsumed()
                            }
                            null -> { /* no pending event */ }
                        }
                    }
                }
            }
        }
    }

    override fun bind(item: Map.Entry<String, String?>, itemView: View, position: Int) {
        val binding = ListitemMetadataBinding.bind(itemView)
        binding.metadataKey.applyFont(assets).text = item.key.uppercase(Locale.getDefault())
        binding.metadataValue.applyFont(assets)
        if (item.value != null) {
            binding.metadataValue.movementMethod = TextViewLinkHandler(this)
            binding.metadataValue.text = Html.fromHtml(item.value, Html.FROM_HTML_MODE_LEGACY)
        }
    }

    override fun onItemClick(item: Map.Entry<String, String?>, position: Int) {
        // no-op
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

    override fun onLinkClicked(title: String, url: String) {
        viewModel.onLinkClicked(title, url)
    }
}

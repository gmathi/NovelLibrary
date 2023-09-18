package io.github.gmathi.novellibrary.activity

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.MenuItem
import android.view.View
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.databinding.ActivityMetaDataBinding
import io.github.gmathi.novellibrary.databinding.ListitemMetadataBinding
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.util.system.startSearchResultsActivity
import io.github.gmathi.novellibrary.util.view.TextViewLinkHandler
import io.github.gmathi.novellibrary.util.view.extensions.applyFont
import io.github.gmathi.novellibrary.util.view.setDefaults
import java.util.*

class MetaDataActivity : BaseActivity(), GenericAdapter.Listener<Map.Entry<String, String?>>, TextViewLinkHandler.OnClickListener {

    lateinit var novel: Novel
    lateinit var adapter: GenericAdapter<Map.Entry<String, String?>>

    private lateinit var binding: ActivityMetaDataBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMetaDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        novel = intent.getParcelableExtra<Novel>("novel")!!
        setRecyclerView()


        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setRecyclerView() {
        @Suppress("UNCHECKED_CAST") adapter = GenericAdapter(
            items = (ArrayList(novel.metadata.entries) as ArrayList<Map.Entry<String, String?>>),
            layoutResId = R.layout.listitem_metadata,
            listener = this
        )
        binding.contentRecyclerView.recyclerView.setDefaults(adapter)
        binding.contentRecyclerView.swipeRefreshLayout.setOnRefreshListener { binding.contentRecyclerView.swipeRefreshLayout.isRefreshing = false }
    }

    override fun bind(item: Map.Entry<String, String?>, itemView: View, position: Int) {
        val binding = ListitemMetadataBinding.bind(itemView)
        binding.metadataKey.applyFont(assets).text = item.key.uppercase(Locale.getDefault())
        binding.metadataValue.applyFont(assets)
        if (item.value != null) {
            binding.metadataValue.movementMethod = TextViewLinkHandler(this)
            @Suppress("DEPRECATION") binding.metadataValue.text =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) Html.fromHtml(item.value, Html.FROM_HTML_MODE_LEGACY) else Html.fromHtml(item.value)
        }
    }

    override fun onItemClick(item: Map.Entry<String, String?>, position: Int) {

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

    override fun onLinkClicked(title: String, url: String) {
        startSearchResultsActivity(title, url)
    }

}

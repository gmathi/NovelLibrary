package io.github.gmathi.novellibrary.activity

import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.view.MenuItem
import android.view.View
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.extension.applyFont
import io.github.gmathi.novellibrary.extension.setDefaults
import io.github.gmathi.novellibrary.extension.startSearchResultsActivity
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.util.TextViewLinkHandler
import kotlinx.android.synthetic.main.activity_meta_data.*
import kotlinx.android.synthetic.main.content_chapters.*
import kotlinx.android.synthetic.main.listitem_metadata.view.*
import java.util.*

class MetaDataActivity : AppCompatActivity(), GenericAdapter.Listener<Map.Entry<String, String>>, TextViewLinkHandler.OnClickListener {

    lateinit var novel: Novel
    lateinit var adapter: GenericAdapter<Map.Entry<String, String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meta_data)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        novel = intent.getSerializableExtra("novel") as Novel
        setRecyclerView()


        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = (ArrayList(novel.metaData.entries) as ArrayList<Map.Entry<String, String>>), layoutResId = R.layout.listitem_metadata, listener = this)
        recyclerView.setDefaults(adapter)
        swipeRefreshLayout.setOnRefreshListener { swipeRefreshLayout.isRefreshing = false }
    }

    override fun bind(item: Map.Entry<String, String>, itemView: View, position: Int) {
        itemView.metadataKey.applyFont(assets).text = item.key.toUpperCase(Locale.getDefault())
        itemView.metadataValue.applyFont(assets)
        itemView.metadataValue.movementMethod = TextViewLinkHandler(this)
        itemView.metadataValue.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Html.fromHtml(item.value, Html.FROM_HTML_MODE_LEGACY) else Html.fromHtml(item.value)
    }

    override fun onItemClick(item: Map.Entry<String, String>) {

    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

    override fun onLinkClicked(title: String, url: String) {
        startSearchResultsActivity(title, url)
    }

}

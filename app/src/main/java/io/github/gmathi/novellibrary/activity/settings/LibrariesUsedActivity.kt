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
import io.github.gmathi.novellibrary.model.GenericJsonMappedModel
import io.github.gmathi.novellibrary.util.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.applyFont
import io.github.gmathi.novellibrary.util.setDefaults
import kotlinx.android.synthetic.main.activity_libraries_used.*
import kotlinx.android.synthetic.main.content_recycler_view.*
import kotlinx.android.synthetic.main.listitem_settings.view.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class LibrariesUsedActivity : BaseActivity(), GenericAdapter.Listener<GenericJsonMappedModel> {

    lateinit var adapter: GenericAdapter<GenericJsonMappedModel>
    private lateinit var librariesUsed: ArrayList<GenericJsonMappedModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_libraries_used)
        setSupportActionBar(toolbar)
        val libraries = getLibraryData()
        if (libraries == null) finish()
        librariesUsed = ArrayList(libraries!!.filter { it != null }.map { it!! })
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setRecyclerView()
    }

    private fun getLibraryData(): ArrayList<GenericJsonMappedModel?>? {
        val reader: BufferedReader
        val stringBuilder = StringBuilder()
        try {
            reader = BufferedReader(InputStreamReader(assets.open("libraries.json")))
            var mLine = reader.readLine()
            while (mLine != null) {
                stringBuilder.append(mLine)
                mLine = reader.readLine()
            }
            return Gson().fromJson(stringBuilder.toString(), object : TypeToken<ArrayList<GenericJsonMappedModel>>() {}.type)
        } catch (e: IOException) {
            Logs.error("LibrariesUsedActivity", e.localizedMessage, e)
        }
        return null
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = librariesUsed, layoutResId = R.layout.listitem_settings, listener = this)
        recyclerView.setDefaults(adapter)
        recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        swipeRefreshLayout.isEnabled = false
    }

    override fun bind(item: GenericJsonMappedModel, itemView: View, position: Int) {
        itemView.settingsTitle.applyFont(assets).text = item.name
        itemView.chevron.visibility = if (!item.link.isNullOrBlank()) View.VISIBLE else View.INVISIBLE
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

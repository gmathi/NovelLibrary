package io.github.gmathi.novellibrary.activity.settings

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import android.view.MenuItem
import android.view.View
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.extensions.openInBrowser
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.model.Library
import io.github.gmathi.novellibrary.util.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.applyFont
import io.github.gmathi.novellibrary.util.setDefaults
import kotlinx.android.synthetic.main.activity_libraries_used.*
import kotlinx.android.synthetic.main.content_recycler_view.*
import kotlinx.android.synthetic.main.listitem_settings.view.*
import org.jsoup.helper.StringUtil
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class LibrariesUsedActivity : BaseActivity(), GenericAdapter.Listener<Library> {

    lateinit var adapter: GenericAdapter<Library>
    private lateinit var librariesUsed: ArrayList<Library>

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

    private fun getLibraryData(): ArrayList<Library?>? {
        val reader: BufferedReader
        val stringBuilder = StringBuilder()
        try {
            reader = BufferedReader(InputStreamReader(assets.open("libraries.json")))
            var mLine = reader.readLine()
            while (mLine != null) {
                stringBuilder.append(mLine)
                mLine = reader.readLine()
            }
            return Gson().fromJson(stringBuilder.toString(), object : TypeToken<ArrayList<Library>>() {}.type)
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

    override fun bind(item: Library, itemView: View, position: Int) {
        itemView.settingsTitle.applyFont(assets).text = item.name
        itemView.chevron.visibility = if (!StringUtil.isBlank(item.link)) View.VISIBLE else View.INVISIBLE
        itemView.setBackgroundColor(if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
        else ContextCompat.getColor(this, android.R.color.transparent))
    }

    override fun onItemClick(item: Library) {
        if (!StringUtil.isBlank(item.link))
            openInBrowser(item.link!!)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

}

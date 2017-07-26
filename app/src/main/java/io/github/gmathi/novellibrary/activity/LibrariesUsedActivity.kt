package io.github.gmathi.novellibrary.activity

import android.graphics.Rect
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.MenuItem
import android.view.View
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.extension.applyFont
import io.github.gmathi.novellibrary.extension.openInBrowser
import io.github.gmathi.novellibrary.extension.setDefaults
import io.github.gmathi.novellibrary.model.Library
import kotlinx.android.synthetic.main.activity_libraries_used.*
import kotlinx.android.synthetic.main.content_recycler_view.*
import kotlinx.android.synthetic.main.listitem_settings.view.*
import org.jsoup.helper.StringUtil
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class LibrariesUsedActivity : AppCompatActivity(), GenericAdapter.Listener<Library> {

    lateinit var adapter: GenericAdapter<Library>
    lateinit var librariesUsed: ArrayList<Library>

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
        } catch (ex: IOException) {
            Log.e("Deserialize", ex.message)
            ex.printStackTrace()
        }
        return null
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = librariesUsed, layoutResId = R.layout.listitem_settings, listener = this)
        recyclerView.setDefaults(adapter)
        recyclerView.addItemDecoration(object : DividerItemDecoration(this, DividerItemDecoration.VERTICAL) {

            override fun getItemOffsets(outRect: Rect?, view: View?, parent: RecyclerView?, state: RecyclerView.State?) {
                val position = parent?.getChildAdapterPosition(view)
                if (position == parent?.adapter?.itemCount?.minus(1)) {
                    outRect?.setEmpty()
                } else {
                    super.getItemOffsets(outRect, view, parent, state)
                }
            }
        })
        swipeRefreshLayout.isEnabled = false
    }

    override fun bind(item: Library, itemView: View, position: Int) {
        itemView.settingsTitle.applyFont(assets).text = item.name
        itemView.settingsChevron.visibility = if (!StringUtil.isBlank(item.link)) View.VISIBLE else View.INVISIBLE
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

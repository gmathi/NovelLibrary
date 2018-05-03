package io.github.gmathi.novellibrary.activity.settings

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.view.MenuItem
import android.view.View
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.util.applyFont
import io.github.gmathi.novellibrary.util.setDefaults
import kotlinx.android.synthetic.main.activity_language.*
import kotlinx.android.synthetic.main.content_recycler_view.*
import kotlinx.android.synthetic.main.listitem_image_title_subtitle.view.*
import java.util.*

class LanguageActivity : BaseActivity(), GenericAdapter.Listener<String> {

    private val languagesMap = HashMap<String, String>()
    private val languagesImageResourceMap = HashMap<String, Int>()

    lateinit var adapter: GenericAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language)
        setSupportActionBar(toolbar)
        setLanguages()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = resources.getString(R.string.languages_supported)
        setRecyclerView()
    }

    private fun setLanguages() {
        languagesMap["English"] = "en_US"
        languagesMap["German"] = "de_DE"
        languagesMap["Indonesian"] = "id_ID"
        languagesMap["Portuguese"] = "pt_BR"
        languagesMap["Spanish"] = "es_CL"
        languagesMap["Turkish"] = "tr_TR"
        languagesMap["French"] = "fr_FR"
        languagesMap["Tagalog"] = "tr_PH"

        languagesImageResourceMap["English"] = R.drawable.flag_us
        languagesImageResourceMap["German"] = R.drawable.flag_de
        languagesImageResourceMap["Indonesian"] = R.drawable.flag_id
        languagesImageResourceMap["Portuguese"] = R.drawable.flag_br
        languagesImageResourceMap["Spanish"] = R.drawable.flag_cl
        languagesImageResourceMap["Turkish"] = R.drawable.flag_tr
        languagesImageResourceMap["French"] = R.drawable.flag_fr
        languagesImageResourceMap["Tagalog"] = R.drawable.flag_tl
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = ArrayList(languagesMap.keys.sorted()), layoutResId = R.layout.listitem_image_title_subtitle, listener = this)
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

    override fun bind(item: String, itemView: View, position: Int) {
        itemView.subtitle.visibility = View.GONE
        itemView.title.applyFont(assets).text = item
        itemView.imageView.setImageResource(languagesImageResourceMap[item]!!)

        itemView.setBackgroundColor(if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
        else ContextCompat.getColor(this, android.R.color.transparent))
    }

    @SuppressLint("NewApi")
    override fun onItemClick(item: String) {
        dataCenter.language = languagesMap[item]!!
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

}

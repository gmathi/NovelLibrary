package io.github.gmathi.novellibrary.activity.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.util.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.LocaleManager.Companion.changeLocale
import io.github.gmathi.novellibrary.util.applyFont
import io.github.gmathi.novellibrary.util.setDefaults
import kotlinx.android.synthetic.main.activity_language.*
import kotlinx.android.synthetic.main.content_recycler_view.*
import kotlinx.android.synthetic.main.listitem_image_title_subtitle.view.*
import java.util.*

class LanguageActivity : BaseActivity(), GenericAdapter.Listener<String> {

    companion object {
        private val languagesMap = HashMap<String, String>()
        private val languagesImageResourceMap = HashMap<String, Int>()

        init {
            languagesMap["English"] = "en_US"
            languagesMap["German"] = "de_DE"
            languagesMap["Indonesian"] = "id_ID"
            languagesMap["Portuguese"] = "pt_BR"
            languagesMap["Spanish"] = "es_CL"
            languagesMap["Turkish"] = "tr_TR"
            languagesMap["French"] = "fr_FR"
            languagesMap["Tagalog"] = "tr_PH"

            languagesImageResourceMap["System Default"] = android.R.color.transparent
            languagesImageResourceMap["English"] = R.drawable.flag_us
            languagesImageResourceMap["German"] = R.drawable.flag_de
            languagesImageResourceMap["Indonesian"] = R.drawable.flag_id
            languagesImageResourceMap["Portuguese"] = R.drawable.flag_br
            languagesImageResourceMap["Spanish"] = R.drawable.flag_cl
            languagesImageResourceMap["Turkish"] = R.drawable.flag_tr
            languagesImageResourceMap["French"] = R.drawable.flag_fr
            languagesImageResourceMap["Tagalog"] = R.drawable.flag_tl
        }
    }

    private var changeLanguage: Boolean = false

    lateinit var adapter: GenericAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language)
        setSupportActionBar(toolbar)

        if (intent.hasExtra("changeLanguage")) {
            changeLanguage = intent.getBooleanExtra("changeLanguage", false)
        }

        if (savedInstanceState != null && savedInstanceState.containsKey("changeLanguage")) {
            changeLanguage = savedInstanceState.getBoolean("changeLanguage")
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = resources.getString(R.string.languages_supported)
        setRecyclerView()
    }

    private fun setRecyclerView() {
        val items = ArrayList(languagesMap.keys.sorted())
        if (changeLanguage)
            items.add(0, resources.getString(R.string.system_default))
        adapter = GenericAdapter(items = items, layoutResId = R.layout.listitem_image_title_subtitle, listener = this)
        recyclerView.setDefaults(adapter)
        recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
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
        if (changeLanguage)
            changeLocale(this, if (item == resources.getString(R.string.system_default)) "systemDefault_" else languagesMap[item]!!)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("changeLanguage", changeLanguage)
    }
}

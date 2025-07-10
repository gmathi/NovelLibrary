package io.github.gmathi.novellibrary.activity.settings

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.databinding.ActivityLanguageBinding
import io.github.gmathi.novellibrary.databinding.ListitemImageTitleSubtitleBinding
import io.github.gmathi.novellibrary.util.Constants.SYSTEM_DEFAULT
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.lang.LocaleManager.Companion.changeLocale
import io.github.gmathi.novellibrary.util.lang.LocaleManager.Companion.translated
import io.github.gmathi.novellibrary.util.view.extensions.applyFont
import io.github.gmathi.novellibrary.util.view.setDefaults
import io.github.gmathi.novellibrary.util.showToast
import java.util.*
import kotlin.collections.ArrayList

class LanguageActivity : BaseActivity(), GenericAdapter.Listener<String> {

    companion object {

        fun getLanguageName(context: Context, language: String): String {
            return when (language) {
                "en" -> context.resources.getString(R.string.locale_english)
                "de" -> context.resources.getString(R.string.locale_german)
                "id" -> context.resources.getString(R.string.locale_indonesian)
                "pt" -> context.resources.getString(R.string.locale_portuguese)
                "es" -> context.resources.getString(R.string.locale_spanish)
                "tr" -> context.resources.getString(R.string.locale_turkish)
                "fr" -> context.resources.getString(R.string.locale_french)
                "tl" -> context.resources.getString(R.string.locale_Tagalog)
                "la" -> context.resources.getString(R.string.locale_latin)
                "pa" -> context.resources.getString(R.string.locale_pirate)
                else -> context.resources.getString(R.string.locale_system_default)
            }
        }

    }

    private val languagesMap = HashMap<String, String>()
    private val languagesImageResourceMap = HashMap<String, Int>()
    
    private lateinit var binding: ActivityLanguageBinding

    private fun getList(): ArrayList<String> {
        if (languagesMap.isEmpty()) {
            languagesMap[resources.getString(R.string.locale_english)] = "en_US"
            languagesMap[resources.getString(R.string.locale_german)] = "de_DE"
            languagesMap[resources.getString(R.string.locale_indonesian)] = "id_ID"
            languagesMap[resources.getString(R.string.locale_portuguese)] = "pt_BR"
            languagesMap[resources.getString(R.string.locale_spanish)] = "es_CL"
            languagesMap[resources.getString(R.string.locale_turkish)] = "tr_TR"
            languagesMap[resources.getString(R.string.locale_french)] = "fr_FR"
            languagesMap[resources.getString(R.string.locale_Tagalog)] = "tl_PH"
            languagesMap[resources.getString(R.string.locale_latin)] = "la_"

            languagesImageResourceMap[resources.getString(R.string.locale_system_default)] = android.R.color.transparent
            languagesImageResourceMap[resources.getString(R.string.locale_english)] = R.drawable.flag_us
            languagesImageResourceMap[resources.getString(R.string.locale_german)] = R.drawable.flag_de
            languagesImageResourceMap[resources.getString(R.string.locale_indonesian)] = R.drawable.flag_id
            languagesImageResourceMap[resources.getString(R.string.locale_portuguese)] = R.drawable.flag_br
            languagesImageResourceMap[resources.getString(R.string.locale_spanish)] = R.drawable.flag_cl
            languagesImageResourceMap[resources.getString(R.string.locale_turkish)] = R.drawable.flag_tr
            languagesImageResourceMap[resources.getString(R.string.locale_french)] = R.drawable.flag_fr
            languagesImageResourceMap[resources.getString(R.string.locale_Tagalog)] = R.drawable.flag_tl
            languagesImageResourceMap[resources.getString(R.string.locale_latin)] = android.R.color.transparent
            languagesImageResourceMap[resources.getString(R.string.locale_pirate)] = R.drawable.flag_pa
        }

        val date = Calendar.getInstance()
        if (date.get(Calendar.MONTH) == 3 && date.get(Calendar.DAY_OF_MONTH) == 1)
            languagesMap[resources.getString(R.string.locale_pirate)] = "pa_"

        val list = ArrayList(languagesMap.keys.sorted())
        if (changeLanguage)
            list.add(0, resources.getString(R.string.locale_system_default))
        return list
    }

    private var changeLanguage: Boolean = false

    lateinit var adapter: GenericAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

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
        val items = getList()
        adapter = GenericAdapter(items = items, layoutResId = R.layout.listitem_image_title_subtitle, listener = this)
        binding.contentRecyclerView.recyclerView.setDefaults(adapter)
        binding.contentRecyclerView.recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.contentRecyclerView.swipeRefreshLayout.isEnabled = false
    }

    override fun bind(item: String, itemView: View, position: Int) {
        val itemBinding = ListitemImageTitleSubtitleBinding.bind(itemView)
        itemBinding.subtitle.visibility = View.GONE
        itemBinding.title.applyFont(assets).text = item
        itemBinding.imageView.setImageResource(languagesImageResourceMap[item]!!)

        itemView.setBackgroundColor(if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
        else ContextCompat.getColor(this, android.R.color.transparent))
    }

    @SuppressLint("NewApi")
    override fun onItemClick(item: String, position: Int) {
        val language = if (item == resources.getString(R.string.locale_system_default)) SYSTEM_DEFAULT else languagesMap[item]!!.split('_')[0]
        if (changeLanguage)
            changeLocale(this, language)
        else if (language != "en" && language != "pa") {
            val translated = translated(this, language)
            val total = translated(this)
            if (translated == -1 || total == -1)
                return
            val percentage = String.format(Locale.ROOT, "%.2f", translated.toDouble() / total * 100)
            showToast(resources.getString(R.string.translated, translated, total, percentage), Toast.LENGTH_LONG)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("changeLanguage", changeLanguage)
    }
}

package io.github.gmathi.novellibrary.activity.settings

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import com.afollestad.materialdialogs.color.ColorChooserDialog
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.databinding.ActivitySettingsBinding
import io.github.gmathi.novellibrary.databinding.ListitemTitleSubtitleWidgetBinding
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.applyFont
import io.github.gmathi.novellibrary.util.setDefaults
import okhttp3.internal.toHexString
import java.util.*
import kotlin.collections.ArrayList


class ReaderBackgroundSettingsActivity : BaseActivity(), GenericAdapter.Listener<String>, ColorChooserDialog.ColorCallback {

    companion object {
        private const val POSITION_DAY_BACKGROUND = 0
        private const val POSITION_DAY_TEXT = 1
        private const val POSITION_NIGHT_BACKGROUND = 2
        private const val POSITION_NIGHT_TEXT = 3
    }

    private lateinit var adapter: GenericAdapter<String>
    private lateinit var settingsItems: ArrayList<String>

    private var selectedPosition: Int = 0
    
    private lateinit var binding:ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setRecyclerView()
    }

    private fun setRecyclerView() {
        settingsItems = ArrayList(resources.getStringArray(R.array.reader_background_color_options).asList())
        adapter = GenericAdapter(items = settingsItems, layoutResId = R.layout.listitem_title_subtitle_widget, listener = this)
        binding.contentRecyclerView.recyclerView.setDefaults(adapter)
        binding.contentRecyclerView.recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.contentRecyclerView.swipeRefreshLayout.isEnabled = false
    }

    override fun bind(item: String, itemView: View, position: Int) {
        val itemBinding = ListitemTitleSubtitleWidgetBinding.bind(itemView)
        //(itemView as ViewGroup).enabled(true)
        itemBinding.blackOverlay.visibility = View.INVISIBLE
        itemBinding.widgetChevron.visibility = View.INVISIBLE
        itemBinding.widgetSwitch.visibility = View.INVISIBLE
        itemBinding.currentValue.visibility = View.INVISIBLE
        itemBinding.currentValue.text = ""
        itemBinding.colorView.visibility = View.VISIBLE

        itemBinding.title.applyFont(assets).text = item
        val drawable = itemBinding.colorView.background as GradientDrawable

        when (position) {
            POSITION_DAY_BACKGROUND -> {
                itemBinding.subtitle.applyFont(assets).text = getString(R.string.hex_color, dataCenter.dayModeBackgroundColor.toHexString().toUpperCase(Locale.ROOT))
                drawable.setColor(dataCenter.dayModeBackgroundColor)
            }
            POSITION_DAY_TEXT -> {
                itemBinding.subtitle.applyFont(assets).text = getString(R.string.hex_color, dataCenter.dayModeTextColor.toHexString().toUpperCase(Locale.ROOT))
                drawable.setColor(dataCenter.dayModeTextColor)
            }
            POSITION_NIGHT_BACKGROUND -> {
                itemBinding.subtitle.applyFont(assets).text = getString(R.string.hex_color, dataCenter.nightModeBackgroundColor.toHexString().toUpperCase(Locale.ROOT))
                drawable.setColor(dataCenter.nightModeBackgroundColor)
            }
            POSITION_NIGHT_TEXT -> {
                itemBinding.subtitle.applyFont(assets).text = getString(R.string.hex_color, dataCenter.nightModeTextColor.toHexString().toUpperCase(Locale.ROOT))
                drawable.setColor(dataCenter.nightModeTextColor)
            }
        }

        itemView.setBackgroundColor(
            if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
            else ContextCompat.getColor(this, android.R.color.transparent)
        )
    }

    override fun onItemClick(item: String, position: Int) {
        selectedPosition = position
        ColorChooserDialog.Builder(this, R.string.confirm_action).show(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

    override fun onColorSelection(dialog: ColorChooserDialog, selectedColor: Int) {
        when (selectedPosition) {
            POSITION_DAY_BACKGROUND -> dataCenter.dayModeBackgroundColor = selectedColor
            POSITION_DAY_TEXT -> dataCenter.dayModeTextColor = selectedColor
            POSITION_NIGHT_BACKGROUND -> dataCenter.nightModeBackgroundColor = selectedColor
            POSITION_NIGHT_TEXT -> dataCenter.nightModeTextColor = selectedColor
        }
        adapter.updateItem(settingsItems[selectedPosition])
    }

    override fun onColorChooserDismissed(dialog: ColorChooserDialog) {

    }

}
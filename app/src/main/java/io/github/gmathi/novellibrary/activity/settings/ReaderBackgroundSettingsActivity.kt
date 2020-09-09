package io.github.gmathi.novellibrary.activity.settings

import android.graphics.Color
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
import io.github.gmathi.novellibrary.util.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.applyFont
import io.github.gmathi.novellibrary.util.setDefaults
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.content_recycler_view.*
import kotlinx.android.synthetic.main.listitem_title_subtitle_widget.view.*
import okhttp3.internal.toHexString


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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setRecyclerView()
    }

    private fun setRecyclerView() {
        settingsItems = ArrayList(resources.getStringArray(R.array.reader_background_color_options).asList())
        adapter = GenericAdapter(items = settingsItems, layoutResId = R.layout.listitem_title_subtitle_widget, listener = this)
        recyclerView.setDefaults(adapter)
        recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        swipeRefreshLayout.isEnabled = false
    }

    override fun bind(item: String, itemView: View, position: Int) {
        //(itemView as ViewGroup).enabled(true)
        itemView.blackOverlay.visibility = View.INVISIBLE
        itemView.widgetChevron.visibility = View.INVISIBLE
        itemView.widgetSwitch.visibility = View.INVISIBLE
        itemView.currentValue.visibility = View.INVISIBLE
        itemView.currentValue.text = ""
        itemView.colorView.visibility = View.VISIBLE

        itemView.title.applyFont(assets).text = item
        val drawable = itemView.colorView.background as GradientDrawable

        when (position) {
            POSITION_DAY_BACKGROUND -> {
                val color = "#${dataCenter.dayModeBackgroundColor}"
                itemView.subtitle.applyFont(assets).text = getString(R.string.hex_color, color)
                drawable.setColor(Color.parseColor(color))
            }
            POSITION_DAY_TEXT -> {
                val color = "#${dataCenter.dayModeTextColor}"
                itemView.subtitle.applyFont(assets).text = getString(R.string.hex_color, color)
                drawable.setColor(Color.parseColor(color))
            }
            POSITION_NIGHT_BACKGROUND -> {
                val color = "#${dataCenter.nightModeBackgroundColor}"
                itemView.subtitle.applyFont(assets).text = getString(R.string.hex_color, color)
                drawable.setColor(Color.parseColor(color))
            }
            POSITION_NIGHT_TEXT -> {
                val color = "#${dataCenter.nightModeTextColor}"
                itemView.subtitle.applyFont(assets).text = getString(R.string.hex_color, color)
                drawable.setColor(Color.parseColor(color))
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
            POSITION_DAY_BACKGROUND -> dataCenter.dayModeBackgroundColor = selectedColor.toHexString()
            POSITION_DAY_TEXT -> dataCenter.dayModeTextColor = selectedColor.toHexString()
            POSITION_NIGHT_BACKGROUND -> dataCenter.nightModeBackgroundColor = selectedColor.toHexString()
            POSITION_NIGHT_TEXT -> dataCenter.nightModeTextColor = selectedColor.toHexString()
        }
        adapter.updateItem(settingsItems[selectedPosition])
    }

    override fun onColorChooserDismissed(dialog: ColorChooserDialog) {

    }

}
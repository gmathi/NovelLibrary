package io.github.gmathi.novellibrary.util.system

import android.content.SharedPreferences
import android.util.TypedValue
import android.view.View
import android.widget.CompoundButton
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.databinding.ListitemTitleSubtitleWidgetBinding
import io.github.gmathi.novellibrary.model.ui.ListitemSetting
import io.github.gmathi.novellibrary.model.ui.SettingItemClickCallback
import io.github.gmathi.novellibrary.util.view.extensions.applyFont
import kotlin.math.roundToInt

/**
 * Initial setting item binding cleanup
 */
fun BaseActivity.bindSettingListitemDefaults(itemBinding: ListitemTitleSubtitleWidgetBinding, name: String, description: String, position: Int) {
    itemBinding.blackOverlay.visibility = View.INVISIBLE
    itemBinding.widgetChevron.visibility = View.INVISIBLE
    itemBinding.widgetSwitch.visibility = View.INVISIBLE
    itemBinding.currentValue.visibility = View.INVISIBLE
    itemBinding.subtitle.visibility = View.VISIBLE
    itemBinding.widget.visibility = View.VISIBLE
    itemBinding.currentValue.text = ""

    //(itemBinding.root as ViewGroup).enabled(true)
    itemBinding.title.applyFont(assets).text = name
    itemBinding.subtitle.applyFont(assets).text = description
    itemBinding.widgetSwitch.setOnCheckedChangeListener(null)
    itemBinding.root.setBackgroundColor(
        if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
        else ContextCompat.getColor(this, android.R.color.transparent)
    )
}

/**
 * Shortcut to show a toggle on a setting with the provided change listener.
 */
fun ListitemTitleSubtitleWidgetBinding.bindSwitch(checked: Boolean, closure: CompoundButton.OnCheckedChangeListener) {
    widgetSwitch.visibility = View.VISIBLE
    widgetSwitch.isChecked = checked
    widgetSwitch.setOnCheckedChangeListener(closure)
}

/**
 * Shortcut to show a chevron on a setting
 */
fun ListitemTitleSubtitleWidgetBinding.bindChevron() {
    widgetChevron.visibility = View.VISIBLE
}

/**
 * Shortcut to show a chevron on a setting with a provided click listener.
 */
fun<T> ListitemSetting<T>.bindChevron(closure: SettingItemClickCallback<T, ListitemTitleSubtitleWidgetBinding>):ListitemSetting<T> {
    return this.onBind { _, view, _ -> view.bindChevron() }.onClick(closure)
}

fun<T> ListitemSetting<T>.bindHeader(): ListitemSetting<T> {
    return this.onBind { _, view, _ ->
        view.subtitle.visibility = View.GONE
        view.widget.visibility = View.GONE
        view.root.minimumHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40F, view.root.resources.displayMetrics).roundToInt()
    }
}

inline fun<reified T> SharedPreferences.getJson(key: String, def: String): T =
    Gson().fromJson(getString(key, def), object : TypeToken<T>() {}.type)

inline fun<reified T> SharedPreferences.getJson(key: String): T? =
    Gson().fromJson(getString(key, "null"), object : TypeToken<T>() {}.type)

inline fun<reified T> SharedPreferences.Editor.putJson(key: String, value: T?):SharedPreferences.Editor =
    putString(key, Gson().toJson(value))
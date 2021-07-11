package io.github.gmathi.novellibrary.model.ui

import android.app.Activity
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.databinding.ListitemTitleSubtitleWidgetBinding
import io.github.gmathi.novellibrary.util.view.extensions.applyFont

class SettingItem<T, V>(val name: Int, val description: Int) {

    companion object {
        inline fun<reified T> of(name: Int, description: Int) = GenericSettingItem<T>(name, description)
    }

    var bindCallback: SettingItemBindingCallback<T, V>? = null
    var clickCallback: SettingItemClickCallback<T, V>? = null

    fun onBind(closure: SettingItemBindingCallback<T, V>?):SettingItem<T, V> {
        bindCallback = closure
        return this
    }

    fun onClick(closure: SettingItemClickCallback<T, V>?):SettingItem<T, V> {
        clickCallback = closure
        return this
    }

}

typealias SettingItemBindingCallback<T, V> = T.(item: SettingItem<T, V>, view: V, position: Int) -> Unit
typealias SettingItemClickCallback<T, V> = T.(item: SettingItem<T, V>, position: Int) -> Unit
typealias GenericSettingItem<T> = SettingItem<T, ListitemTitleSubtitleWidgetBinding>

fun ListitemTitleSubtitleWidgetBinding.bindSwitch(checked: Boolean, closure: CompoundButton.OnCheckedChangeListener) {
    widgetSwitch.visibility = View.VISIBLE
    widgetSwitch.isChecked = checked
    widgetSwitch.setOnCheckedChangeListener(closure)
}

fun ListitemTitleSubtitleWidgetBinding.bindChevron() {
    widgetChevron.visibility = View.VISIBLE
}
fun BaseActivity.fillSettingDefaults(itemBinding: ListitemTitleSubtitleWidgetBinding, name: String, description: String, position: Int) {
    itemBinding.blackOverlay.visibility = View.INVISIBLE
    itemBinding.widgetChevron.visibility = View.INVISIBLE
    itemBinding.widgetSwitch.visibility = View.INVISIBLE
    itemBinding.currentValue.visibility = View.INVISIBLE
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

fun<T> GenericSettingItem<T>.bindChevron(closure: SettingItemClickCallback<T, ListitemTitleSubtitleWidgetBinding>):GenericSettingItem<T> {
    return this.onBind { _, view, _ -> view.bindChevron() }.onClick(closure)
}
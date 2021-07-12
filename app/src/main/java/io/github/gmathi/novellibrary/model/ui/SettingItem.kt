package io.github.gmathi.novellibrary.model.ui

import io.github.gmathi.novellibrary.databinding.ListitemTitleSubtitleWidgetBinding

class SettingItem<T, V>(val name: Int, val description: Int) {

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
typealias ListitemSetting<T> = SettingItem<T, ListitemTitleSubtitleWidgetBinding>
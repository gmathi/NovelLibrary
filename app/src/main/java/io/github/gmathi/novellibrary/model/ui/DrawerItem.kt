package io.github.gmathi.novellibrary.model.ui

import android.view.ViewGroup
import io.github.gmathi.novellibrary.adapter.DrawerAdapter


abstract class DrawerItem<T : DrawerAdapter.ViewHolder> {

    private var isCheckedSlideMenu: Boolean = false
    private var textOnly: Boolean = false
    private var switch: Boolean = false
    open val isSelectable: Boolean
        get() = true

    abstract fun createViewHolder(parent: ViewGroup): T

    abstract fun bindViewHolder(holder: T,position: Int)

    fun setChecked(isChecked: Boolean): DrawerItem<*> {
        this.isCheckedSlideMenu = isChecked
        return this
    }

    fun setSwitchOn(value: Boolean): DrawerItem<*> {
        this.switch = value
        return this
    }

    fun isSwitchOn(): Boolean {
        return switch
    }

    fun setTextOnly(testOnly: Boolean): DrawerItem<*> {
        this.textOnly = testOnly
        return this
    }

    fun isChecked(): Boolean {
        return isCheckedSlideMenu
    }

    fun isTextOnly(): Boolean {
        return textOnly
    }

}
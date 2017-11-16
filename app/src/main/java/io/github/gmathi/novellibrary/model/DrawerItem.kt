package io.github.gmathi.novellibrary.model

import android.view.ViewGroup
import io.github.gmathi.novellibrary.adapter.DrawerAdapter


/**
 * Created by a6001823 on 11/15/17.
 */
internal abstract class DrawerItem<T : DrawerAdapter.ViewHolder> {

    protected var isCheckedSlideMenu: Boolean = false
    protected var isSwitchOn:Boolean =false
    open val isSelectable: Boolean
        get() = true

    abstract fun createViewHolder(parent: ViewGroup): T

    abstract fun bindViewHolder(holder: T)

    fun setChecked(isChecked: Boolean): DrawerItem<*> {
        this.isCheckedSlideMenu = isChecked
        return this
    }
    fun setSwitchOn(isChecked: Boolean): DrawerItem<*> {
        this.isSwitchOn = isChecked
        return this
    }
    fun isChecked(): Boolean {
        return isCheckedSlideMenu
    }
    fun isSwitch(): Boolean {
        return isSwitchOn
    }

}
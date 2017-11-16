package io.github.gmathi.novellibrary.model

import android.view.View
import android.view.ViewGroup
import io.github.gmathi.novellibrary.adapter.DrawerAdapter

/**
 * Created by a6001823 on 11/15/17.
 */
internal class SpaceItem(private val spaceDp: Int) : DrawerItem<SpaceItem.ViewHolder>() {

    override val isSelectable: Boolean
        get() = false

    override fun createViewHolder(parent: ViewGroup): ViewHolder {
        val c = parent.context
        val view = View(c)
        val height = (c.resources.displayMetrics.density * spaceDp).toInt()
        view.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height)
        return ViewHolder(view)
    }

    override fun bindViewHolder(holder: ViewHolder) {

    }

    internal class ViewHolder(itemView: View) : DrawerAdapter.ViewHolder(itemView)
}

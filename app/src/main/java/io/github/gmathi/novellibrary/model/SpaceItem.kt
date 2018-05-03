package io.github.gmathi.novellibrary.model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.DrawerAdapter

internal class SpaceItem(private var title: String) : DrawerItem<SpaceItem.ViewHolder>() {
    private var normalItemTextTint: Int = 0

    override val isSelectable: Boolean
        get() = false

    override fun createViewHolder(parent: ViewGroup): SpaceItem.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(R.layout.item_option, parent, false)
        return SpaceItem.ViewHolder(v)
    }

    override fun bindViewHolder(holder: ViewHolder,position:Int) {
        holder.title.text = title
        holder.title.setTextColor(normalItemTextTint)
    }

    internal class ViewHolder(itemView: View) : DrawerAdapter.ViewHolder(itemView) {
        internal var title: TextView = itemView.findViewById(R.id.title) as TextView
    }

}

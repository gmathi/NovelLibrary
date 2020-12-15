package io.github.gmathi.novellibrary.model.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.DrawerAdapter

class SimpleItem(private var item: ReaderMenu, private var listener: Listener<ReaderMenu>) : DrawerItem<SimpleItem.ViewHolder>() {

    private var selectedItemIconTint: Int = 0
    private var selectedItemTextTint: Int = 0

    private var normalItemIconTint: Int = 0
    private var normalItemTextTint: Int = 0

    override fun createViewHolder(parent: ViewGroup): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(R.layout.item_option, parent, false)
        return ViewHolder(v)
    }

    override fun bindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(item, listener, position, this)
    }

    interface Listener<in T> {
        fun bind(item: T, itemView: View, position: Int, simpleItem: SimpleItem)
    }

    fun withSelectedIconTint(selectedItemIconTint: Int): SimpleItem {
        this.selectedItemIconTint = selectedItemIconTint
        return this
    }

    fun withSelectedTextTint(selectedItemTextTint: Int): SimpleItem {
        this.selectedItemTextTint = selectedItemTextTint
        return this
    }

    fun withIconTint(normalItemIconTint: Int): SimpleItem {
        this.normalItemIconTint = normalItemIconTint
        return this
    }

    fun withTextTint(normalItemTextTint: Int): SimpleItem {
        this.normalItemTextTint = normalItemTextTint
        return this
    }

    class ViewHolder(itemView: View) : DrawerAdapter.ViewHolder(itemView) {
        fun bind(item: ReaderMenu, listener: Listener<ReaderMenu>, position: Int, simpleItem: SimpleItem) {
            listener.bind(item = item, itemView = itemView, position = position, simpleItem = simpleItem)
        }
    }
}

package io.github.gmathi.novellibrary.model

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.DrawerAdapter

/**
 * Created by a6001823 on 11/15/17.
 */
internal class SimpleItem(private var icon: Drawable, private var title: String) : DrawerItem<SimpleItem.ViewHolder>() {

    private var selectedItemIconTint: Int = 0
    private var selectedItemTextTint: Int = 0

    private var normalItemIconTint: Int = 0
    private var normalItemTextTint: Int = 0

    override fun createViewHolder(parent: ViewGroup): ViewHolder {
        var inflater = LayoutInflater.from(parent.context)
        var v = inflater.inflate(R.layout.item_option, parent, false)
        return ViewHolder(v)
    }

    override fun bindViewHolder(holder: ViewHolder) {
        holder.title.text = title
        if (!isSwitchOn)
            holder.icon.setImageDrawable(icon)
        else
        {
            holder.icon.setVisibility(View.GONE)
            holder.switch.setVisibility(View.VISIBLE)
        }

        holder.title.setTextColor(if (isCheckedSlideMenu) selectedItemTextTint else normalItemTextTint)
        holder.icon.setColorFilter(if (isCheckedSlideMenu) selectedItemIconTint else normalItemIconTint)
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

    internal class ViewHolder(itemView: View) : DrawerAdapter.ViewHolder(itemView) {

        internal var icon: ImageView
        internal var title: TextView
        internal var switch: Switch

        init {
            icon = itemView.findViewById<ImageView>(R.id.icon) as ImageView
            title = itemView.findViewById<TextView>(R.id.title) as TextView
            switch=itemView.findViewById<Switch>(R.id.switchtest) as Switch
        }
    }
}

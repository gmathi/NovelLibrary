package io.github.gmathi.novellibrary.adapter

import android.support.v7.widget.RecyclerView
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import io.github.gmathi.novellibrary.model.DrawerItem


/**
 * Created by a6001823 on 11/15/17.
 */
internal class DrawerAdapter(private val items: List<DrawerItem<DrawerAdapter.ViewHolder>>) : RecyclerView.Adapter<DrawerAdapter.ViewHolder>() {
    private var viewTypes: MutableMap<Class<out DrawerItem<DrawerAdapter.ViewHolder>>, Int>
    private var holderFactories: SparseArray<DrawerItem<DrawerAdapter.ViewHolder>>

    private var listener: OnItemSelectedListener? = null

    init {
        this.viewTypes = HashMap()
        this.holderFactories = SparseArray<DrawerItem<DrawerAdapter.ViewHolder>>()

        processViewTypes()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var holder = holderFactories.get(viewType).createViewHolder(parent)
        holder.adapter = this
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        items[position].bindViewHolder(holder)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        return viewTypes[items[position].javaClass]!!
    }

    private fun processViewTypes() {
        var type = 0
        for (item in items) {
            if (!viewTypes.containsKey(item.javaClass)) {
                viewTypes.put(item.javaClass, type)
                holderFactories.put(type, item)
                type++
            }
        }
    }

    fun setSelected(position: Int) {
        val newChecked = items[position]
        if (!newChecked.isSelectable) {
            return
        }

        for (i in items.indices) {
            val item = items[i]
            if (item.isChecked()) {
                item.setChecked(false)
                notifyItemChanged(i)
                break
            }
        }

        newChecked.setChecked(true)
        notifyItemChanged(position)

        if (listener != null) {
            listener!!.onItemSelected(position)
        }
    }

    fun setListener(listener: OnItemSelectedListener) {
        this.listener = listener
    }

    internal abstract class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        var adapter: DrawerAdapter? = null

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            adapter!!.setSelected(adapterPosition)
        }
    }

    interface OnItemSelectedListener {
        fun onItemSelected(position: Int)
    }
}
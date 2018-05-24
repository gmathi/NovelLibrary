package io.github.gmathi.novellibrary.adapter

import android.support.v7.widget.RecyclerView
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import io.github.gmathi.novellibrary.model.DrawerItem


class DrawerAdapter(private val items: List<DrawerItem<DrawerAdapter.ViewHolder>>) : RecyclerView.Adapter<DrawerAdapter.ViewHolder>() {
    private var viewTypes: MutableMap<Class<out DrawerItem<DrawerAdapter.ViewHolder>>, Int> = HashMap()
    private var holderFactories: SparseArray<DrawerItem<DrawerAdapter.ViewHolder>> = SparseArray()

    private var listener: OnItemSelectedListener? = null

    init {

        processViewTypes()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val holder = holderFactories.get(viewType).createViewHolder(parent)
        holder.adapter = this
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        items[position].bindViewHolder(holder, position)
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
                viewTypes[item.javaClass] = type
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

    abstract class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var adapter: DrawerAdapter? = null

        init {
            itemView.setOnClickListener {
                adapter!!.setSelected(adapterPosition)
            }
        }

    }

    interface OnItemSelectedListener {
        fun onItemSelected(position: Int)
    }
}
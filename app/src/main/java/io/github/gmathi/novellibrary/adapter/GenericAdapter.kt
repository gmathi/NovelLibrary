package io.github.gmathi.novellibrary.adapter

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import io.github.gmathi.novellibrary.util.inflate
import java.util.*

class GenericAdapter<T>(val items: ArrayList<T>, val layoutResId: Int, val listener: Listener<T>) : RecyclerView.Adapter<GenericAdapter.ViewHolder<T>>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder<T>(parent.inflate(layoutResId))

    override fun onBindViewHolder(holder: ViewHolder<T>, position: Int) = holder.bind(item = items[position], listener = listener, position = position)

    override fun onBindViewHolder(holder: ViewHolder<T>, position: Int, payloads: MutableList<Any>?) = holder.bind(item = items[position], listener = listener, position = position, payloads = payloads)

    override fun getItemCount() = items.size

    class ViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: T, listener: Listener<T>, position: Int) {
            with(itemView) { setOnClickListener { listener.onItemClick(item) } }
            listener.bind(item = item, itemView = itemView, position = position)
        }

        fun bind(item: T, listener: Listener<T>, position: Int, payloads: MutableList<Any>?) {
            with(itemView) { setOnClickListener { listener.onItemClick(item) } }
            listener.bind(item = item, itemView = itemView, position = position, payloads = payloads)
        }

    }

    interface Listener<in T> {
        fun bind(item: T, itemView: View, position: Int)
        fun bind(item: T, itemView: View, position: Int, payloads: MutableList<Any>?) {
            bind(item, itemView, position)
        }

        fun onItemClick(item: T)
    }

    fun updateData(newItems: ArrayList<T>) {
        //Empty Current List --> Add All
        if (items.size == 0) {
            items.addAll(newItems)
            notifyItemRangeInserted(0, items.size)
            return
        }

        //Empty New List --> Remove All
        if (newItems.size == 0) {
            val size = items.size
            items.clear()
            notifyItemRangeRemoved(0, size)
            return
        }

        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun updateItem(item: T) {
        val index = items.indexOf(item)
        if (index != -1) {
            items.removeAt(index)
            items.add(index, item)
            notifyItemChanged(index)
        }
    }

    fun removeItem(item: T) {
        val index = items.indexOf(item)
        if (index != -1) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun removeItemAt(position: Int) {
        if (position != -1 && position < items.size) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    @Suppress("unused")
    fun insertItem(item: T, position: Int = -1) {
        val index = items.indexOf(item)
        if (index == -1) {
            if (position != -1) items.add(position, item) else items.add(item)
        } else
            updateItem(item)
    }

    fun onItemDismiss(position: Int) {
        items.removeAt(position)
        notifyDataSetChanged()
    }

    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(items, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(items, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

}
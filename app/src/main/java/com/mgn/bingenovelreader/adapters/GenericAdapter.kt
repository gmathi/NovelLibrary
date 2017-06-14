package com.mgn.bingenovelreader.adapters

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.mgn.bingenovelreader.utils.inflate


class GenericAdapter<T>(val items: ArrayList<T>, val layoutResId: Int, val listener: Listener<T>) : RecyclerView.Adapter<GenericAdapter.ViewHolder<T>>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder<T>(parent.inflate(layoutResId))

    override fun onBindViewHolder(holder: ViewHolder<T>, position: Int) = holder.bind(items[position], listener)

    override fun getItemCount() = items.size

    class ViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: T, listener: Listener<T>) {
            with(itemView) { setOnClickListener { listener.onItemClick(item) } }
            listener.bind(item, itemView)
        }
    }

    interface Listener<in T> {
        fun bind(item: T, itemView: View)
        fun onItemClick(item: T)
    }

    fun updateData(newItems: ArrayList<T>) {
        items.clear()
        items.addAll(newItems)
    }
}
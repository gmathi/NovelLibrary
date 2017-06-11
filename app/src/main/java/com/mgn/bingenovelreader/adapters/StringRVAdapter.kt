package com.mgn.bingenovelreader.adapters

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.mgn.bingenovelreader.R
import com.mgn.bingenovelreader.utils.inflate
import kotlinx.android.synthetic.main.listitem_card_view.view.*


class StringRVAdapter(val items: ArrayList<String>, val listener: (String) -> Unit) : RecyclerView.Adapter<StringRVAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent.inflate(R.layout.listitem_card_view))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position], listener)

    override fun getItemCount() = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: String, listener: (String) -> Unit) = with(itemView) {
            listItemTitle.text = item
            setOnClickListener { listener(item) }
        }
    }

    //Or run post delayed with 0.5sec interval

    fun updateData(newList: ArrayList<String>) {
        items.clear()
        for (item in newList) {
            items.add(item)
            notifyItemInserted(items.size)
        }
    }
}
package com.mgn.bingenovelreader.adapters

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.mgn.bingenovelreader.R


class CardListItemAdapter(val context: Context, var cacheTitles: ArrayList<String>) : BaseAdapter() {


    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view: View? = convertView
        if (view == null)
            view = LayoutInflater.from(context).inflate(R.layout.listitem_card_view, null)

        view!! // this throws a NPE if view is null
        val titleTextView: TextView = view.findViewById(R.id.listItemTitle) as TextView
        titleTextView.typeface = Typeface.createFromAsset(context.assets, "font/roboto_regular" +
                ".ttf")
        titleTextView.text = getItem(position)

        return view
    }

    override fun getItem(position: Int): String {
        return cacheTitles[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return cacheTitles.size
    }

    fun updateData(newData: ArrayList<String>) {
        this.cacheTitles.clear()
        this.cacheTitles.addAll(newData)
        notifyDataSetInvalidated()
    }

    fun addItem(item: String) {
        this.cacheTitles.add(item)
        notifyDataSetInvalidated()
    }


    fun removeItem(item: String) {
        this.cacheTitles.remove(item)
        notifyDataSetInvalidated()
    }

}
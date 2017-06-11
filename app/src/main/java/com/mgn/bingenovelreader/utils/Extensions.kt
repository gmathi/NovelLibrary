package com.mgn.bingenovelreader.utils

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.mgn.bingenovelreader.dataCenter


fun ViewGroup.inflate(layoutRes: Int): View {
    val view = LayoutInflater.from(context).inflate(layoutRes, this, false)
    //view.layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.MATCH_PARENT)
    return view
}

fun Activity.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun ArrayList<String>.addSearchTerm(searchTerm: String) {

}

fun String.addToSearchHistory() {
    val list = dataCenter.loadSearchHistory()
    if (!list.contains(this))
        list.add(0, this)
    dataCenter.saveSearchHistory(list)
}

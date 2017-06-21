package com.mgn.bingenovelreader.utils

import android.app.Activity
import android.net.Uri
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import com.mgn.bingenovelreader.adapters.GenericAdapter
import com.mgn.bingenovelreader.dataCenter
import jp.wasabeef.recyclerview.animators.SlideInUpAnimator


fun ViewGroup.inflate(layoutRes: Int): View {
    val view = LayoutInflater.from(context).inflate(layoutRes, this, false)
    //view.layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.MATCH_PARENT)
    return view
}

fun Activity.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun String.addToSearchHistory() {
    val list = dataCenter.loadSearchHistory()
    if (!list.contains(this))
        list.add(0, this)
    dataCenter.saveSearchHistory(list)
}

fun String.writableFileName(): String {
    var fileName = this.replace(Regex.fromLiteral("[^a-zA-Z0-9.-]"), "_").replace("/", "_")
    if (fileName.length > 150)
        fileName = fileName.substring(0, 150)
    return fileName
}

/**
 * Sets the Adapter, LayoutManager, Animations and fixesSize flag.
 */
fun <T> RecyclerView.setDefaults(adapter: GenericAdapter<T>): RecyclerView {
    val animator = SlideInUpAnimator(OvershootInterpolator(1f))
    animator.addDuration = 1000
    animator.removeDuration = 1000

    this.setHasFixedSize(true)
    this.layoutManager = LinearLayoutManager(context)
    this.itemAnimator = animator
    this.adapter = adapter

    return this
}

fun Uri.getFileName(): String {
    return (this.lastPathSegment + this.toString().substringAfter("?", "")).writableFileName()
}

package com.mgn.bingenovelreader.utils

import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import com.bumptech.glide.Glide
import com.mgn.bingenovelreader.adapters.GenericAdapter
import com.mgn.bingenovelreader.dataCenter
import jp.wasabeef.recyclerview.animators.SlideInRightAnimator
import kotlinx.android.synthetic.main.layout_loading.*


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
    val animator = SlideInRightAnimator(OvershootInterpolator(1f))
    animator.addDuration = 1000
    animator.removeDuration = 1000
    animator.changeDuration = 0
    animator.moveDuration = 1000

    this.setHasFixedSize(true)
    this.layoutManager = SnappingLinearLayoutManager(context)
    this.itemAnimator = animator
    this.adapter = adapter

    return this
}

fun Uri.getFileName(): String {
    return (this.lastPathSegment + this.toString().substringAfter("?", "")).writableFileName()
}

fun Activity.setLoadingView(drawableRes: Int, message: String) {
    Glide.with(this).load(drawableRes).into(loadingImageView)
    loadingTextView.typeface = Typeface.createFromAsset(assets, "font/roboto_regular" + ".ttf")
    loadingTextView.text = message
}

fun Activity.enableLoadingView(enable: Boolean) {
    loadingLayout.visibility = if (enable) View.VISIBLE else View.GONE
}

fun ContextWrapper.sendBroadcast(extras: Bundle, action: String) {
    val localIntent = Intent()
    localIntent.action = action
    localIntent.putExtras(extras)
    localIntent.addCategory(Intent.CATEGORY_DEFAULT)
    sendBroadcast(localIntent)
}

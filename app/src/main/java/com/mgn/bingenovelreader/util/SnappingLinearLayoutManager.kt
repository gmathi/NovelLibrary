package com.mgn.bingenovelreader.util

import android.content.Context
import android.graphics.PointF
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.LinearSmoothScroller
import android.support.v7.widget.RecyclerView

class SnappingLinearLayoutManager(val context: Context) : LinearLayoutManager(context) {
    override fun smoothScrollToPosition(recyclerView: RecyclerView?, state: RecyclerView.State?, position: Int) {
        val smoothScroller = object : LinearSmoothScroller(context) {
            override fun getVerticalSnapPreference(): Int = SNAP_TO_START
            override fun computeScrollVectorForPosition(targetPosition: Int): PointF
                    = this@SnappingLinearLayoutManager.computeScrollVectorForPosition(targetPosition)
        }
        smoothScroller.targetPosition = position
        startSmoothScroll(smoothScroller)
    }
}
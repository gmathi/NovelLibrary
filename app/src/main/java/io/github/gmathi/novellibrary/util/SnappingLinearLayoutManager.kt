package io.github.gmathi.novellibrary.util

import android.content.Context
import android.graphics.PointF
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView

class SnappingLinearLayoutManager(val context: Context) : LinearLayoutManager(context) {
    override fun smoothScrollToPosition(recyclerView: RecyclerView?, state: RecyclerView.State?, position: Int) {
        val smoothScroller = object : LinearSmoothScroller(context) {
            override fun getVerticalSnapPreference(): Int = SNAP_TO_START
            override fun computeScrollVectorForPosition(targetPosition: Int): PointF? = this@SnappingLinearLayoutManager.computeScrollVectorForPosition(targetPosition)
        }
        smoothScroller.targetPosition = position
        startSmoothScroll(smoothScroller)
    }
}
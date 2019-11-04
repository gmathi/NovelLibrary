package io.github.gmathi.novellibrary.util

import android.content.Context
import android.graphics.Rect
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import android.view.View

class CustomDividerItemDecoration(context: Context, orientation: Int) : DividerItemDecoration(context, orientation) {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        if (position == parent.adapter?.itemCount?.minus(1)) {
            outRect.setEmpty()
        } else {
            super.getItemOffsets(outRect, view, parent, state)
        }
    }
}
package io.github.gmathi.novellibrary.util.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

/**
 * A [ViewPager] whose swipe paging can be switched off. Programmatic page changes still work.
 * Used by the reader so that chapter swiping can be disabled by the user, and is always off in
 * page mode where horizontal gestures turn pages inside the chapter instead.
 */
class LockableViewPager @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ViewPager(context, attrs) {

    var isSwipeEnabled: Boolean = true

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean =
        isSwipeEnabled && super.onInterceptTouchEvent(ev)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean =
        isSwipeEnabled && super.onTouchEvent(ev)
}

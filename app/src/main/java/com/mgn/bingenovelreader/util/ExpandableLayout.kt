/**
 * Copyright 2015, KyoSherlock
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mgn.bingenovelreader.util

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Canvas
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.mgn.bingenovelreader.R

@Suppress("unused")
class ExpandableLayout : LinearLayout {
    private var mWidthMeasureSpec: Int = 0
    private var mHeightMeasureSpec: Int = 0
    private var mAttachedToWindow: Boolean = false
    private var mFirstLayout = true
    private var mInLayout: Boolean = false
    private var mExpandAnimator: ObjectAnimator? = null
    private var mListener: OnExpandListener? = null

    constructor(context: Context) : super(context) {
        this.init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        this.init()
    }

    constructor(context: Context, attrs: AttributeSet,
                defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        this.init()
    }

    @TargetApi(21)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        this.init()
    }

    private fun init() {
        this.orientation = LinearLayout.VERTICAL
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        mWidthMeasureSpec = widthMeasureSpec
        mHeightMeasureSpec = heightMeasureSpec
        val child = findExpandableView()
        if (child != null) {
            val p = child.layoutParams as LayoutParams

            if (p.weight != 0f) {
                throw IllegalArgumentException(
                    "ExpandableView can't use weight")
            }

            if (!p.isExpanded && !p.isExpanding) {
                child.visibility = View.GONE
            } else {
                child.visibility = View.VISIBLE
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        mInLayout = true
        super.onLayout(changed, l, t, r, b)
        mInLayout = false
        mFirstLayout = false
    }

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        return super.drawChild(canvas, child, drawingTime)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mAttachedToWindow = true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mAttachedToWindow = false
        val child = findExpandableView()
        if (mExpandAnimator != null && mExpandAnimator!!.isRunning) {
            mExpandAnimator!!.end()
            mExpandAnimator = null
        }
        if (child != null) {
            val p = child.layoutParams as LayoutParams
            if (p.isExpanded) {
                p.height = p.originalHeight
                child.visibility = View.VISIBLE
            } else {
                p.height = p.originalHeight
                child.visibility = View.GONE
            }
            p.isExpanding = false
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun requestLayout() {
        if (!mInLayout) {
            super.requestLayout()
        }
    }

    fun findExpandableView(): View? {
        for (i in 0..this.childCount - 1) {
            val p = this.getChildAt(i)
                .layoutParams as LayoutParams
            if (p.canExpand) {
                return this.getChildAt(i)
            }
        }
        return null
    }

    internal fun checkExpandableView(expandableView: View): Boolean {
        val p = expandableView.layoutParams as LayoutParams
        return p.canExpand
    }

    val isExpanded: Boolean
        get() {
            val child = findExpandableView()
            if (child != null) {
                val p = child.layoutParams as LayoutParams
                if (p.isExpanded) {
                    return true
                }
            }
            return false
        }

    /**
     * @return
     */
    fun toggleExpansion(): Boolean {
        return this.setExpanded(!isExpanded, true)
    }

    /**
     * @param isExpanded
     * *
     * @return
     */
    fun setExpanded(isExpanded: Boolean): Boolean {
        return this.setExpanded(isExpanded, false)
    }

    /**
     * @param isExpanded
     * *
     * @param shouldAnimate
     * *
     * @return
     */
    fun setExpanded(isExpanded: Boolean, shouldAnimate: Boolean): Boolean {
        var result = false
        val child = findExpandableView()
        if (child != null) {
            if (isExpanded != this.isExpanded) {
                if (isExpanded) {
                    result = this.expand(child, shouldAnimate)
                } else {
                    result = this.collapse(child, shouldAnimate)
                }
            }
        }
        this.requestLayout()
        return result
    }

    fun setOnExpandListener(listenr: OnExpandListener) {
        this.mListener = listenr
    }

    /**
     * @param child
     * *
     * @param shouldAnimate
     * *
     * @return
     */
    private fun expand(child: View, shouldAnimate: Boolean): Boolean {
        var result = false
        if (!checkExpandableView(child)) {
            throw IllegalArgumentException(
                "expand(), View is not expandableView")
        }
        val p = child.layoutParams as LayoutParams
        if (mFirstLayout || !mAttachedToWindow || !shouldAnimate) {
            p.isExpanded = true
            p.isExpanding = false
            p.height = p.originalHeight
            child.visibility = View.VISIBLE
            result = true
        } else {
            if (!p.isExpanded && !p.isExpanding) {
                this.playExpandAnimation(child)
                result = true
            }
        }
        return result
    }

    private fun playExpandAnimation(child: View) {
        val p = child.layoutParams as LayoutParams
        if (p.isExpanding) {
            return
        }
        child.visibility = View.VISIBLE
        p.isExpanding = true
        this.measure(mWidthMeasureSpec, mHeightMeasureSpec)
        val measuredHeight = child.measuredHeight
        p.height = 0

        mExpandAnimator = ObjectAnimator.ofInt(p, "height", 0, measuredHeight)
        mExpandAnimator!!.duration = context.resources.getInteger(
            android.R.integer.config_shortAnimTime).toLong()
        mExpandAnimator!!.addUpdateListener {
            dispatchOffset(child)
            child.requestLayout()
        }
        mExpandAnimator!!.addListener(object : AnimatorListener {

            override fun onAnimationStart(animation: Animator) {

            }

            override fun onAnimationRepeat(animation: Animator) {

            }

            override fun onAnimationEnd(animation: Animator) {
                performToggleState(child)
            }

            override fun onAnimationCancel(animation: Animator) {

            }
        })
        mExpandAnimator!!.start()
    }

    /**
     * @param child
     * *
     * @param shouldAnimation
     * *
     * @return
     */
    private fun collapse(child: View, shouldAnimation: Boolean): Boolean {
        var result = false
        if (!checkExpandableView(child)) {
            throw IllegalArgumentException(
                "collapse(), View is not expandableView")
        }
        val p = child.layoutParams as LayoutParams
        if (mFirstLayout || !mAttachedToWindow || !shouldAnimation) {
            p.isExpanded = false
            p.isExpanding = false
            p.height = p.originalHeight
            child.visibility = View.GONE
            result = true
        } else {
            if (p.isExpanded && !p.isExpanding) {
                this.playCollapseAnimation(child)
                result = true
            }
        }
        return result
    }

    private fun playCollapseAnimation(child: View) {
        val p = child.layoutParams as LayoutParams
        if (p.isExpanding) {
            return
        }
        child.visibility = View.VISIBLE
        p.isExpanding = true
        this.measure(mWidthMeasureSpec, mHeightMeasureSpec)
        val measuredHeight = child.measuredHeight

        mExpandAnimator = ObjectAnimator.ofInt(p, "height", measuredHeight, 0)
        mExpandAnimator!!.duration = context.resources.getInteger(
            android.R.integer.config_shortAnimTime).toLong()
        mExpandAnimator!!.addUpdateListener {
            dispatchOffset(child)
            child.requestLayout()
        }
        mExpandAnimator!!.addListener(object : AnimatorListener {

            override fun onAnimationStart(animation: Animator) {

            }

            override fun onAnimationRepeat(animation: Animator) {

            }

            override fun onAnimationEnd(animation: Animator) {
                performToggleState(child)
            }

            override fun onAnimationCancel(animation: Animator) {

            }
        })
        mExpandAnimator!!.start()
    }

    val isRunningAnimation: Boolean
        get() {
            val child = findExpandableView()
            val p = child!!.layoutParams as LayoutParams
            return p.isExpanding
        }

    private fun dispatchOffset(child: View) {
        if (mListener != null) {
            mListener!!.onExpandOffset(this, child, child.height.toFloat(),
                !isExpanded)
        }
    }

    private fun performToggleState(child: View) {
        val p = child.layoutParams as LayoutParams
        if (p.isExpanded) {
            p.isExpanded = false
            if (mListener != null) {
                mListener!!.onToggle(this, child, false)
            }
            child.visibility = View.GONE
            p.height = p.originalHeight
        } else {
            p.isExpanded = true
            if (mListener != null) {
                mListener!!.onToggle(this, child, true)
            }
        }
        p.isExpanding = false
    }

    override fun onSaveInstanceState(): Parcelable {
        val ss = SavedState(super.onSaveInstanceState())
        if (isExpanded) {
            ss.isExpanded = true
        }
        return ss
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val ss = state as SavedState
        super.onRestoreInstanceState(ss.superState)
        if (ss.isExpanded) {
            val child = findExpandableView()
            if (child != null) {
                setExpanded(true)
            }
        }
    }

    private class SavedState : View.BaseSavedState {

        internal var isExpanded: Boolean = false

        constructor(source: Parcel) : super(source) {
            isExpanded = source.readInt() == 1
        }

        constructor(superState: Parcelable) : super(superState)

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeInt(if (isExpanded) 1 else 0)
        }

        companion object {

            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {

                override fun createFromParcel(source: Parcel): SavedState {
                    return SavedState(source)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return LayoutParams(this.context, attrs)
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    override fun generateLayoutParams(
        p: android.view.ViewGroup.LayoutParams): LayoutParams {
        return LayoutParams(p)
    }

    override fun checkLayoutParams(p: android.view.ViewGroup.LayoutParams): Boolean {
        return super.checkLayoutParams(p) && p is LayoutParams
    }

    class LayoutParams : LinearLayout.LayoutParams {
        internal var originalHeight = NO_MEASURED_HEIGHT
        internal var isExpanded: Boolean = false
        internal var canExpand: Boolean = false
        internal var isExpanding: Boolean = false

        constructor(c: Context, attrs: AttributeSet) : super(c, attrs) {
            val a = c.obtainStyledAttributes(attrs, R.styleable.ExpandableLayout)
            canExpand = a.getBoolean(R.styleable.ExpandableLayout_canExpand,
                false)
            isExpanded = a.getBoolean(R.styleable.ExpandableLayout_startExpanded,
                false)
            originalHeight = this.height
            a.recycle()
        }

        constructor(width: Int, height: Int, weight: Float) : super(width, height, weight) {
            originalHeight = this.height
        }

        constructor(width: Int, height: Int) : super(width, height) {
            originalHeight = this.height
        }

        constructor(source: android.view.ViewGroup.LayoutParams) : super(source) {
            originalHeight = this.height
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        constructor(source: LinearLayout.LayoutParams) : super(source) {
            originalHeight = this.height
        }

        constructor(source: ViewGroup.MarginLayoutParams) : super(source) {
            originalHeight = this.height
        }

        fun setHeight(height: Int) {
            this.height = height
        }

        companion object {
            private val NO_MEASURED_HEIGHT = -10
        }
    }

    interface OnExpandListener {
        fun onToggle(view: ExpandableLayout, child: View, isExpanded: Boolean)

        fun onExpandOffset(view: ExpandableLayout, child: View,
                           offset: Float, isExpanding: Boolean)
    }
}
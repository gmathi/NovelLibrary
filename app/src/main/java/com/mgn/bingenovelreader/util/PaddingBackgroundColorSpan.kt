package com.mgn.bingenovelreader.util

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.style.LineBackgroundSpan

@Suppress("unused")
class PaddingBackgroundColorSpan(private val mBackgroundColor: Int, private val mPadding: Int) : LineBackgroundSpan {

    private val mBgRect: Rect = Rect()

    override fun drawBackground(c: Canvas, p: Paint, left: Int, right: Int, top: Int, baseline: Int, bottom: Int, text: CharSequence, start: Int, end: Int, lnum: Int) {
        val textWidth = Math.round(p.measureText(text, start, end))
        val paintColor = p.color
        // Draw the background
        mBgRect.set(left - mPadding,
            top - if (lnum == 0) mPadding / 2 else -(mPadding / 2),
            left + textWidth + mPadding,
            bottom + mPadding / 2)
        p.color = mBackgroundColor
        c.drawRect(mBgRect, p)
        p.color = paintColor
    }
}
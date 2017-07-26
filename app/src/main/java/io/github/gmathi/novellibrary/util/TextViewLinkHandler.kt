package io.github.gmathi.novellibrary.util

import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.MotionEvent
import android.widget.TextView

class TextViewLinkHandler(private val mOnClickListener: OnClickListener) : LinkMovementMethod() {

    interface OnClickListener {
        fun onLinkClicked(title: String, url: String)
    }

    override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP)
            return super.onTouchEvent(widget, buffer, event)

        var x = event.x.toInt()
        var y = event.y.toInt()

        x -= widget.totalPaddingLeft
        y -= widget.totalPaddingTop

        x += widget.scrollX
        y += widget.scrollY

        val layout = widget.layout
        val line = layout.getLineForVertical(y)
        val off = layout.getOffsetForHorizontal(line, x.toFloat())

        val link = buffer.getSpans(off, off, URLSpan::class.java)
        val linkText = buffer.getSpans(off, off, ClickableSpan::class.java)

        if (link.isNotEmpty() && linkText.isNotEmpty()) {
            mOnClickListener.onLinkClicked(buffer.subSequence(buffer.getSpanStart(link[0]),
                buffer.getSpanEnd(link[0])).toString(), link[0].url)
        }

        return true
    }

}

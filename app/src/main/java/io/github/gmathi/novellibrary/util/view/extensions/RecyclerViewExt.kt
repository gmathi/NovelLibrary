package io.github.gmathi.novellibrary.util.view

import android.view.animation.OvershootInterpolator
import androidx.recyclerview.widget.RecyclerView
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import jp.wasabeef.recyclerview.animators.SlideInRightAnimator

/**
 * Sets the Adapter, LayoutManager, Animations and fixesSize flag.
 */
fun <T> RecyclerView.setDefaults(adapter: GenericAdapter<T>): RecyclerView {
    val animator = SlideInRightAnimator(OvershootInterpolator(1f))
    animator.addDuration = 1000
    animator.removeDuration = 1000
    animator.changeDuration = 0
    animator.moveDuration = 200

    this.setHasFixedSize(true)
    this.layoutManager = SnappingLinearLayoutManager(context)
    this.itemAnimator = animator
    this.adapter = adapter

    return this
}

fun RecyclerView.setDefaultsNoAnimation(adapter: RecyclerView.Adapter<*>): RecyclerView {
    this.setHasFixedSize(true)
    this.layoutManager = SnappingLinearLayoutManager(context)
    this.adapter = adapter
    return this
}
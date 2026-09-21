@file:Suppress("NOTHING_TO_INLINE")

package io.github.gmathi.novellibrary.util.view

import android.graphics.Point
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.TooltipCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import io.github.gmathi.novellibrary.R

/**
 * Returns coordinates of view.
 * Used for animation
 *
 * @return coordinates of view
 */
fun View.getCoordinates() = Point((left + right) / 2, (top + bottom) / 2)

/**
 * Shows a snackbar in this view.
 *
 * @param message the message to show.
 * @param length the duration of the snack.
 * @param f a function to execute in the snack, allowing for example to define a custom action.
 */
inline fun View.snack(
    message: String,
    length: Int = Snackbar.LENGTH_LONG,
    f: Snackbar.() -> Unit = {}
): Snackbar {
    val snack = Snackbar.make(this, message, length)
    snack.f()
    snack.show()
    return snack
}

/**
 * Adds a tooltip shown on long press.
 *
 * @param stringRes String resource for tooltip.
 */
inline fun View.setTooltip(@StringRes stringRes: Int) {
    TooltipCompat.setTooltipText(this, context.getString(stringRes))
}

/**
 * Shows a popup menu on top of this view.
 *
 * @param menuRes menu items to inflate the menu with.
 * @param initMenu function to execute when the menu after is inflated.
 * @param onMenuItemClick function to execute when a menu item is clicked.
 */
inline fun View.popupMenu(
    @MenuRes menuRes: Int,
    noinline initMenu: (Menu.() -> Unit)? = null,
    noinline onMenuItemClick: MenuItem.() -> Boolean
): PopupMenu {
    val popup = PopupMenu(context, this, Gravity.NO_GRAVITY, androidx.appcompat.R.attr.actionOverflowMenuStyle, 0)
    popup.menuInflater.inflate(menuRes, popup.menu)

    if (initMenu != null) {
        popup.menu.initMenu()
    }
    popup.setOnMenuItemClickListener { it.onMenuItemClick() }

    popup.show()
    return popup
}

/**
 * Shrink an ExtendedFloatingActionButton when the associated RecyclerView is scrolled down.
 *
 * @param recycler [RecyclerView] that the FAB should shrink/extend in response to.
 */
inline fun ExtendedFloatingActionButton.shrinkOnScroll(recycler: RecyclerView): RecyclerView.OnScrollListener {
    val listener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (dy <= 0) {
                extend()
            } else {
                shrink()
            }
        }
    }
    recycler.addOnScrollListener(listener)
    return listener
}

/**
 * Replaces chips in a ChipGroup.
 *
 * @param items List of strings that are shown as individual chips.
 * @param onClick Optional on click listener for each chip.
 */
inline fun ChipGroup.setChips(
    items: List<String>?,
    noinline onClick: (item: String) -> Unit = {}
) {
    removeAllViews()

    items?.forEach { item ->
        val chip = Chip(context).apply {
            text = item
            setOnClickListener { onClick(item) }
        }

        addView(chip)
    }
}

/**
 * Applies window insets to this view.
 *
 * @param block function to execute with the view and system insets.
 */
inline fun View.applyInsets(noinline block: (view: View, systemInsets: Insets) -> Unit) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        block(view, systemInsets)
        insets
    }
}

/**
 * Applies top system window insets as padding to this view.
 * Useful for toolbars and app bars to avoid overlap with status bar.
 */
inline fun View.applyTopSystemWindowInsetsPadding() {
    applyInsets { view, insets ->
        view.setPadding(
            view.paddingLeft,
            insets.top,
            view.paddingRight,
            view.paddingBottom
        )
    }
}

/**
 * Applies bottom system window insets as padding to this view.
 * Useful for bottom bars/buttons to avoid overlap with the navigation bar
 * when the activity is drawn edge-to-edge.
 *
 * The view's original bottom padding is preserved and the navigation-bar inset
 * is added on top of it. If the view is a [RecyclerView] (or any scrolling
 * container), [android.view.ViewGroup.setClipToPadding] is disabled so the last
 * item can scroll into the padded region instead of being clipped.
 */
fun View.applyBottomSystemWindowInsetsPadding() {
    // Capture the design-time bottom padding once so repeated inset callbacks
    // (e.g. on rotation / keyboard) don't accumulate padding.
    val initialBottom = paddingBottom
    if (this is android.view.ViewGroup) {
        clipToPadding = false
    }
    applyInsets { view, insets ->
        view.setPadding(
            view.paddingLeft,
            view.paddingTop,
            view.paddingRight,
            initialBottom + insets.bottom
        )
    }
}

/**
 * Walks this view hierarchy to find a full-height scrolling list
 * ([RecyclerView] / [android.widget.ScrollView] /
 * [androidx.core.widget.NestedScrollView] / [android.widget.ListView]) that
 * should receive the bottom navigation-bar inset as padding (with clipToPadding
 * disabled) instead of the content root.
 *
 * Returns null when the screen is not a pure scrolling list (e.g. a form, a
 * WebView with a bottom bar). In that case the caller should pad the content
 * root so any bottom-anchored bar/button is lifted above the nav bar.
 *
 * [android.webkit.WebView] subtrees are skipped so full-bleed web content keeps
 * its edge-to-edge look.
 */
fun android.view.ViewGroup.findScrollableTarget(): View? = findScrollableChild(this)

private fun findScrollableChild(view: View): View? {
    if (view is android.webkit.WebView) return null
    when (view) {
        is RecyclerView,
        is android.widget.ScrollView,
        is androidx.core.widget.NestedScrollView,
        is android.widget.ListView -> return view
    }
    if (view is android.view.ViewGroup) {
        for (i in 0 until view.childCount) {
            findScrollableChild(view.getChildAt(i))?.let { return it }
        }
    }
    return null
}

/**
 * Adds the bottom navigation-bar inset to the bottom margin of any direct child
 * of this view group that is anchored to the bottom (bottom gravity), so a
 * bottom-anchored label/button that is a sibling of the scrolling list is lifted
 * above the Android navigation buttons.
 *
 * The original bottom margin of each child is preserved. Children that are an
 * [com.google.android.material.appbar.AppBarLayout], a [android.webkit.WebView],
 * or the [except] view (already padded elsewhere) are skipped.
 */
fun android.view.ViewGroup.applyBottomInsetMarginToBottomAnchoredChildren(except: View? = null) {
    for (i in 0 until childCount) {
        val child = getChildAt(i)
        if (child === except) continue
        if (child is com.google.android.material.appbar.AppBarLayout) continue
        if (child is android.webkit.WebView) continue
        if (!child.isAnchoredToBottom()) continue
        child.applyBottomSystemWindowInsetsMargin()
    }
}

/** True if the view's layout params place it at the bottom of its parent. */
private fun View.isAnchoredToBottom(): Boolean {
    val lp = layoutParams
    val gravity: Int = when (lp) {
        is android.widget.FrameLayout.LayoutParams -> lp.gravity
        is androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams -> lp.gravity
        is android.widget.LinearLayout.LayoutParams -> lp.gravity
        else -> return false
    }
    if (gravity <= 0) return false
    val vertical = gravity and android.view.Gravity.VERTICAL_GRAVITY_MASK
    return vertical == android.view.Gravity.BOTTOM ||
            (gravity and android.view.Gravity.BOTTOM) == android.view.Gravity.BOTTOM
}

/**
 * Adds the bottom navigation-bar inset to this view's bottom margin, preserving
 * the original margin. No-op if the view has no margin layout params.
 */
fun View.applyBottomSystemWindowInsetsMargin() {
    val lp = layoutParams as? android.view.ViewGroup.MarginLayoutParams ?: return
    val initialBottom = lp.bottomMargin
    applyInsets { view, insets ->
        val params = view.layoutParams as? android.view.ViewGroup.MarginLayoutParams
            ?: return@applyInsets
        params.bottomMargin = initialBottom + insets.bottom
        view.layoutParams = params
    }
}

package io.github.gmathi.novellibrary.activity

import android.content.Context
import android.view.ViewGroup
import com.google.android.material.appbar.AppBarLayout
import com.google.firebase.analytics.FirebaseAnalytics
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.lang.LocaleManager
import io.github.gmathi.novellibrary.util.view.applyTopSystemWindowInsetsPadding
import uy.kohesive.injekt.injectLazy


abstract class BaseActivity : io.github.gmathi.novellibrary.core.activity.BaseActivity() {

    override val firebaseAnalytics: FirebaseAnalytics by injectLazy()
    override val dataCenter: DataCenter by injectLazy()
    override val dbHelper: DBHelper by injectLazy()
    override val sourceManager: SourceManager by injectLazy()
    override val networkHelper: NetworkHelper by injectLazy()

    override fun setupEdgeToEdge() {
        // Enable edge-to-edge display
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    override fun applyWindowInsets() {
        // Automatically apply window insets to AppBarLayout
        applyWindowInsetsToAppBar()
    }

    override fun getLocaleContext(context: Context): Context {
        return LocaleManager.updateContextLocale(context)
    }

    private fun applyWindowInsetsToAppBar() {
        // Find AppBarLayout in the view hierarchy and apply top insets
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        rootView?.let { findAndApplyInsetsToAppBar(it) }
    }

    private fun findAndApplyInsetsToAppBar(viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is AppBarLayout) {
                child.applyTopSystemWindowInsetsPadding()
                return
            } else if (child is ViewGroup) {
                findAndApplyInsetsToAppBar(child)
            }
        }
    }

}
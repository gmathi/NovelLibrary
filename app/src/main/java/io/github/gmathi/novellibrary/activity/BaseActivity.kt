package io.github.gmathi.novellibrary.activity

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.AppBarLayout
import com.google.firebase.analytics.FirebaseAnalytics
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.lang.LocaleManager
import io.github.gmathi.novellibrary.util.system.DataAccessor
import io.github.gmathi.novellibrary.util.view.applyTopSystemWindowInsetsPadding
import uy.kohesive.injekt.injectLazy


abstract class BaseActivity : AppCompatActivity(), DataAccessor {

    override val firebaseAnalytics: FirebaseAnalytics by injectLazy()
    override val dataCenter: DataCenter by injectLazy()
    override val dbHelper: DBHelper by injectLazy()
    override val sourceManager: SourceManager by injectLazy()
    override val networkHelper: NetworkHelper by injectLazy()
    override fun getContext(): Context? = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge display
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    override fun onContentChanged() {
        super.onContentChanged()
        // Automatically apply window insets to AppBarLayout
        applyWindowInsetsToAppBar()
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

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.updateContextLocale(newBase))
    }

}
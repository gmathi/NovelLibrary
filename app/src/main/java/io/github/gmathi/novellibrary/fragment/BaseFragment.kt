package io.github.gmathi.novellibrary.fragment

import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.AppBarLayout
import com.google.firebase.analytics.FirebaseAnalytics
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.system.DataAccessor
import io.github.gmathi.novellibrary.util.view.applyBottomSystemWindowInsetsPadding
import io.github.gmathi.novellibrary.util.view.applyTopSystemWindowInsetsPadding
import io.github.gmathi.novellibrary.util.view.findScrollableTarget
import uy.kohesive.injekt.injectLazy


open class BaseFragment : Fragment(), DataAccessor {

    override val firebaseAnalytics: FirebaseAnalytics by injectLazy()
    override val dataCenter: DataCenter by injectLazy()
    override val dbHelper: DBHelper by injectLazy()
    override val sourceManager: SourceManager by injectLazy()
    override val networkHelper: NetworkHelper by injectLazy()

    override fun onViewCreated(view: View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Automatically apply window insets to AppBarLayout
        applyWindowInsetsToAppBar(view)
        // Keep content above the Android navigation bar (bottom insets).
        // Fragments are hosted in a DrawerLayout activity that skips bottom
        // insets, so they must apply them here.
        applyBottomWindowInsets(view)
    }

    private fun applyWindowInsetsToAppBar(view: View) {
        if (view is AppBarLayout) {
            view.applyTopSystemWindowInsetsPadding()
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyWindowInsetsToAppBar(view.getChildAt(i))
            }
        }
    }

    private fun applyBottomWindowInsets(view: View) {
        // Only pad a discovered scrollable list (with clipToPadding disabled).
        // Padding the whole fragment root here would push non-scrolling content
        // up unnecessarily, and Compose fragments manage their own insets.
        val target = (view as? ViewGroup)?.findScrollableTarget()
        target?.applyBottomSystemWindowInsetsPadding()
    }

}

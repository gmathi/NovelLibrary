package io.github.gmathi.novellibrary.util.system

import android.content.Context
import androidx.lifecycle.Lifecycle
import com.google.firebase.analytics.FirebaseAnalytics
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter

/**
 * A common interface for helper functions that perform complex actions such as updating DB, do novel sync, fire firebase events, etc.
 *
 * See DataAccessorExt.kt for methods.
 */
interface DataAccessor {

    val firebaseAnalytics: FirebaseAnalytics
    val dataCenter: DataCenter
    val dbHelper: DBHelper
    val sourceManager: SourceManager
    val networkHelper: NetworkHelper
    fun getContext(): Context?

}
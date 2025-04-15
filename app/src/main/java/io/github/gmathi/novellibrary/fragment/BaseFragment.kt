package io.github.gmathi.novellibrary.fragment

import androidx.fragment.app.Fragment
import com.google.firebase.analytics.FirebaseAnalytics
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.system.DataAccessor
import uy.kohesive.injekt.injectLazy


open class BaseFragment : Fragment(), DataAccessor {

    override val firebaseAnalytics: FirebaseAnalytics by injectLazy()
    override val dataCenter: DataCenter by injectLazy()
    override val dbHelper: DBHelper by injectLazy()
    override val sourceManager: SourceManager by injectLazy()
    override val networkHelper: NetworkHelper by injectLazy()

}

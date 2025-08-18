package io.github.gmathi.novellibrary.fragment

import androidx.fragment.app.Fragment
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.system.DataAccessor
import javax.inject.Inject


@AndroidEntryPoint
open class BaseFragment : Fragment(), DataAccessor {

    @Inject override lateinit var firebaseAnalytics: FirebaseAnalytics
    @Inject override lateinit var dataCenter: DataCenter
    @Inject override lateinit var dbHelper: DBHelper
    @Inject override lateinit var sourceManager: SourceManager
    @Inject override lateinit var networkHelper: NetworkHelper

}

package io.github.gmathi.novellibrary.activity

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.lang.LocaleManager
import io.github.gmathi.novellibrary.util.system.DataAccessor
import javax.inject.Inject

@AndroidEntryPoint
abstract class BaseActivity : AppCompatActivity(), DataAccessor {

    @Inject override lateinit var firebaseAnalytics: FirebaseAnalytics
    @Inject override lateinit var dataCenter: DataCenter
    @Inject override lateinit var dbHelper: DBHelper
    @Inject override lateinit var sourceManager: SourceManager
    @Inject override lateinit var networkHelper: NetworkHelper
    override fun getContext(): Context? = this

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.updateContextLocale(newBase))
    }

}
package io.github.gmathi.novellibrary.activity

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.lang.LocaleManager
import io.github.gmathi.novellibrary.util.system.DataAccessor
import uy.kohesive.injekt.injectLazy


abstract class BaseActivity : AppCompatActivity(), DataAccessor {

    override val firebaseAnalytics: FirebaseAnalytics by injectLazy()
    override val dataCenter: DataCenter by injectLazy()
    override val dbHelper: DBHelper by injectLazy()
    override val sourceManager: SourceManager by injectLazy()
    override val networkHelper: NetworkHelper by injectLazy()
    override fun getContext(): Context? = this

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.updateContextLocale(newBase))
    }

}
package io.github.gmathi.novellibrary.activity

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.DataCenter
import io.github.gmathi.novellibrary.util.lang.LocaleManager
import uy.kohesive.injekt.injectLazy


abstract class BaseActivity : AppCompatActivity() {

    lateinit var firebaseAnalytics: FirebaseAnalytics

    val dataCenter: DataCenter by injectLazy()
    val dbHelper: DBHelper by injectLazy()
    val sourceManager: SourceManager by injectLazy()
    val networkHelper: NetworkHelper by injectLazy()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.updateContextLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Obtain the FirebaseAnalytics instance.
        firebaseAnalytics = Firebase.analytics
    }
}
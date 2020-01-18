package io.github.gmathi.novellibrary.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_META_DATA
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.gmathi.novellibrary.util.LocaleManager


abstract class BaseActivity : AppCompatActivity() {

    @SuppressLint("ObsoleteSdkInt")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            try {
                val label: Int = packageManager.getActivityInfo(componentName, GET_META_DATA).labelRes
                if (label != 0)
                    setTitle(label)
            } catch (e: PackageManager.NameNotFoundException) {
                // ignore
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.updateContextLocale(newBase))
    }

    @Deprecated(message = "This is a workaround for a bug in AppCompat 1.1.0 that will be fixed in 1.2.0!", level = DeprecationLevel.WARNING)
    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        if (overrideConfiguration != null) {
            val uiMode = overrideConfiguration.uiMode
            overrideConfiguration.setTo(baseContext.resources.configuration)
            overrideConfiguration.uiMode = uiMode
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }
}
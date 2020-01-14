package io.github.gmathi.novellibrary.activity

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.Display
import androidx.appcompat.app.AppCompatActivity
import io.github.gmathi.novellibrary.util.ContextLocaleWrapper
import io.github.gmathi.novellibrary.util.ContextLocaleWrapper.Companion.getLanguage

abstract class BaseActivity : AppCompatActivity() {

    protected open val titleRes: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (titleRes != null) {
            val titleShouldBe: String = resources.getString(titleRes!!)
            if (title.toString() != titleShouldBe)
                title = titleShouldBe
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ContextLocaleWrapper.wrapContextWithLocale(newBase, getLanguage()))
    }

    override fun getApplicationContext(): Context {
        return ContextLocaleWrapper.wrapContextWithLocale(super.getApplicationContext(), getLanguage())
    }

    override fun getBaseContext(): Context {
        return ContextLocaleWrapper.wrapContextWithLocale(super.getBaseContext(), getLanguage())
    }

    override fun createConfigurationContext(overrideConfiguration: Configuration): Context {
        return ContextLocaleWrapper.wrapContextWithLocale(super.createConfigurationContext(overrideConfiguration), getLanguage())
    }

    override fun createDeviceProtectedStorageContext(): Context {
        return ContextLocaleWrapper.wrapContextWithLocale(super.createDeviceProtectedStorageContext(), getLanguage())
    }

    override fun createDisplayContext(display: Display): Context {
        return ContextLocaleWrapper.wrapContextWithLocale(super.createDisplayContext(display), getLanguage())
    }

    override fun createPackageContext(packageName: String?, flags: Int): Context {
        return ContextLocaleWrapper.wrapContextWithLocale(super.createPackageContext(packageName, flags), getLanguage())
    }

    override fun createContextForSplit(splitName: String?): Context {
        return ContextLocaleWrapper.wrapContextWithLocale(super.createContextForSplit(splitName), getLanguage())
    }
}
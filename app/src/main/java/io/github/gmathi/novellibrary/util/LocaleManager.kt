package io.github.gmathi.novellibrary.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Build.VERSION_CODES.JELLY_BEAN_MR1
import io.github.gmathi.novellibrary.dataCenter
import java.util.*

class LocaleManager {

    companion object {

        private fun getLanguage(): String {
            return try {
                dataCenter.language.split('_')[0]
            } catch (e: KotlinNullPointerException) {
                "systemDefault"
            }
        }

        @SuppressLint("ObsoleteSdkInt")
        @Suppress("DEPRECATION")
        fun updateContextLocale(context: Context, language: String = getLanguage()): Context {
            if (language == "systemDefault")
                return context
            val config = Configuration(context.resources.configuration)
            val locale = Locale(language)
            Locale.setDefault(locale)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocale(locale)
                context.createConfigurationContext(config)
            } else {
                val res: Resources = context.resources
                if (Build.VERSION.SDK_INT >= JELLY_BEAN_MR1) {
                    config.setLocale(locale)
                } else {
                    config.locale = locale
                }
                res.updateConfiguration(config, res.displayMetrics)
                context
            }
        }
    }
}
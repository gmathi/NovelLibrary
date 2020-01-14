package io.github.gmathi.novellibrary.util

import android.annotation.TargetApi
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import io.github.gmathi.novellibrary.dataCenter
import java.util.*

@Suppress("DEPRECATION")
class ContextLocaleWrapper(base: Context)
    : ContextWrapper(base) {

    companion object {

        fun getLanguage(): String {
            val language: String = try {
                dataCenter.language.split('_')[0]
            } catch (e: KotlinNullPointerException) {
                "en"
            }
            android.util.Log.i("LANGUAGE!", language)
            return language
        }

        private fun isContextWithLocale(config: Configuration, language: String): Boolean {
            val sysLocale: Locale? =
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N)
                        getSystemLocale(config)
                    else getSystemLocaleLegacy(config)
            return language == sysLocale?.language
        }

        private fun getSystemLocaleLegacy(config: Configuration): Locale? {
            return config.locale
        }

        @TargetApi(Build.VERSION_CODES.N)
        private fun getSystemLocale(config: Configuration): Locale? {
            return config.locales[0]
        }

        private fun setSystemLocaleLegacy(config: Configuration, locale: Locale?) {
            config.locale = locale
        }

        @TargetApi(Build.VERSION_CODES.N)
        private fun setSystemLocale(config: Configuration, locale: Locale?) {
            config.setLocale(locale)
        }

        fun wrapContextWithLocale(context: Context, language: String): ContextWrapper {
            val config: Configuration = context.resources.configuration
            if (!isContextWithLocale(config, language)) {
                val locale = Locale(language)
                Locale.setDefault(locale)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    setSystemLocale(config, locale)
                } else {
                    setSystemLocaleLegacy(config, locale)
                }
                return ContextLocaleWrapper(context.createConfigurationContext(config))
            }
            return if (context is ContextWrapper) context else ContextWrapper(context)
        }
    }
}
package io.github.gmathi.novellibrary.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Build.VERSION_CODES.JELLY_BEAN_MR1
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.util.Constants.SYSTEM_DEFAULT
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Locale
import java.util.MissingResourceException
import kotlin.collections.HashMap
import kotlin.system.exitProcess


class LocaleManager {

    companion object {

        private fun toResourceLocale(language: String): String {
            return when (language) {
                "id" -> "in"
                else -> language
            }
        }

        private const val englishLanguage = "en"

        private var translated: HashMap<String, Int> = HashMap()

        @Synchronized
        fun translated(context: Context, language: String = englishLanguage): Int {
            if (language == SYSTEM_DEFAULT)
                return -1
            if (translated.isEmpty()) {
                val reader: BufferedReader
                val stringBuilder = StringBuilder()
                try {
                    reader = BufferedReader(InputStreamReader(context.assets.open("translations.json")))
                    var mLine = reader.readLine()
                    while (mLine != null) {
                        stringBuilder.append(mLine)
                        mLine = reader.readLine()
                    }
                    val type = object : TypeToken<Map<String, String>>() {}.type
                    val json: LinkedTreeMap<String, String> = Gson().fromJson(stringBuilder.toString(), type)
                    for (pair in json) {
                        translated[pair.key] = pair.value.toInt()
                    }
                } catch (e: IOException) {
                    Logs.error("LocaleManager", e.localizedMessage, e)
                    translated.clear()
                }
            }
            return translated[toResourceLocale(language)] ?: -1
        }

        @SuppressLint("ObsoleteSdkInt")
        @Suppress("DEPRECATION")
        private fun getResourcesLocale(context: Context, language: String = englishLanguage): Resources? {
            if (language == SYSTEM_DEFAULT)
                return null
            val locale = Locale(language)
            return if (Build.VERSION.SDK_INT >= JELLY_BEAN_MR1) {
                val config = Configuration(context.resources.configuration)
                config.setLocale(locale)
                context.createConfigurationContext(config).resources
            } else null
        }

        private fun getLanguage(): String {
            return try {
                dataCenter.language
            } catch (e: KotlinNullPointerException) {
                SYSTEM_DEFAULT
            }
        }

        @SuppressLint("ObsoleteSdkInt")
        @Suppress("DEPRECATION")
        fun updateContextLocale(context: Context, language: String = getLanguage()): Context {
            if (language == SYSTEM_DEFAULT)
                return context
            val config = Configuration(context.resources.configuration)
            val locale = Locale(language)
            try {
                if (locale.isO3Language.isEmpty())
                    return context
            } catch (e: MissingResourceException) {
                return context
            } catch (e: NullPointerException) {
                return context
            }
            Locale.setDefault(locale)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
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

        fun changeLocale(context: Context, language: String) {
            if (dataCenter.language != language) {
                dataCenter.language = language
                val intent = context.packageManager
                        .getLaunchIntentForPackage(context.packageName)!!
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                if (context is AppCompatActivity)
                    context.finish()
                exitProcess(0)
            }
        }
    }
}
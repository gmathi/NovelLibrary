package io.github.gmathi.novellibrary.util.lang

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.util.Constants.SYSTEM_DEFAULT
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.util.logging.Logs
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
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

        private fun getLanguage(context: Context): String = DataCenter(context).language


        fun updateContextLocale(context: Context, language: String = getLanguage(context)): Context {
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
            config.setLocale(locale)
            return context.createConfigurationContext(config)
        }

        fun changeLocale(context: Context, language: String) {
            val dataCenter = DataCenter(context)
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
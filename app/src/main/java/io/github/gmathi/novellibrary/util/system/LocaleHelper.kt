package io.github.gmathi.novellibrary.util.system

import android.content.Context
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.model.preference.DataCenter
import uy.kohesive.injekt.injectLazy
import java.util.Locale

/**
 * Utility class to change the application's language in runtime.
 */
@Suppress("DEPRECATION")
object LocaleHelper {

    private val dataCenter: DataCenter by injectLazy()

    private var systemLocale: Locale? = null

    /**
     * The application's locale. When it's null, the system locale is used.
     */
    private var appLocale = getLocaleFromString(dataCenter.language)

    /**
     * The currently applied locale. Used to avoid losing the selected language after a non locale
     * configuration change to the application.
     */
    private var currentLocale: Locale? = null

    /**
     * Returns the locale for the value stored in preferences, or null if it's system language.
     *
     * @param pref the string value stored in preferences.
     */
    fun getLocaleFromString(pref: String?): Locale? {
        if (pref.isNullOrEmpty()) {
            return null
        }
        return getLocale(pref)
    }

    /**
     * Returns Display name of a string language code
     */
    fun getSourceDisplayName(lang: String?, context: Context): String {
        return when (lang) {
            "" -> context.getString(R.string.other_source)
            "all" -> context.getString(R.string.all_lang)
            else -> getDisplayName(lang)
        }
    }

    /**
     * Returns Display name of a string language code
     */
    fun getDisplayName(lang: String?): String {
        return when (lang) {
            null -> ""
            "" -> {
                systemLocale!!.getDisplayName(systemLocale!!).capitalize()
            }
            else -> {
                val locale = getLocale(lang)
                locale.getDisplayName(locale).capitalize()
            }
        }
    }

    /**
     * Return Locale from string language code
     */
    private fun getLocale(lang: String): Locale {
        val sp = lang.split("_", "-")
        return when (sp.size) {
            2 -> Locale(sp[0], sp[1])
            3 -> Locale(sp[0], sp[1], sp[2])
            else -> Locale(lang)
        }
    }

}

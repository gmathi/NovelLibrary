package io.github.gmathi.novellibrary

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.database.sqlite.SQLiteException
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleObserver
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.security.ProviderInstaller
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.DBKeys
import io.github.gmathi.novellibrary.database.deleteWebPageSettings
import io.github.gmathi.novellibrary.database.deleteWebPages
import io.github.gmathi.novellibrary.model.other.SelectorQuery
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.MultiTrustManager
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.util.logging.Logs
import io.github.gmathi.novellibrary.util.notification.Notifications
import io.github.gmathi.novellibrary.util.lang.LocaleManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.injectLazy
import uy.kohesive.injekt.registry.default.DefaultRegistrar
import java.io.File
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext


open class NovelLibraryApplication : Application(), LifecycleObserver {
    companion object {
        private const val TAG = "NovelLibraryApplication"

        /**
         * Deletes the corrupt database and rebuilds a fresh schema. This is destructive:
         * it removes the user's entire library, so it must only be invoked from an explicit
         * user action (e.g. a confirmation dialog), never automatically at startup.
         *
         * @return true if the database was recreated, false if recovery failed.
         */
        fun recoverFromCorruptDatabase(context: Context): Boolean {
            return try {
                context.applicationContext.deleteDatabase(DBKeys.DATABASE_NAME)
                // Force a fresh helper instance so the next access rebuilds the schema.
                DBHelper.refreshInstance(context.applicationContext)
                true
            } catch (e: Exception) {
                Logs.error(TAG, "recoverFromCorruptDatabase(): failed to recreate database", e)
                false
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        Injekt = InjektScope(DefaultRegistrar())
        Injekt.importModule(AppModule(this))

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        cleanupDatabase()

        val imagesDir = File(filesDir, "images")
        if (!imagesDir.exists())
            imagesDir.mkdir()

        val dataCenter: DataCenter by injectLazy()

        setPreferences(dataCenter)

        // Apply app night mode preference (does not affect reader theming)
        AppCompatDelegate.setDefaultNightMode(
            if (dataCenter.appNightMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        try {
            enableSSLSocket()
        } catch (e: Exception) {
            Logs.error(TAG, "enableSSLSocket(): ${e.localizedMessage}", e)
        }

        //BugFix for <5.0 devices
        //https://stackoverflow.com/questions/29916962/javax-net-ssl-sslhandshakeexception-javax-net-ssl-sslprotocolexception-ssl-han
        updateAndroidSecurityProvider()

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        setRemoteConfig(dataCenter)
        setupNotificationChannels()
    }

    // Marker file present only while the (non-critical) startup DB cleanup is running.
    private val cleanupSentinel: File
        get() = File(filesDir, "db_cleanup_in_progress")

    private fun cleanupDatabase() {
        // If this marker still exists, the previous run died *during* cleanup. That fault is
        // an uncatchable native SQLite crash on a corrupt DB page (see tombstone: pread ->
        // unixRead -> readDbPage), which try/catch and background threads cannot stop. Skip
        // this non-critical maintenance to break the boot loop instead of crashing again.
        if (cleanupSentinel.exists()) {
            Logs.error(TAG, "cleanupDatabase(): skipping - previous attempt crashed the process (likely DB corruption)")
            return
        }

        // Run off the main thread: nothing else in onCreate() depends on it, and this avoids
        // blocking boot on slow disk I/O.
        Thread {
            try {
                cleanupSentinel.createNewFile()

                val dbHelper: DBHelper by injectLazy()

                //Stray webPages to be deleted
                dbHelper.deleteWebPages(-1L)
                dbHelper.deleteWebPageSettings(-1L)
            } catch (e: SQLiteException) {
                // Catchable disk/IO error on a corrupt DB. Do NOT wipe the database
                // automatically - that would silently destroy the user's whole library.
                // Instead, flag it so the UI can offer recovery as an explicit choice.
                Logs.error(TAG, "cleanupDatabase(): database error detected, flagging for user-initiated recovery", e)
                val dataCenter: DataCenter by injectLazy()
                dataCenter.databaseCorruptionDetected = true
            } catch (e: Exception) {
                // Never let database cleanup crash startup for any other reason.
                Logs.error(TAG, "cleanupDatabase(): unexpected error", e)
            } finally {
                // Reached only if cleanup did NOT natively crash the process. Clearing the
                // marker re-enables cleanup on the next launch.
                cleanupSentinel.delete()
            }
        }.apply {
            name = "db-cleanup"
            priority = Thread.MIN_PRIORITY
            isDaemon = true
        }.start()
    }

    private fun setPreferences(dataCenter: DataCenter) {
        dataCenter.fooled = false
        if (!dataCenter.hasAlreadyDeletedOldChannels) {
            val notificationManager = NotificationManagerCompat.from(applicationContext)
            notificationManager.deleteNotificationChannel("default")
            notificationManager.deleteNotificationChannel("io.github.gmathi.novellibrary.service.tts.NOW_PLAYING")
            dataCenter.hasAlreadyDeletedOldChannels = true
        }
        HostNames.hostNamesList = dataCenter.getVerifiedHosts()
        HostNames.defaultHostNamesList.forEach {
            HostNames.addHost(it)
        }
    }

    @Throws(KeyManagementException::class, NoSuchAlgorithmException::class)
    private fun enableSSLSocket() {
        HttpsURLConnection.setDefaultHostnameVerifier { hostName: String?, _ ->
            if (hostName != null) HostNames.isVerifiedHost(hostName) else false
        }
        val context = SSLContext.getInstance("TLS")
        val multiTrustManager = MultiTrustManager()
        multiTrustManager.addDefaultTrustManager()
        context.init(null, arrayOf(multiTrustManager), SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(context.socketFactory)
    }

    private fun updateAndroidSecurityProvider() {
        try {
            ProviderInstaller.installIfNeeded(this)
        } catch (e: GooglePlayServicesNotAvailableException) {
            Logs.error("SecurityException", "Google Play Services not available.")
        } catch (e: Exception) {
            Logs.error("Exception", "Other Exception: ${e.localizedMessage}", e)
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleManager.updateContextLocale(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LocaleManager.updateContextLocale(this)
    }

    private fun setRemoteConfig(dataCenter: DataCenter) {
        try {
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            remoteConfig.setConfigSettingsAsync(FirebaseRemoteConfigSettings.Builder().build())
            val defaults = HashMap<String, Any>()
            defaults[Constants.RemoteConfig.ADDITIVE_SELECTOR_QUERIES] = "[]"
            remoteConfig.setDefaultsAsync(defaults)
            remoteConfig.fetchAndActivate().addOnCompleteListener {
                try {
                    var selectorQueries = remoteConfig.getString(Constants.RemoteConfig.ADDITIVE_SELECTOR_QUERIES)
                    if (selectorQueries.isBlank()) selectorQueries = "[]"
                    dataCenter.htmlCleanerSelectorQueries = Gson().fromJson(selectorQueries, object : TypeToken<ArrayList<SelectorQuery>>() {}.type)
                } catch (e: Exception) {
                    Logs.error("NovelLibraryApplication", "addOnCompleteListener", e)
                }
            }
        } catch (e: Exception) {
            Logs.error("NovelLibraryApplication", "setRemoteConfig", e)
        }
    }

    protected open fun setupNotificationChannels() {
        Notifications.createChannels(this)
    }

}
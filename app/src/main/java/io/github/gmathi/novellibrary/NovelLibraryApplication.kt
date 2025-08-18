package io.github.gmathi.novellibrary

import android.app.Application
import android.content.Context
import android.content.res.Configuration
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
import dagger.hilt.android.HiltAndroidApp
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.deleteWebPageSettings
import io.github.gmathi.novellibrary.database.deleteWebPages
import io.github.gmathi.novellibrary.model.other.SelectorQuery
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.MultiTrustManager
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.notification.Notifications
import io.github.gmathi.novellibrary.util.lang.LocaleManager
import java.io.File
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext


@HiltAndroidApp
open class NovelLibraryApplication : Application(), LifecycleObserver {
    companion object {
        private const val TAG = "NovelLibraryApplication"
    }

    @Inject
    lateinit var dataCenter: DataCenter

    @Inject
    lateinit var dbHelper: DBHelper

    @Inject
    lateinit var migrationValidator: io.github.gmathi.novellibrary.util.migration.MigrationValidator

    @Inject
    lateinit var migrationLogger: io.github.gmathi.novellibrary.util.migration.MigrationLogger

    override fun onCreate() {
        super.onCreate()

        // Hilt handles dependency injection automatically
        // Removed Injekt initialization:
        // Injekt = InjektScope(DefaultRegistrar())
        // Injekt.importModule(AppModule(this))

        // Validate Hilt dependency injection
        validateHiltMigration()

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        cleanupDatabase()

        val imagesDir = File(filesDir, "images")
        if (!imagesDir.exists())
            imagesDir.mkdir()

        setPreferences(dataCenter)

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

    private fun cleanupDatabase() {
        //Stray webPages to be deleted
        dbHelper.deleteWebPages(-1L)
        dbHelper.deleteWebPageSettings(-1L)
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

    /**
     * Validates that Hilt dependency injection is working correctly.
     * This is part of the migration validation process.
     */
    private fun validateHiltMigration() {
        try {
            migrationLogger.logPhaseStart("Application Startup Validation")
            
            val isValid = migrationValidator.validateDependencies()
            
            if (isValid) {
                migrationLogger.logValidationResult("Application Dependencies", true, "All core dependencies validated successfully")
                Logs.info(TAG, "Hilt migration validation passed - all dependencies properly injected")
            } else {
                migrationLogger.logValidationResult("Application Dependencies", false, "Some dependencies failed validation")
                Logs.error(TAG, "Hilt migration validation failed - check dependency injection")
            }
            
            migrationLogger.logPhaseComplete("Application Startup Validation")
        } catch (e: Exception) {
            migrationLogger.logError("Application Startup Validation", e)
            Logs.error(TAG, "Error during Hilt migration validation", e)
        }
    }

}
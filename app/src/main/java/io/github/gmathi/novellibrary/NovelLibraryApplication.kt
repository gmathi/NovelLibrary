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
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize dependency injection first
        Injekt = InjektScope(DefaultRegistrar())
        Injekt.importModule(AppModule(this))

        // Enable vector drawables
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        // Defer heavy operations to background thread
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            cleanupDatabase()
            
            val imagesDir = File(filesDir, "images")
            if (!imagesDir.exists())
                imagesDir.mkdir()
        }

        val dataCenter: DataCenter by injectLazy()

        // Set preferences synchronously (light operation)
        setPreferences(dataCenter)

        // Defer network setup to background
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                enableSSLSocket()
            } catch (e: Exception) {
                Logs.error(TAG, "enableSSLSocket(): ${e.localizedMessage}", e)
            }

            //BugFix for <5.0 devices
            //https://stackoverflow.com/questions/29916962/javax-net-ssl-sslhandshakeexception-javax-net-ssl-sslprotocolexception-ssl-han
            updateAndroidSecurityProvider()
        }

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        // Defer remote config to background
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            setRemoteConfig(dataCenter)
        }
        
        setupNotificationChannels()
    }

    private fun cleanupDatabase() {
        val dbHelper: DBHelper by injectLazy()

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

}
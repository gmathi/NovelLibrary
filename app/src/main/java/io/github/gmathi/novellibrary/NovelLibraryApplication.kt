package io.github.gmathi.novellibrary

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.CoilUtils
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
import io.github.gmathi.novellibrary.database.DatabaseCache
import io.github.gmathi.novellibrary.util.storage.CacheManager
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


open class NovelLibraryApplication : Application(), ImageLoaderFactory {
    companion object {
        private const val TAG = "NovelLibraryApplication"
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
        
        // Setup process lifecycle observer for proper cache management
        setupProcessLifecycleObserver()
    }

    /**
     * Setup process lifecycle observer to handle cache cleanup when app is swiped out
     */
    private fun setupProcessLifecycleObserver() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
        Logs.debug(TAG, "Process lifecycle observer setup completed")
    }

    /**
     * Lifecycle observer for handling app lifecycle events
     */
    private inner class AppLifecycleObserver : DefaultLifecycleObserver {
        
        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)
            Logs.debug(TAG, "App coming to foreground")
            // Optionally warm up cache or perform other initialization
        }
        
        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)
            Logs.debug(TAG, "App going to background - preparing for potential termination")
            // Optionally perform lightweight cleanup here
        }
        
        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            Logs.debug(TAG, "App process being destroyed - cleaning up caches")
            cleanupCaches()
        }
    }

    /**
     * Clean up all caches when app is being destroyed
     */
    private fun cleanupCaches() {
        try {
            // Cleanup database cache
            DatabaseCache.getInstance().shutdown()
            Logs.debug(TAG, "Database cache cleanup completed")
            
            // Cleanup file cache manager
            CacheManager.getInstance(applicationContext).shutdown()
            Logs.debug(TAG, "Cache manager cleanup completed")
            
        } catch (e: Exception) {
            Logs.error(TAG, "Error during cache cleanup", e)
        }
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

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // 25% of available memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "image_cache"))
                    .maxSizePercent(0.02) // 2% of available disk space
                    .build()
            }
            .respectCacheHeaders(false) // Always cache images
            .crossfade(true) // Enable crossfade animations
            .crossfade(300) // 300ms crossfade duration
            .build()
    }
}
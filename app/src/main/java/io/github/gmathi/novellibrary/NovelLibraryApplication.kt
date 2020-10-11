package io.github.gmathi.novellibrary

import android.content.Context
import android.content.res.Configuration
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import androidx.multidex.MultiDexApplication
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.security.ProviderInstaller
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.deleteWebPageSettings
import io.github.gmathi.novellibrary.database.deleteWebPages
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.MultiTrustManager
import io.github.gmathi.novellibrary.service.sync.BackgroundNovelSyncTask
import io.github.gmathi.novellibrary.util.DataCenter
import io.github.gmathi.novellibrary.util.LocaleManager
import io.github.gmathi.novellibrary.util.Logs
import java.io.File
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext


val dataCenter: DataCenter by lazy {
    NovelLibraryApplication.dataCenter!!
}

val dbHelper: DBHelper by lazy {
    NovelLibraryApplication.dbHelper!!
}

class NovelLibraryApplication : MultiDexApplication() {
    companion object {
        var dataCenter: DataCenter? = null
        var dbHelper: DBHelper? = null

        private const val TAG = "NovelLibraryApplication"

        fun refreshDBHelper(context: Context) {
            dbHelper = DBHelper.refreshInstance(context)
        }
    }

    override fun onCreate() {
        dataCenter = DataCenter(applicationContext)
        dbHelper = DBHelper.getInstance(applicationContext)
        val date = Calendar.getInstance()
        if (date.get(Calendar.MONTH) == 4 && date.get(Calendar.DAY_OF_MONTH) == 1) {
            if (!dataCenter?.fooled!!) {
                dataCenter?.language = "pa"
                dataCenter?.fooled = true
            }
        } else dataCenter?.fooled = false
        super.onCreate()

        if (dataCenter?.hasAlreadyDeletedOldChannels == false) {
            deleteOldNotificationChannels()
        }

        //Stray webPages to be deleted
        dbHelper?.deleteWebPages(-1L)
        dbHelper?.deleteWebPageSettings(-1L)

//        dataCenter?.isDeveloper = true
//        dataCenter?.lockRoyalRoad = false
//
        try {
            HostNames.hostNamesList = dataCenter!!.getVerifiedHosts()
            HostNames.defaultHostNamesList.forEach {
                HostNames.addHost(it)
            }
        } catch (e: Exception) {
            Logs.error(TAG, "Set the HostNames.hostNamesList from dataCenter", e)
        }

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        val imagesDir = File(filesDir, "images")
        if (!imagesDir.exists())
            imagesDir.mkdir()

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

        if (dataCenter!!.enableNotifications)
            startSyncService()
    }

    @Deprecated("This method deletes old notification channels. Assuming that all users updated and run the app at least once, this method should be removed!")
    private fun deleteOldNotificationChannels() {
        val notificationManager = NotificationManagerCompat.from(applicationContext)
        notificationManager.deleteNotificationChannel("default")
        notificationManager.deleteNotificationChannel("io.github.gmathi.novellibrary.service.tts.NOW_PLAYING")
        dataCenter?.hasAlreadyDeletedOldChannels = true
    }

    @Throws(KeyManagementException::class, NoSuchAlgorithmException::class)
    private fun enableSSLSocket() {
        HttpsURLConnection.setDefaultHostnameVerifier { hostName: String?, _ ->
            if (hostName != null) HostNames.isVerifiedHost(hostName) else false
        }
        val context = SSLContext.getInstance("TLS")
        context.init(null, arrayOf(MultiTrustManager()), SecureRandom())
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

    private fun startSyncService() {
        BackgroundNovelSyncTask.scheduleRepeat(applicationContext)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleManager.updateContextLocale(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LocaleManager.updateContextLocale(this)
    }

}
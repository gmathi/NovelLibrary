package io.github.gmathi.novellibrary

import android.app.Application
import android.os.Build
import android.support.v7.app.AppCompatDelegate
import android.util.Log
import android.webkit.WebView
import com.squareup.leakcanary.LeakCanary
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.util.DataCenter
import java.io.File
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager


val dataCenter: DataCenter by lazy {
    NovelLibraryApplication.dataCenter!!
}

val dbHelper: DBHelper by lazy {
    NovelLibraryApplication.dbHelper!!
}

class NovelLibraryApplication : Application() {
    companion object {
        var dataCenter: DataCenter? = null
        var dbHelper: DBHelper? = null
    }

    override fun onCreate() {
        dataCenter = DataCenter(applicationContext)
        dbHelper = DBHelper(applicationContext)
        super.onCreate()
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        if (LeakCanary.isInAnalyzerProcess(this)) {
            return
        }
        LeakCanary.install(this)

        val imagesDir = File(filesDir, "images")
        if (!imagesDir.exists())
            imagesDir.mkdir()

        try {
            enableSSLSocket()
        } catch (e: Exception) {
            Log.e("NovelLibraryApplication", e.localizedMessage, e)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    @Throws(KeyManagementException::class, NoSuchAlgorithmException::class)
    fun enableSSLSocket() {
        HttpsURLConnection.setDefaultHostnameVerifier {
            hostName: String?, _ ->
            if (hostName != null) HostNames.isVerifiedHost(hostName) else false
        }

        val context = SSLContext.getInstance("TLS")
        context.init(null, arrayOf<X509TrustManager>(object : X509TrustManager {
            @Throws(CertificateException::class)
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
            }

            override fun getAcceptedIssuers(): Array<X509Certificate?> {
                return arrayOfNulls(0)
            }
        }), SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(context.socketFactory)
    }
}
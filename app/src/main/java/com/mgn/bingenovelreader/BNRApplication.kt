package com.mgn.bingenovelreader

import android.app.Application
import android.os.Build
import android.util.Log
import android.webkit.WebView
import com.mgn.bingenovelreader.database.DBHelper
import com.mgn.bingenovelreader.util.Constants
import com.mgn.bookmark.util.DataCenter
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
    BNRApplication.dataCenter!!
}

val dbHelper: DBHelper by lazy {
    BNRApplication.dbHelper!!
}

class BNRApplication : Application() {
    companion object {
        var dataCenter: DataCenter? = null
        var dbHelper: DBHelper? = null
    }

    override fun onCreate() {
        dataCenter = DataCenter(applicationContext)
        dbHelper = DBHelper(applicationContext)
        super.onCreate()

        val imagesDir = File(filesDir, "images")
        if (!imagesDir.exists())
            imagesDir.mkdir()

        try {
            enableSSLSocket()
        } catch (e: Exception) {
            Log.e("BNRApplication", e.localizedMessage, e)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    @Throws(KeyManagementException::class, NoSuchAlgorithmException::class)
    fun enableSSLSocket() {
        HttpsURLConnection.setDefaultHostnameVerifier {
            hostName: String?, _ ->
            if (hostName != null) Constants.allowedWebsites.contains(hostName) else false
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
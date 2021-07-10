package io.github.gmathi.novellibrary.network

import android.content.Context
import android.net.ConnectivityManager
import coil.util.CoilUtils
import io.github.gmathi.novellibrary.BuildConfig
import io.github.gmathi.novellibrary.network.interceptor.CloudflareInterceptor
import io.github.gmathi.novellibrary.network.interceptor.UserAgentInterceptor
import io.github.gmathi.novellibrary.util.DataCenter
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.util.concurrent.TimeUnit

class NetworkHelper(private val context: Context) {

    private val dataCenter: DataCenter by injectLazy()
    private val cacheDir = File(context.cacheDir, "network_cache")
    private val cacheSize = 5L * 1024 * 1024 // 5 MiB

    val cookieManager = AndroidCookieJar()

    private val baseClientBuilder: OkHttpClient.Builder
        get() {
            val builder = OkHttpClient.Builder()
                .cookieJar(cookieManager)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(UserAgentInterceptor())

            if (BuildConfig.DEBUG) {
                val httpLoggingInterceptor = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.HEADERS
                }
                builder.addInterceptor(httpLoggingInterceptor)
            }

            when (dataCenter.dohProvider) {
                PREF_DOH_CLOUDFLARE -> builder.dohCloudflare()
                PREF_DOH_GOOGLE -> builder.dohGoogle()
                // PREF_DOH_NONE -> do nothing
            }

            return builder
        }

    val client by lazy { baseClientBuilder.cache(Cache(cacheDir, cacheSize)).build() }

    val coilClient by lazy { baseClientBuilder.cache(CoilUtils.createDefaultCache(context)).build() }

    val cloudflareClient by lazy {
        client.newBuilder()
            .addInterceptor(CloudflareInterceptor(context))
            .build()
    }

    /**
     * returns - True - if there is connection to the internet
     */
    fun isConnectedToNetwork(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val netInfo = connectivityManager?.activeNetworkInfo
        return netInfo != null && netInfo.isConnected
    }

}

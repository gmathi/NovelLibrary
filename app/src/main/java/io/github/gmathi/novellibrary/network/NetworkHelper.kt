package io.github.gmathi.novellibrary.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import coil.ImageLoader
import coil.disk.DiskCache
import io.github.gmathi.novellibrary.BuildConfig
import io.github.gmathi.novellibrary.network.interceptor.CloudflareInterceptor
import io.github.gmathi.novellibrary.network.interceptor.UserAgentInterceptor
import io.github.gmathi.novellibrary.model.preference.DataCenter
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
    val cloudflareCookieManager = CloudflareCookieManager()

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

    val cloudflareClient by lazy {
        client.newBuilder()
            .addInterceptor(CloudflareInterceptor(context))
            .build()
    }

    /**
     * Coil ImageLoader singleton with disk cache and shared OkHttpClient.
     * Using the app's OkHttpClient ensures cookies, user-agent, and DoH are applied to image requests.
     */
    val coilImageLoader by lazy {
        ImageLoader.Builder(context)
            .okHttpClient(client)
            .diskCache {
                DiskCache.Builder()
                    .directory(File(context.cacheDir, "coil_image_cache"))
                    .maxSizeBytes(25L * 1024 * 1024) // 25 MiB
                    .build()
            }
            .crossfade(true)
            .build()
    }

    /**
     * returns - True - if there is connection to the internet
     */
    @Suppress("DEPRECATION")
    fun isConnectedToNetwork(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val netInfo = connectivityManager.activeNetworkInfo
            netInfo != null && netInfo.isConnected
        }
    }

}

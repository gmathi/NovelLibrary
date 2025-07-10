package io.github.gmathi.novellibrary.network

import android.content.Context
import android.net.ConnectivityManager
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.CoilUtils
import io.github.gmathi.novellibrary.BuildConfig
import io.github.gmathi.novellibrary.network.interceptor.CloudflareInterceptor
import io.github.gmathi.novellibrary.network.interceptor.UserAgentInterceptor
import io.github.gmathi.novellibrary.model.preference.DataCenter
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Optimized NetworkHelper with performance improvements:
 * - Connection pooling
 * - Request caching
 * - Optimized timeouts
 * - Memory-efficient image loading
 */
class NetworkHelperOptimized(private val context: Context) {

    private val dataCenter: DataCenter by injectLazy()
    private val cacheDir = File(context.cacheDir, "network_cache")
    private val cacheSize = 50L * 1024 * 1024 // 50 MiB for better caching
    
    val cookieManager = AndroidCookieJar()

    // Optimized connection pool
    private val connectionPool = ConnectionPool(
        maxIdleConnections = 30, // Increased from default 5
        keepAliveDuration = 5, // 5 minutes
        timeUnit = TimeUnit.MINUTES
    )

    private val baseClientBuilder: OkHttpClient.Builder
        get() {
            val builder = OkHttpClient.Builder()
                .cookieJar(cookieManager)
                .connectionPool(connectionPool)
                .connectTimeout(15, TimeUnit.SECONDS) // Reduced from 30s
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(UserAgentInterceptor())

            if (BuildConfig.DEBUG) {
                val httpLoggingInterceptor = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC // Reduced from HEADERS
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

    val client by lazy { 
        baseClientBuilder
            .cache(Cache(cacheDir, cacheSize))
            .build() 
    }

    val cloudflareClient by lazy {
        client.newBuilder()
            .addInterceptor(CloudflareInterceptor(context))
            .build()
    }
    
    // Optimized image loader with better caching
    val imageLoader by lazy {
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25) // 25% of available memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(context.cacheDir, "image_cache"))
                    .maxSizePercent(0.02) // 2% of available disk space
                    .build()
            }
            .okHttpClient {
                OkHttpClient.Builder()
                    .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
                    .cache(Cache(File(context.cacheDir, "image_http_cache"), 25L * 1024 * 1024))
                    .build()
            }
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
    
    /**
     * Check if we have a fast connection (WiFi or 4G+)
     */
    fun hasFastConnection(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val network = connectivityManager?.activeNetwork
        val capabilities = connectivityManager?.getNetworkCapabilities(network)
        
        return capabilities?.let {
            it.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
            (it.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) && 
             it.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED))
        } ?: false
    }
    
    /**
     * Get optimized client based on connection speed
     */
    fun getOptimizedClient(): OkHttpClient {
        return if (hasFastConnection()) {
            // Use more aggressive settings for fast connections
            client.newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
        } else {
            // Use conservative settings for slow connections
            client.newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .build()
        }
    }
}
package io.github.gmathi.novellibrary.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import coil.ImageLoader
import coil.disk.DiskCache
import coil.util.CoilUtils
import io.github.gmathi.novellibrary.BuildConfig
import io.github.gmathi.novellibrary.network.interceptor.CloudflareInterceptor
import io.github.gmathi.novellibrary.network.interceptor.UserAgentInterceptor
import io.github.gmathi.novellibrary.model.preference.DataCenter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataCenter: DataCenter,
    private val timeoutConfig: NetworkTimeoutConfig
) {
    private val cacheDir = File(context.cacheDir, "network_cache")
    private val cacheSize = 5L * 1024 * 1024 // 5 MiB

    val cookieManager = AndroidCookieJar()

    private val baseClientBuilder: OkHttpClient.Builder
        get() {
            val builder = OkHttpClient.Builder()
                .cookieJar(cookieManager)
                .connectTimeout(timeoutConfig.getConnectTimeout(), TimeUnit.SECONDS)
                .readTimeout(timeoutConfig.getReadTimeout(), TimeUnit.SECONDS)
                .writeTimeout(timeoutConfig.getWriteTimeout(), TimeUnit.SECONDS)
                .callTimeout(timeoutConfig.getCallTimeout(), TimeUnit.SECONDS)
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
     * Retrofit instance configured for coroutines support
     */
    val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/") // Default base URL, can be overridden per service
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Retrofit instance with Cloudflare client for sites that require it
     */
    val cloudflareRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/") // Default base URL, can be overridden per service
            .client(cloudflareClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * returns - True - if there is connection to the internet
     */
    fun isConnectedToNetwork(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager?.activeNetwork
            val capabilities = connectivityManager?.getNetworkCapabilities(network)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val netInfo = connectivityManager?.activeNetworkInfo
            netInfo != null && netInfo.isConnected
        }
    }

    /**
     * Flow-based network connectivity monitoring to replace ReactiveNetwork
     * Emits true when connected to internet, false when disconnected
     */
    fun networkConnectivityFlow(): Flow<Boolean> {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            ?: return flowOf(false)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            callbackFlow {
                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        trySend(true)
                    }

                    override fun onLost(network: Network) {
                        trySend(false)
                    }

                    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                        val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                        trySend(hasInternet)
                    }
                }

                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()

                connectivityManager.registerNetworkCallback(request, callback)

                // Send initial state
                trySend(isConnectedToNetwork())

                awaitClose {
                    connectivityManager.unregisterNetworkCallback(callback)
                }
            }.distinctUntilChanged()
        } else {
            // For older Android versions, fall back to polling
            flowOf(isConnectedToNetwork())
        }
    }

    /**
     * Suspend function to check network connectivity
     * Useful for one-time checks in coroutines
     */
    suspend fun checkNetworkConnectivity(): Boolean {
        return isConnectedToNetwork()
    }

}

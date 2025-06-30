package io.github.gmathi.novellibrary.network

import android.content.Context
import android.net.ConnectivityManager
import coil.ImageLoader
import coil.disk.DiskCache
import coil.util.CoilUtils
import io.github.gmathi.novellibrary.BuildConfig
import io.github.gmathi.novellibrary.network.interceptor.CloudflareInterceptor
import io.github.gmathi.novellibrary.network.interceptor.DeduplicationInterceptor
import io.github.gmathi.novellibrary.network.interceptor.UserAgentInterceptor
import io.github.gmathi.novellibrary.model.preference.DataCenter
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.util.concurrent.TimeUnit
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong
import io.github.gmathi.novellibrary.network.interceptor.RateLimitInterceptor

// Add Priority enum
enum class RequestPriority(val value: Int) {
    HIGH(3),
    NORMAL(2),
    LOW(1)
}

// PriorityTask for queueing
private class PriorityFutureTask<T>(
    val priority: RequestPriority,
    callable: Callable<T>
) : FutureTask<T>(callable), Comparable<PriorityFutureTask<*>> {
    override fun compareTo(other: PriorityFutureTask<*>): Int = other.priority.value - this.priority.value
}

// PriorityExecutorService
class PriorityExecutorService(
    corePoolSize: Int,
    maxPoolSize: Int,
    keepAliveTime: Long,
    unit: TimeUnit
) : ThreadPoolExecutor(
    corePoolSize,
    maxPoolSize,
    keepAliveTime,
    unit,
    PriorityBlockingQueue<Runnable>()
) {
    override fun <T> newTaskFor(callable: Callable<T>): RunnableFuture<T> {
        return PriorityFutureTask(RequestPriority.NORMAL, callable)
    }
    fun <T> submit(task: Callable<T>, priority: RequestPriority): Future<T> {
        val priorityTask = PriorityFutureTask(priority, task)
        execute(priorityTask)
        return priorityTask
    }
}

class NetworkHelper(private val context: Context) {

    // Add a custom PriorityExecutorService for OkHttp Dispatcher
    private val priorityExecutor = PriorityExecutorService(4, 8, 60L, TimeUnit.SECONDS)
    private val dispatcher = Dispatcher(priorityExecutor)

    private val dataCenter: DataCenter by injectLazy()
    private val cacheDir = File(context.cacheDir, "network_cache")
    private val cacheSize = 50L * 1024 * 1024 // 50 MiB (increased from 5 MiB)

    val cookieManager = AndroidCookieJar()

    var rateLimitPerSecond: Int = 5

    private val baseClientBuilder: OkHttpClient.Builder
        get() {
            val builder = OkHttpClient.Builder()
                .addInterceptor(RateLimitInterceptor(rateLimitPerSecond))
                .cookieJar(cookieManager)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(UserAgentInterceptor())
                .addInterceptor(DeduplicationInterceptor()) // Enable request deduplication
                .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES)) // Optimize connection pool
                .dispatcher(dispatcher) // Use custom dispatcher

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
     * Enqueue a request with a given priority
     */
    fun newCallWithPriority(request: okhttp3.Request, priority: RequestPriority = RequestPriority.NORMAL): okhttp3.Call {
        val prioritizedRequest = request.newBuilder().tag(RequestPriority::class.java, priority).build()
        return client.newCall(prioritizedRequest)
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

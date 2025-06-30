import io.github.gmathi.novellibrary.network.interceptor.RateLimitInterceptor
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class RateLimitInterceptorTest {
    @Test
    fun testRateLimiting() {
        val rateLimit = 5 // 5 requests per second
        val interceptor = RateLimitInterceptor(rateLimit)
        val request = Request.Builder().url("http://test.com").build()
        val callCount = 10
        val latch = CountDownLatch(callCount)
        val times = mutableListOf<Long>()
        val chain = object : Interceptor.Chain {
            override fun request(): Request = request
            override fun proceed(request: Request): Response {
                times.add(System.nanoTime())
                latch.countDown()
                return Response.Builder()
                    .request(request)
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .build()
            }
            override fun connection() = null
            override fun call() = throw NotImplementedError()
            override fun connectTimeoutMillis() = 0
            override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
            override fun readTimeoutMillis() = 0
            override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
            override fun writeTimeoutMillis() = 0
            override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        }
        val threads = List(callCount) {
            Thread {
                interceptor.intercept(chain)
            }
        }
        val start = System.nanoTime()
        threads.forEach { it.start() }
        latch.await(3, TimeUnit.SECONDS)
        val elapsed = System.nanoTime() - start
        // 10 requests at 5 rps should take at least 2 seconds, allow margin for thread scheduling
        assertTrue(elapsed >= 1_500_000_000L)
    }
} 
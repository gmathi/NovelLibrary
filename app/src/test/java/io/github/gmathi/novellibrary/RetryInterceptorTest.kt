import io.github.gmathi.novellibrary.network.interceptor.RetryInterceptor
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class RetryInterceptorTest {
    @Test
    fun testRetriesOnIOException() {
        val interceptor = RetryInterceptor(maxRetries = 2, baseDelayMillis = 1)
        val attempts = AtomicInteger(0)
        val chain = object : Interceptor.Chain {
            override fun request(): Request = Request.Builder().url("http://test.com").get().build()
            override fun proceed(request: Request): Response {
                attempts.incrementAndGet()
                throw IOException("Simulated network error")
            }
            override fun connection() = null
            override fun call() = throw NotImplementedError()
            override fun connectTimeoutMillis() = 0
            override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun readTimeoutMillis() = 0
            override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun writeTimeoutMillis() = 0
            override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        }
        assertThrows(IOException::class.java) {
            interceptor.intercept(chain)
        }
        assertEquals(3, attempts.get()) // 1 initial + 2 retries
    }

    @Test
    fun testNoRetryOn4xx() {
        val interceptor = RetryInterceptor(maxRetries = 2, baseDelayMillis = 1)
        val attempts = AtomicInteger(0)
        val chain = object : Interceptor.Chain {
            override fun request(): Request = Request.Builder().url("http://test.com").get().build()
            override fun proceed(request: Request): Response {
                attempts.incrementAndGet()
                return Response.Builder()
                    .request(request)
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(404)
                    .message("Not Found")
                    .build()
            }
            override fun connection() = null
            override fun call() = throw NotImplementedError()
            override fun connectTimeoutMillis() = 0
            override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun readTimeoutMillis() = 0
            override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun writeTimeoutMillis() = 0
            override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        }
        val response = interceptor.intercept(chain)
        assertEquals(1, attempts.get())
        assertEquals(404, response.code)
    }

    @Test
    fun testNoRetryOnPost() {
        val interceptor = RetryInterceptor(maxRetries = 2, baseDelayMillis = 1)
        val attempts = AtomicInteger(0)
        val chain = object : Interceptor.Chain {
            override fun request(): Request = Request.Builder().url("http://test.com").post(okhttp3.RequestBody.create(null, ByteArray(0))).build()
            override fun proceed(request: Request): Response {
                attempts.incrementAndGet()
                throw IOException("Simulated network error")
            }
            override fun connection() = null
            override fun call() = throw NotImplementedError()
            override fun connectTimeoutMillis() = 0
            override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun readTimeoutMillis() = 0
            override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun writeTimeoutMillis() = 0
            override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        }
        assertThrows(IOException::class.java) {
            interceptor.intercept(chain)
        }
        assertEquals(1, attempts.get()) // No retry for POST
    }
} 
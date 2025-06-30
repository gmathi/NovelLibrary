import io.github.gmathi.novellibrary.network.interceptor.CacheControlInterceptor
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CacheControlInterceptorTest {
    @Test
    fun test200ResponseIsCacheable() {
        val interceptor = CacheControlInterceptor()
        val chain = object : Interceptor.Chain {
            override fun request(): Request = Request.Builder().url("http://test.com").build()
            override fun proceed(request: Request): Response {
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
            override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun readTimeoutMillis() = 0
            override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun writeTimeoutMillis() = 0
            override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        }
        val response = interceptor.intercept(chain)
        assertEquals("MISS", response.header("X-Cache"))
        assertNull(response.header("Cache-Control"))
    }

    @Test
    fun test404ResponseIsNotCacheable() {
        val interceptor = CacheControlInterceptor()
        val chain = object : Interceptor.Chain {
            override fun request(): Request = Request.Builder().url("http://test.com").build()
            override fun proceed(request: Request): Response {
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
        assertEquals("NO-CACHE", response.header("X-Cache"))
        assertEquals("no-store, no-cache, must-revalidate, max-age=0", response.header("Cache-Control"))
        assertEquals("no-cache", response.header("Pragma"))
    }

    @Test
    fun test500ResponseIsNotCacheable() {
        val interceptor = CacheControlInterceptor()
        val chain = object : Interceptor.Chain {
            override fun request(): Request = Request.Builder().url("http://test.com").build()
            override fun proceed(request: Request): Response {
                return Response.Builder()
                    .request(request)
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(500)
                    .message("Internal Server Error")
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
        assertEquals("NO-CACHE", response.header("X-Cache"))
        assertEquals("no-store, no-cache, must-revalidate, max-age=0", response.header("Cache-Control"))
        assertEquals("no-cache", response.header("Pragma"))
    }

    @Test
    fun test301RedirectIsNotCacheable() {
        val interceptor = CacheControlInterceptor()
        val chain = object : Interceptor.Chain {
            override fun request(): Request = Request.Builder().url("http://test.com").build()
            override fun proceed(request: Request): Response {
                return Response.Builder()
                    .request(request)
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(301)
                    .message("Moved Permanently")
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
        assertEquals("NO-CACHE", response.header("X-Cache"))
        assertEquals("no-store, no-cache, must-revalidate, max-age=0", response.header("Cache-Control"))
        assertEquals("no-cache", response.header("Pragma"))
    }

    @Test
    fun test302RedirectIsNotCacheable() {
        val interceptor = CacheControlInterceptor()
        val chain = object : Interceptor.Chain {
            override fun request(): Request = Request.Builder().url("http://test.com").build()
            override fun proceed(request: Request): Response {
                return Response.Builder()
                    .request(request)
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(302)
                    .message("Found")
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
        assertEquals("NO-CACHE", response.header("X-Cache"))
        assertEquals("no-store, no-cache, must-revalidate, max-age=0", response.header("Cache-Control"))
        assertEquals("no-cache", response.header("Pragma"))
    }

    @Test
    fun test403ForbiddenIsNotCacheable() {
        val interceptor = CacheControlInterceptor()
        val chain = object : Interceptor.Chain {
            override fun request(): Request = Request.Builder().url("http://test.com").build()
            override fun proceed(request: Request): Response {
                return Response.Builder()
                    .request(request)
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(403)
                    .message("Forbidden")
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
        assertEquals("NO-CACHE", response.header("X-Cache"))
        assertEquals("no-store, no-cache, must-revalidate, max-age=0", response.header("Cache-Control"))
        assertEquals("no-cache", response.header("Pragma"))
    }

    @Test
    fun test503ServiceUnavailableIsNotCacheable() {
        val interceptor = CacheControlInterceptor()
        val chain = object : Interceptor.Chain {
            override fun request(): Request = Request.Builder().url("http://test.com").build()
            override fun proceed(request: Request): Response {
                return Response.Builder()
                    .request(request)
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(503)
                    .message("Service Unavailable")
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
        assertEquals("NO-CACHE", response.header("X-Cache"))
        assertEquals("no-store, no-cache, must-revalidate, max-age=0", response.header("Cache-Control"))
        assertEquals("no-cache", response.header("Pragma"))
    }
} 
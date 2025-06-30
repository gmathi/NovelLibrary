package io.github.gmathi.novellibrary

import io.github.gmathi.novellibrary.network.interceptor.DeduplicationInterceptor
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DeduplicationInterceptorTest {
    @Test
    fun testConcurrentIdenticalRequestsAreDeduplicated() {
        val interceptor = DeduplicationInterceptor()
        val callCount = arrayOf(0)
        val testInterceptor = Interceptor { chain ->
            synchronized(callCount) { callCount[0]++ }
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build()
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor(testInterceptor)
            .build()

        val request = Request.Builder().url("http://test.com/").build()
        val latch = CountDownLatch(10)
        val executor = Executors.newFixedThreadPool(10)
        repeat(10) {
            executor.submit {
                client.newCall(request).execute().use { response ->
                    assertEquals(200, response.code)
                }
                latch.countDown()
            }
        }
        latch.await(5, TimeUnit.SECONDS)
        assertEquals(1, callCount[0])
    }

    @Test
    fun testErrorPropagationToAllWaiters() {
        val interceptor = DeduplicationInterceptor()
        val testInterceptor = Interceptor { chain ->
            throw RuntimeException("Simulated failure")
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor(testInterceptor)
            .build()

        val request = Request.Builder().url("http://test.com/").build()
        val latch = CountDownLatch(5)
        val executor = Executors.newFixedThreadPool(5)
        val errors = mutableListOf<Throwable>()
        repeat(5) {
            executor.submit {
                try {
                    client.newCall(request).execute()
                } catch (e: Exception) {
                    synchronized(errors) { errors.add(e) }
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await(5, TimeUnit.SECONDS)
        assertEquals(5, errors.size)
        errors.forEach { assertEquals("Simulated failure", it.cause?.message ?: it.message) }
    }
} 
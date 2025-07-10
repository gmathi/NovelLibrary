package io.github.gmathi.novellibrary.util.lang

import org.junit.Test
import org.junit.Assert.*
import rx.Observable
import rx.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class RetryWithDelayTest {

    @Test
    fun `test RetryWithDelay with successful operation`() {
        var attemptCount = 0
        val retryWithDelay = RetryWithDelay(maxRetries = 2)
        
        val observable = Observable.fromCallable {
            attemptCount++
            if (attemptCount == 1) {
                throw RuntimeException("First attempt fails")
            }
            "Success"
        }.retryWhen(retryWithDelay)
        
        val result = observable.toBlocking().first()
        assertEquals("Success", result)
        assertEquals(2, attemptCount)
    }

    @Test
    fun `test RetryWithDelay with all attempts failing`() {
        var attemptCount = 0
        val retryWithDelay = RetryWithDelay(maxRetries = 2)
        
        val observable = Observable.fromCallable {
            attemptCount++
            throw RuntimeException("Always fails")
        }.retryWhen(retryWithDelay)
        
        try {
            observable.toBlocking().first()
            fail("Should have thrown exception")
        } catch (e: RuntimeException) {
            assertEquals("Always fails", e.message)
            assertEquals(3, attemptCount) // Initial + 2 retries
        }
    }

    @Test
    fun `test RetryWithDelay with zero max retries`() {
        var attemptCount = 0
        val retryWithDelay = RetryWithDelay(maxRetries = 0)
        
        val observable = Observable.fromCallable {
            attemptCount++
            throw RuntimeException("Fails")
        }.retryWhen(retryWithDelay)
        
        try {
            observable.toBlocking().first()
            fail("Should have thrown exception")
        } catch (e: RuntimeException) {
            assertEquals("Fails", e.message)
            assertEquals(1, attemptCount) // Only initial attempt
        }
    }

    @Test
    fun `test RetryWithDelay with custom retry strategy`() {
        var attemptCount = 0
        val retryWithDelay = RetryWithDelay(
            maxRetries = 2,
            retryStrategy = { attempt -> attempt * 100 } // 100ms, 200ms delays
        )
        
        val startTime = System.currentTimeMillis()
        
        val observable = Observable.fromCallable {
            attemptCount++
            if (attemptCount < 3) {
                throw RuntimeException("Attempt $attemptCount fails")
            }
            "Success"
        }.retryWhen(retryWithDelay)
        
        val result = observable.toBlocking().first()
        val endTime = System.currentTimeMillis()
        
        assertEquals("Success", result)
        assertEquals(3, attemptCount)
        // Should have waited at least 300ms (100 + 200)
        assertTrue(endTime - startTime >= 300)
    }

    @Test
    fun `test RetryWithDelay with custom scheduler`() {
        var attemptCount = 0
        val retryWithDelay = RetryWithDelay(
            maxRetries = 1,
            scheduler = Schedulers.immediate()
        )
        
        val observable = Observable.fromCallable {
            attemptCount++
            if (attemptCount == 1) {
                throw RuntimeException("First attempt fails")
            }
            "Success"
        }.retryWhen(retryWithDelay)
        
        val result = observable.toBlocking().first()
        assertEquals("Success", result)
        assertEquals(2, attemptCount)
    }

    @Test
    fun `test RetryWithDelay with immediate success`() {
        var attemptCount = 0
        val retryWithDelay = RetryWithDelay(maxRetries = 2)
        
        val observable = Observable.fromCallable {
            attemptCount++
            "Success immediately"
        }.retryWhen(retryWithDelay)
        
        val result = observable.toBlocking().first()
        assertEquals("Success immediately", result)
        assertEquals(1, attemptCount) // Only one attempt needed
    }

    @Test
    fun `test RetryWithDelay with negative max retries`() {
        var attemptCount = 0
        val retryWithDelay = RetryWithDelay(maxRetries = -1)
        
        val observable = Observable.fromCallable {
            attemptCount++
            throw RuntimeException("Fails")
        }.retryWhen(retryWithDelay)
        
        try {
            observable.toBlocking().first()
            fail("Should have thrown exception")
        } catch (e: RuntimeException) {
            assertEquals("Fails", e.message)
            assertEquals(1, attemptCount) // Only initial attempt
        }
    }

    @Test
    fun `test RetryWithDelay with very large max retries`() {
        var attemptCount = 0
        val retryWithDelay = RetryWithDelay(maxRetries = 1000)
        
        val observable = Observable.fromCallable {
            attemptCount++
            if (attemptCount <= 5) {
                throw RuntimeException("Attempt $attemptCount fails")
            }
            "Success after 5 attempts"
        }.retryWhen(retryWithDelay)
        
        val result = observable.toBlocking().first()
        assertEquals("Success after 5 attempts", result)
        assertEquals(6, attemptCount) // 5 failures + 1 success
    }

    @Test
    fun `test RetryWithDelay with different exception types`() {
        var attemptCount = 0
        val retryWithDelay = RetryWithDelay(maxRetries = 2)
        
        val observable = Observable.fromCallable {
            attemptCount++
            when (attemptCount) {
                1 -> throw IllegalArgumentException("Illegal argument")
                2 -> throw NullPointerException("Null pointer")
                else -> "Success"
            }
        }.retryWhen(retryWithDelay)
        
        val result = observable.toBlocking().first()
        assertEquals("Success", result)
        assertEquals(3, attemptCount)
    }

    @Test
    fun `test RetryWithDelay with custom retry strategy returning zero delay`() {
        var attemptCount = 0
        val retryWithDelay = RetryWithDelay(
            maxRetries = 2,
            retryStrategy = { 0 } // No delay
        )
        
        val startTime = System.currentTimeMillis()
        
        val observable = Observable.fromCallable {
            attemptCount++
            if (attemptCount < 3) {
                throw RuntimeException("Attempt $attemptCount fails")
            }
            "Success"
        }.retryWhen(retryWithDelay)
        
        val result = observable.toBlocking().first()
        val endTime = System.currentTimeMillis()
        
        assertEquals("Success", result)
        assertEquals(3, attemptCount)
        // Should complete quickly since no delays
        assertTrue(endTime - startTime < 100)
    }

    @Test
    fun `test RetryWithDelay with custom retry strategy returning negative delay`() {
        var attemptCount = 0
        val retryWithDelay = RetryWithDelay(
            maxRetries = 2,
            retryStrategy = { -100 } // Negative delay
        )
        
        val observable = Observable.fromCallable {
            attemptCount++
            if (attemptCount < 3) {
                throw RuntimeException("Attempt $attemptCount fails")
            }
            "Success"
        }.retryWhen(retryWithDelay)
        
        val result = observable.toBlocking().first()
        assertEquals("Success", result)
        assertEquals(3, attemptCount)
    }
} 
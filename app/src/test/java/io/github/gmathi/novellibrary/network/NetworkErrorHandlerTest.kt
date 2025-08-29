package io.github.gmathi.novellibrary.network

import android.content.Context
import io.github.gmathi.novellibrary.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class NetworkErrorHandlerTest {

    private lateinit var context: Context
    private lateinit var errorHandler: NetworkErrorHandler

    @Before
    fun setup() {
        context = mockk()
        errorHandler = NetworkErrorHandler(context)
        
        // Mock string resources
        every { context.getString(R.string.error_network_timeout) } returns "Request timed out"
        every { context.getString(R.string.error_network_interrupted) } returns "Network interrupted"
        every { context.getString(R.string.error_network_connection_failed) } returns "Connection failed"
        every { context.getString(R.string.error_network_no_internet) } returns "No internet"
        every { context.getString(R.string.error_network_unknown) } returns "Unknown error"
    }

    @Test
    fun `getErrorMessage returns timeout message for InterruptedIOException with timeout`() {
        // Given
        val exception = InterruptedIOException("timeout")

        // When
        val message = errorHandler.getErrorMessage(exception)

        // Then
        assertEquals("Request timed out", message)
    }

    @Test
    fun `getErrorMessage returns interrupted message for InterruptedIOException without timeout`() {
        // Given
        val exception = InterruptedIOException("other reason")

        // When
        val message = errorHandler.getErrorMessage(exception)

        // Then
        assertEquals("Network interrupted", message)
    }

    @Test
    fun `getErrorMessage returns timeout message for SocketTimeoutException`() {
        // Given
        val exception = SocketTimeoutException("Read timed out")

        // When
        val message = errorHandler.getErrorMessage(exception)

        // Then
        assertEquals("Request timed out", message)
    }

    @Test
    fun `getErrorMessage returns connection failed for ConnectException`() {
        // Given
        val exception = ConnectException("Connection refused")

        // When
        val message = errorHandler.getErrorMessage(exception)

        // Then
        assertEquals("Connection failed", message)
    }

    @Test
    fun `getErrorMessage returns no internet for UnknownHostException`() {
        // Given
        val exception = UnknownHostException("Unable to resolve host")

        // When
        val message = errorHandler.getErrorMessage(exception)

        // Then
        assertEquals("No internet", message)
    }

    @Test
    fun `isRetryableError returns true for timeout exceptions`() {
        // Given
        val timeoutException = SocketTimeoutException()
        val interruptedException = InterruptedIOException("timeout")

        // When & Then
        assertTrue(errorHandler.isRetryableError(timeoutException))
        assertTrue(errorHandler.isRetryableError(interruptedException))
    }

    @Test
    fun `isRetryableError returns false for canceled operations`() {
        // Given
        val canceledException = InterruptedIOException("Canceled")

        // When & Then
        assertFalse(errorHandler.isRetryableError(canceledException))
    }

    @Test
    fun `isRetryableError returns false for UnknownHostException`() {
        // Given
        val hostException = UnknownHostException()

        // When & Then
        assertFalse(errorHandler.isRetryableError(hostException))
    }

    @Test
    fun `getRetryDelay returns increasing delays for multiple attempts`() {
        // Given
        val exception = SocketTimeoutException()

        // When
        val delay1 = errorHandler.getRetryDelay(exception, 0)
        val delay2 = errorHandler.getRetryDelay(exception, 1)
        val delay3 = errorHandler.getRetryDelay(exception, 2)

        // Then
        assertTrue("Second delay should be greater than first", delay2 > delay1)
        assertTrue("Third delay should be greater than second", delay3 > delay2)
    }
}
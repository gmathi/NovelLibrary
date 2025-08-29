package io.github.gmathi.novellibrary.network

import android.content.Context
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NetworkTimeoutTest {

    private lateinit var dataCenter: DataCenter
    private lateinit var timeoutConfig: NetworkTimeoutConfig

    @Before
    fun setup() {
        dataCenter = mockk()
        timeoutConfig = NetworkTimeoutConfig(dataCenter)
    }

    @Test
    fun `getConnectTimeout returns default when dataCenter returns 0`() {
        // Given
        every { dataCenter.networkConnectTimeout } returns 0L

        // When
        val timeout = timeoutConfig.getConnectTimeout()

        // Then
        assertEquals(NetworkTimeoutConfig.DEFAULT_CONNECT_TIMEOUT, timeout)
    }

    @Test
    fun `getConnectTimeout returns custom value when set`() {
        // Given
        val customTimeout = 60L
        every { dataCenter.networkConnectTimeout } returns customTimeout

        // When
        val timeout = timeoutConfig.getConnectTimeout()

        // Then
        assertEquals(customTimeout, timeout)
    }

    @Test
    fun `getMaxRetries returns default when dataCenter returns 0`() {
        // Given
        every { dataCenter.networkMaxRetries } returns 0

        // When
        val retries = timeoutConfig.getMaxRetries()

        // Then
        assertEquals(NetworkTimeoutConfig.DEFAULT_MAX_RETRIES, retries)
    }

    @Test
    fun `getMaxRetries returns custom value when set`() {
        // Given
        val customRetries = 5
        every { dataCenter.networkMaxRetries } returns customRetries

        // When
        val retries = timeoutConfig.getMaxRetries()

        // Then
        assertEquals(customRetries, retries)
    }

    @Test
    fun `getBaseRetryDelay returns default when dataCenter returns 0`() {
        // Given
        every { dataCenter.networkRetryDelay } returns 0L

        // When
        val delay = timeoutConfig.getBaseRetryDelay()

        // Then
        assertEquals(NetworkTimeoutConfig.DEFAULT_BASE_DELAY_MS, delay)
    }
}
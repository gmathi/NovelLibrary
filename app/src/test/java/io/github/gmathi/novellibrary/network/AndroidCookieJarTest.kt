package io.github.gmathi.novellibrary.network

import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.mockk.mockk
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidCookieJarTest {

    private lateinit var dataCenter: DataCenter
    private lateinit var androidCookieJar: AndroidCookieJar

    @Before
    fun setUp() {
        dataCenter = mockk(relaxed = true)
        androidCookieJar = AndroidCookieJar(dataCenter)
    }

    @Test
    fun `constructor injection works correctly`() {
        // Given
        val cookieJar = AndroidCookieJar(dataCenter)
        
        // Then
        assertNotNull(cookieJar)
    }

    @Test
    fun `loadForRequest returns empty list for unknown URL`() {
        // Given
        val url = "https://example.com".toHttpUrl()
        
        // When
        val cookies = androidCookieJar.loadForRequest(url)
        
        // Then
        assertTrue(cookies.isEmpty())
    }

    @Test
    fun `get returns empty list for unknown URL`() {
        // Given
        val url = "https://example.com".toHttpUrl()
        
        // When
        val cookies = androidCookieJar.get(url)
        
        // Then
        assertTrue(cookies.isEmpty())
    }
}
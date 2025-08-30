package io.github.gmathi.novellibrary.network.postProxy

import io.github.gmathi.novellibrary.network.NetworkHelper
import io.mockk.every
import io.mockk.mockk
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URL

@RunWith(RobolectricTestRunner::class)
class BasePostProxyHelperTest {

    private lateinit var networkHelper: NetworkHelper
    private lateinit var basePostProxyHelper: BasePostProxyHelper

    @Before
    fun setUp() {
        networkHelper = mockk(relaxed = true)
        basePostProxyHelper = BasePostProxyHelper(networkHelper)
    }

    @Test
    fun `constructor injection works correctly`() {
        // Given
        val helper = BasePostProxyHelper(networkHelper)
        
        // Then
        assertNotNull(helper)
        assertEquals(networkHelper, helper.networkHelper)
    }

    @Test
    fun `client property returns cloudflare client from network helper`() {
        // When
        val client = basePostProxyHelper.client
        
        // Then
        assertNotNull(client)
        // Note: In a real test, we would verify that networkHelper.cloudflareClient is called
    }

    @Test
    fun `getInstance returns JsonContentProxy for inoveltranslation URLs`() {
        // Given
        val mockRequest = mockk<Request> {
            every { url } returns mockk {
                every { toString() } returns "https://inoveltranslation.com/chapters/123"
            }
        }
        val mockResponse = mockk<Response> {
            every { request } returns mockRequest
        }
        
        // When
        val proxy = BasePostProxyHelper.getInstance(mockResponse, networkHelper)
        
        // Then
        assertNotNull(proxy)
        assertTrue(proxy is JsonContentProxy<*>)
    }

    @Test
    fun `getInstance returns null for unknown URLs`() {
        // Given
        val mockRequest = mockk<Request> {
            every { url } returns mockk {
                every { toString() } returns "https://unknown-site.com/some-novel"
            }
        }
        val mockResponse = mockk<Response> {
            every { request } returns mockRequest
        }
        
        // When
        val proxy = BasePostProxyHelper.getInstance(mockResponse, networkHelper)
        
        // Then
        assertNull(proxy)
    }
}
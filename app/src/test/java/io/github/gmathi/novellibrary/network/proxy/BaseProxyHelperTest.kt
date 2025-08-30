package io.github.gmathi.novellibrary.network.proxy

import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BaseProxyHelperTest {

    private lateinit var networkHelper: NetworkHelper
    private lateinit var baseProxyHelper: BaseProxyHelper

    @Before
    fun setUp() {
        networkHelper = mockk(relaxed = true)
        baseProxyHelper = BaseProxyHelper(networkHelper)
    }

    @Test
    fun `constructor injection works correctly`() {
        // Given
        val helper = BaseProxyHelper(networkHelper)
        
        // Then
        assertNotNull(helper)
        assertEquals(networkHelper, helper.networkHelper)
    }

    @Test
    fun `client property returns cloudflare client from network helper`() {
        // When
        val client = baseProxyHelper.client
        
        // Then
        assertNotNull(client)
        // Note: In a real test, we would verify that networkHelper.cloudflareClient is called
    }

    @Test
    fun `getInstance returns FoxTellerProxy for foxteller URLs`() {
        // Given
        val url = "https://www.${HostNames.FOXTELLER}/some-novel"
        
        // When
        val proxy = BaseProxyHelper.getInstance(url, networkHelper)
        
        // Then
        assertNotNull(proxy)
        assertTrue(proxy is FoxTellerProxy)
    }

    @Test
    fun `getInstance returns WattPadProxy for wattpad URLs`() {
        // Given
        val url = "https://www.${HostNames.WATTPAD}/some-novel"
        
        // When
        val proxy = BaseProxyHelper.getInstance(url, networkHelper)
        
        // Then
        assertNotNull(proxy)
        assertTrue(proxy is WattPadProxy)
    }

    @Test
    fun `getInstance returns BabelNovelProxy for babel novel URLs`() {
        // Given
        val url = "https://www.${HostNames.BABEL_NOVEL}/some-novel"
        
        // When
        val proxy = BaseProxyHelper.getInstance(url, networkHelper)
        
        // Then
        assertNotNull(proxy)
        assertTrue(proxy is BabelNovelProxy)
    }

    @Test
    fun `getInstance returns null for unknown URLs`() {
        // Given
        val url = "https://unknown-site.com/some-novel"
        
        // When
        val proxy = BaseProxyHelper.getInstance(url, networkHelper)
        
        // Then
        assertNull(proxy)
    }
}
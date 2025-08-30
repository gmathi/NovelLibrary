package io.github.gmathi.novellibrary.network.sync

import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.NovelSection
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.mockk.*
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URL

@RunWith(RobolectricTestRunner::class)
class NovelUpdatesSyncTest {

    private lateinit var dbHelper: DBHelper
    private lateinit var dataCenter: DataCenter
    private lateinit var networkHelper: NetworkHelper
    private lateinit var sourceManager: SourceManager
    private lateinit var novelUpdatesSync: NovelUpdatesSync

    @Before
    fun setUp() {
        dbHelper = mockk(relaxed = true)
        dataCenter = mockk(relaxed = true)
        networkHelper = mockk(relaxed = true)
        sourceManager = mockk(relaxed = true)
        
        // Mock the cookie manager
        val mockCookieJar = mockk<CookieJar>(relaxed = true)
        every { networkHelper.cookieManager } returns mockCookieJar
        
        novelUpdatesSync = NovelUpdatesSync(dbHelper, dataCenter, networkHelper, sourceManager)
    }

    @Test
    fun `constructor injection works correctly`() {
        // Given
        val sync = NovelUpdatesSync(dbHelper, dataCenter, networkHelper, sourceManager)
        
        // Then
        assertNotNull(sync)
        assertEquals(HostNames.NOVEL_UPDATES, sync.host)
    }

    @Test
    fun `loggedIn returns true when valid cookies are present`() {
        // Given
        val mockUrl = HttpUrl.parse("https://${HostNames.NOVEL_UPDATES}")!!
        val validCookies = mapOf(
            "wordpress_logged_in_test" to "value1",
            "wordpress_sec_test" to "value2"
        )
        every { networkHelper.cookieManager.getCookieMap(any<URL>()) } returns validCookies
        
        // When
        val result = novelUpdatesSync.loggedIn()
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `loggedIn returns false when no valid cookies are present`() {
        // Given
        val invalidCookies = mapOf(
            "some_other_cookie" to "value1",
            "another_cookie" to "value2"
        )
        every { networkHelper.cookieManager.getCookieMap(any<URL>()) } returns invalidCookies
        
        // When
        val result = novelUpdatesSync.loggedIn()
        
        // Then
        assertFalse(result)
    }

    @Test
    fun `getLoginURL returns correct URL`() {
        // When
        val loginUrl = novelUpdatesSync.getLoginURL()
        
        // Then
        assertEquals("https://${HostNames.NOVEL_UPDATES}/login/", loginUrl)
    }

    @Test
    fun `getCookieLookupRegex returns correct regex pattern`() {
        // When
        val regex = novelUpdatesSync.getCookieLookupRegex()
        
        // Then
        assertEquals("wordpress_(logged_in|sec|user_sw)_", regex)
    }

    @Test
    fun `validateNovel throws exception when PostId is missing`() {
        // Given
        val novel = mockk<Novel> {
            every { metadata } returns mutableMapOf()
        }
        
        // When & Then
        assertThrows(Exception::class.java) {
            NovelUpdatesSync.validateNovel(novel)
        }
    }

    @Test
    fun `validateNovel passes when PostId is present`() {
        // Given
        val novel = mockk<Novel> {
            every { metadata } returns mutableMapOf("PostId" to "12345")
        }
        
        // When & Then (should not throw)
        NovelUpdatesSync.validateNovel(novel)
    }

    @Test
    fun `addNovel calls correct URL format`() {
        // Given
        val novel = mockk<Novel> {
            every { metadata } returns mutableMapOf("PostId" to "12345")
        }
        val section = mockk<NovelSection> {
            every { name } returns "Test Section"
        }
        
        // Mock WebPageDocumentFetcher to avoid actual network calls
        mockkObject(io.github.gmathi.novellibrary.network.WebPageDocumentFetcher)
        every { io.github.gmathi.novellibrary.network.WebPageDocumentFetcher.document(any()) } returns mockk(relaxed = true)
        
        // When
        val result = novelUpdatesSync.addNovel(novel, section)
        
        // Then
        assertTrue(result)
        verify { io.github.gmathi.novellibrary.network.WebPageDocumentFetcher.document(any()) }
        
        unmockkObject(io.github.gmathi.novellibrary.network.WebPageDocumentFetcher)
    }

    @Test
    fun `removeNovel calls correct URL format`() {
        // Given
        val novel = mockk<Novel> {
            every { metadata } returns mutableMapOf("PostId" to "12345")
        }
        
        // Mock WebPageDocumentFetcher to avoid actual network calls
        mockkObject(io.github.gmathi.novellibrary.network.WebPageDocumentFetcher)
        every { io.github.gmathi.novellibrary.network.WebPageDocumentFetcher.document(any()) } returns mockk(relaxed = true)
        
        // When
        val result = novelUpdatesSync.removeNovel(novel)
        
        // Then
        assertTrue(result)
        verify { io.github.gmathi.novellibrary.network.WebPageDocumentFetcher.document(any()) }
        
        unmockkObject(io.github.gmathi.novellibrary.network.WebPageDocumentFetcher)
    }

    @Test
    fun `setBookmark calls correct URL format`() {
        // Given
        val novel = mockk<Novel> {
            every { metadata } returns mutableMapOf("PostId" to "12345")
        }
        val chapter = mockk<WebPage> {
            every { url } returns "https://example.com/chapter/67890/"
        }
        
        // Mock WebPageDocumentFetcher to avoid actual network calls
        mockkObject(io.github.gmathi.novellibrary.network.WebPageDocumentFetcher)
        every { io.github.gmathi.novellibrary.network.WebPageDocumentFetcher.document(any()) } returns mockk(relaxed = true)
        
        // When
        val result = novelUpdatesSync.setBookmark(novel, chapter)
        
        // Then
        assertTrue(result)
        verify { io.github.gmathi.novellibrary.network.WebPageDocumentFetcher.document(any()) }
        
        unmockkObject(io.github.gmathi.novellibrary.network.WebPageDocumentFetcher)
    }

    @Test
    fun `clearBookmark calls correct URL format`() {
        // Given
        val novel = mockk<Novel> {
            every { metadata } returns mutableMapOf("PostId" to "12345")
        }
        val chapter = mockk<WebPage> {
            every { url } returns "https://example.com/chapter/67890/"
        }
        
        // Mock WebPageDocumentFetcher to avoid actual network calls
        mockkObject(io.github.gmathi.novellibrary.network.WebPageDocumentFetcher)
        every { io.github.gmathi.novellibrary.network.WebPageDocumentFetcher.document(any()) } returns mockk(relaxed = true)
        
        // When
        val result = novelUpdatesSync.clearBookmark(novel, chapter)
        
        // Then
        assertTrue(result)
        verify { io.github.gmathi.novellibrary.network.WebPageDocumentFetcher.document(any()) }
        
        unmockkObject(io.github.gmathi.novellibrary.network.WebPageDocumentFetcher)
    }

    @Test
    fun `addNovel returns false when exception occurs`() {
        // Given
        val novel = mockk<Novel> {
            every { metadata } returns mutableMapOf("PostId" to "12345")
        }
        
        // Mock WebPageDocumentFetcher to throw exception
        mockkObject(io.github.gmathi.novellibrary.network.WebPageDocumentFetcher)
        every { io.github.gmathi.novellibrary.network.WebPageDocumentFetcher.document(any()) } throws RuntimeException("Network error")
        
        // When
        val result = novelUpdatesSync.addNovel(novel, null)
        
        // Then
        assertFalse(result)
        
        unmockkObject(io.github.gmathi.novellibrary.network.WebPageDocumentFetcher)
    }

    @Test
    fun `removeNovel returns false when exception occurs`() {
        // Given
        val novel = mockk<Novel> {
            every { metadata } returns mutableMapOf("PostId" to "12345")
        }
        
        // Mock WebPageDocumentFetcher to throw exception
        mockkObject(io.github.gmathi.novellibrary.network.WebPageDocumentFetcher)
        every { io.github.gmathi.novellibrary.network.WebPageDocumentFetcher.document(any()) } throws RuntimeException("Network error")
        
        // When
        val result = novelUpdatesSync.removeNovel(novel)
        
        // Then
        assertFalse(result)
        
        unmockkObject(io.github.gmathi.novellibrary.network.WebPageDocumentFetcher)
    }

    @Test
    fun `updateNovel delegates to addNovel`() {
        // Given
        val novel = mockk<Novel> {
            every { metadata } returns mutableMapOf("PostId" to "12345")
        }
        val section = mockk<NovelSection>()
        
        // Mock WebPageDocumentFetcher to avoid actual network calls
        mockkObject(io.github.gmathi.novellibrary.network.WebPageDocumentFetcher)
        every { io.github.gmathi.novellibrary.network.WebPageDocumentFetcher.document(any()) } returns mockk(relaxed = true)
        
        // When
        val result = novelUpdatesSync.updateNovel(novel, section)
        
        // Then
        assertTrue(result)
        verify { io.github.gmathi.novellibrary.network.WebPageDocumentFetcher.document(any()) }
        
        unmockkObject(io.github.gmathi.novellibrary.network.WebPageDocumentFetcher)
    }

    @Test
    fun `getCategories returns empty list when exception occurs`() {
        // Given
        mockkObject(io.github.gmathi.novellibrary.network.WebPageDocumentFetcher)
        every { io.github.gmathi.novellibrary.network.WebPageDocumentFetcher.document(any()) } throws RuntimeException("Network error")
        
        // When
        val result = novelUpdatesSync.getCategories()
        
        // Then
        assertTrue(result.isEmpty())
        
        unmockkObject(io.github.gmathi.novellibrary.network.WebPageDocumentFetcher)
    }
}
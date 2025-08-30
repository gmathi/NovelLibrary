package io.github.gmathi.novellibrary.source

import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.mockk.mockk
import org.junit.Test
import org.junit.Assert.*

class HttpSourceTest {

    private val mockNetworkHelper = mockk<NetworkHelper>()
    private val mockDataCenter = mockk<DataCenter>()

    @Test
    fun `NovelUpdatesSource should inject dependencies via constructor`() {
        // Given
        val source = NovelUpdatesSource(mockNetworkHelper, mockDataCenter)
        
        // When & Then
        assertNotNull(source)
        assertEquals("Novel Updates", source.name)
        assertEquals("en", source.lang)
        assertTrue(source.supportsLatest)
    }

    @Test
    fun `HttpSource should accept NetworkHelper and DataCenter dependencies`() {
        // Given
        val concreteSource = object : io.github.gmathi.novellibrary.model.source.online.HttpSource(mockNetworkHelper, mockDataCenter) {
            override val baseUrl: String = "https://example.com"
            override val lang: String = "en"
            override val name: String = "Test Source"
            override val id: Long = 12345L
            
            override fun popularNovelsRequest(page: Int) = mockk<okhttp3.Request>()
            override fun popularNovelsParse(response: okhttp3.Response) = mockk<io.github.gmathi.novellibrary.model.other.NovelsPage>()
            override fun searchNovelsRequest(page: Int, query: String, filters: io.github.gmathi.novellibrary.model.source.filter.FilterList) = mockk<okhttp3.Request>()
            override fun searchNovelsParse(response: okhttp3.Response) = mockk<io.github.gmathi.novellibrary.model.other.NovelsPage>()
            override fun latestUpdatesRequest(page: Int) = mockk<okhttp3.Request>()
            override fun latestUpdatesParse(response: okhttp3.Response) = mockk<io.github.gmathi.novellibrary.model.other.NovelsPage>()
            override fun novelDetailsParse(novel: io.github.gmathi.novellibrary.model.database.Novel, response: okhttp3.Response) = novel
            override fun chapterListParse(novel: io.github.gmathi.novellibrary.model.database.Novel, response: okhttp3.Response) = emptyList<io.github.gmathi.novellibrary.model.database.WebPage>()
        }
        
        // When & Then
        assertNotNull(concreteSource)
        assertEquals("Test Source", concreteSource.name)
        assertEquals("en", concreteSource.lang)
        assertEquals("https://example.com", concreteSource.baseUrl)
    }

    @Test
    fun `ParsedHttpSource should accept NetworkHelper and DataCenter dependencies`() {
        // Given
        val concreteSource = object : io.github.gmathi.novellibrary.model.source.online.ParsedHttpSource(mockNetworkHelper, mockDataCenter) {
            override val baseUrl: String = "https://example.com"
            override val lang: String = "en"
            override val name: String = "Test Parsed Source"
            override val id: Long = 54321L
            
            override fun popularNovelsSelector() = "div.novel"
            override fun popularNovelsFromElement(element: org.jsoup.nodes.Element) = mockk<io.github.gmathi.novellibrary.model.database.Novel>()
            override fun popularNovelNextPageSelector() = "a.next"
            override fun searchNovelsSelector() = "div.search-result"
            override fun searchNovelsFromElement(element: org.jsoup.nodes.Element) = mockk<io.github.gmathi.novellibrary.model.database.Novel>()
            override fun searchNovelsNextPageSelector() = "a.next"
            override fun latestUpdatesSelector() = "div.latest"
            override fun latestUpdatesFromElement(element: org.jsoup.nodes.Element) = mockk<io.github.gmathi.novellibrary.model.database.Novel>()
            override fun latestUpdatesNextPageSelector() = "a.next"
            override fun novelDetailsParse(novel: io.github.gmathi.novellibrary.model.database.Novel, document: org.jsoup.nodes.Document) = novel
            override fun chapterListSelector() = "div.chapter"
            override fun chapterFromElement(element: org.jsoup.nodes.Element) = mockk<io.github.gmathi.novellibrary.model.database.WebPage>()
        }
        
        // When & Then
        assertNotNull(concreteSource)
        assertEquals("Test Parsed Source", concreteSource.name)
        assertEquals("en", concreteSource.lang)
        assertEquals("https://example.com", concreteSource.baseUrl)
    }
}
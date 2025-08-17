package io.github.gmathi.novellibrary.database.repository

import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.dao.WebPageDao
import io.github.gmathi.novellibrary.database.repository.impl.WebPageRepositoryImpl
import io.github.gmathi.novellibrary.model.database.WebPage
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class WebPageRepositoryTest {

    private lateinit var webPageDao: WebPageDao
    private lateinit var dbHelper: DBHelper
    private lateinit var webPageRepository: WebPageRepository

    @Before
    fun setUp() {
        webPageDao = mockk(relaxed = true)
        dbHelper = mockk(relaxed = true)
        webPageRepository = WebPageRepositoryImpl(webPageDao, dbHelper)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `createWebPage should delegate to dao`() = runTest {
        // Given
        val webPage = WebPage("https://test.com/chapter1", "Chapter 1")
        webPage.novelId = 123L
        coEvery { webPageDao.createWebPage(webPage) } returns true

        // When
        val result = webPageRepository.createWebPage(webPage)

        // Then
        assertTrue(result)
        coVerify { webPageDao.createWebPage(webPage) }
    }

    @Test
    fun `getWebPage should delegate to dao`() = runTest {
        // Given
        val url = "https://test.com/chapter1"
        val expectedWebPage = WebPage(url, "Chapter 1")
        coEvery { webPageDao.getWebPage(url) } returns expectedWebPage

        // When
        val result = webPageRepository.getWebPage(url)

        // Then
        assertEquals(expectedWebPage, result)
        coVerify { webPageDao.getWebPage(url) }
    }

    @Test
    fun `getAllWebPagesFlow should delegate to dao`() = runTest {
        // Given
        val novelId = 123L
        val webPages = listOf(
            WebPage("https://test1.com", "Chapter 1"),
            WebPage("https://test2.com", "Chapter 2")
        )
        every { webPageDao.getAllWebPagesFlow(novelId) } returns flowOf(webPages)

        // When
        val result = webPageRepository.getAllWebPagesFlow(novelId).first()

        // Then
        assertEquals(webPages, result)
        verify { webPageDao.getAllWebPagesFlow(novelId) }
    }

    @Test
    fun `getAllWebPages should delegate to dao`() = runTest {
        // Given
        val novelId = 123L
        val webPages = listOf(
            WebPage("https://test1.com", "Chapter 1"),
            WebPage("https://test2.com", "Chapter 2")
        )
        coEvery { webPageDao.getAllWebPages(novelId) } returns webPages

        // When
        val result = webPageRepository.getAllWebPages(novelId)

        // Then
        assertEquals(webPages, result)
        coVerify { webPageDao.getAllWebPages(novelId) }
    }

    @Test
    fun `getAllWebPagesFlow with translator source should delegate to dao`() = runTest {
        // Given
        val novelId = 123L
        val translatorSource = "Source1"
        val webPages = listOf(WebPage("https://test1.com", "Chapter 1"))
        every { webPageDao.getAllWebPagesFlow(novelId, translatorSource) } returns flowOf(webPages)

        // When
        val result = webPageRepository.getAllWebPagesFlow(novelId, translatorSource).first()

        // Then
        assertEquals(webPages, result)
        verify { webPageDao.getAllWebPagesFlow(novelId, translatorSource) }
    }

    @Test
    fun `getAllWebPages with translator source should delegate to dao`() = runTest {
        // Given
        val novelId = 123L
        val translatorSource = "Source1"
        val webPages = listOf(WebPage("https://test1.com", "Chapter 1"))
        coEvery { webPageDao.getAllWebPages(novelId, translatorSource) } returns webPages

        // When
        val result = webPageRepository.getAllWebPages(novelId, translatorSource)

        // Then
        assertEquals(webPages, result)
        coVerify { webPageDao.getAllWebPages(novelId, translatorSource) }
    }

    @Test
    fun `getWebPage with offset should delegate to dao`() = runTest {
        // Given
        val novelId = 123L
        val offset = 5
        val expectedWebPage = WebPage("https://test.com/chapter6", "Chapter 6")
        coEvery { webPageDao.getWebPage(novelId, offset) } returns expectedWebPage

        // When
        val result = webPageRepository.getWebPage(novelId, offset)

        // Then
        assertEquals(expectedWebPage, result)
        coVerify { webPageDao.getWebPage(novelId, offset) }
    }

    @Test
    fun `getWebPage with translator source and offset should delegate to dao`() = runTest {
        // Given
        val novelId = 123L
        val translatorSource = "Source1"
        val offset = 3
        val expectedWebPage = WebPage("https://test.com/chapter4", "Chapter 4")
        coEvery { webPageDao.getWebPage(novelId, translatorSource, offset) } returns expectedWebPage

        // When
        val result = webPageRepository.getWebPage(novelId, translatorSource, offset)

        // Then
        assertEquals(expectedWebPage, result)
        coVerify { webPageDao.getWebPage(novelId, translatorSource, offset) }
    }

    @Test
    fun `deleteWebPages should delegate to dao`() = runTest {
        // Given
        val novelId = 123L
        coEvery { webPageDao.deleteWebPages(novelId) } just Runs

        // When
        webPageRepository.deleteWebPages(novelId)

        // Then
        coVerify { webPageDao.deleteWebPages(novelId) }
    }

    @Test
    fun `deleteWebPage should delegate to dao`() = runTest {
        // Given
        val url = "https://test.com/chapter1"
        coEvery { webPageDao.deleteWebPage(url) } just Runs

        // When
        webPageRepository.deleteWebPage(url)

        // Then
        coVerify { webPageDao.deleteWebPage(url) }
    }

    @Test
    fun `withTransaction should execute block within transaction`() = runTest {
        // Given
        val mockDatabase = mockk<android.database.sqlite.SQLiteDatabase>(relaxed = true)
        every { dbHelper.writableDatabase } returns mockDatabase
        var blockExecuted = false

        // When
        webPageRepository.withTransaction {
            blockExecuted = true
            "result"
        }

        // Then
        assertTrue(blockExecuted)
        verify { mockDatabase.beginTransaction() }
        verify { mockDatabase.setTransactionSuccessful() }
        verify { mockDatabase.endTransaction() }
    }
}
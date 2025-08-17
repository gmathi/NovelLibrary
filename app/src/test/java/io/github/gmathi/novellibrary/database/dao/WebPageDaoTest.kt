package io.github.gmathi.novellibrary.database.dao

import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.dao.impl.WebPageDaoImpl
import io.github.gmathi.novellibrary.model.database.WebPage
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class WebPageDaoTest {

    private lateinit var dbHelper: DBHelper
    private lateinit var webPageDao: WebPageDao

    @Before
    fun setUp() {
        dbHelper = mockk(relaxed = true)
        webPageDao = WebPageDaoImpl(dbHelper)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `createWebPage should return true when page created successfully`() = runTest {
        // Given
        val webPage = WebPage("https://test.com/chapter1", "Chapter 1")
        webPage.novelId = 123L
        webPage.orderId = 1L

        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        every { dbHelper.writableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.count } returns 0
        every { dbHelper.writableDatabase.insert(any(), any(), any()) } returns 1L

        // When
        val result = webPageDao.createWebPage(webPage)

        // Then
        assertTrue(result)
        verify { dbHelper.writableDatabase.insert(any(), any(), any()) }
    }

    @Test
    fun `createWebPage should return false when page already exists`() = runTest {
        // Given
        val webPage = WebPage("https://test.com/chapter1", "Chapter 1")
        webPage.novelId = 123L

        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        every { dbHelper.writableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.count } returns 1

        // When
        val result = webPageDao.createWebPage(webPage)

        // Then
        assertFalse(result)
        verify(exactly = 0) { dbHelper.writableDatabase.insert(any(), any(), any()) }
    }

    @Test
    fun `getWebPage should return webpage when found`() = runTest {
        // Given
        val url = "https://test.com/chapter1"
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns true
        every { mockCursor.getColumnIndex(any()) } returns 0
        every { mockCursor.getString(0) } returns url andThen "Chapter 1" andThen "Source1"
        every { mockCursor.getLong(0) } returns 123L andThen 1L

        // When
        val result = webPageDao.getWebPage(url)

        // Then
        assertNotNull(result)
        assertEquals(url, result?.url)
        assertEquals("Chapter 1", result?.chapterName)
        assertEquals(123L, result?.novelId)
    }

    @Test
    fun `getWebPage should return null when not found`() = runTest {
        // Given
        val url = "https://test.com/nonexistent"
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns false

        // When
        val result = webPageDao.getWebPage(url)

        // Then
        assertNull(result)
    }

    @Test
    fun `getAllWebPages should return list of webpages for novel`() = runTest {
        // Given
        val novelId = 123L
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns true
        every { mockCursor.moveToNext() } returns true andThen false
        every { mockCursor.getColumnIndex(any()) } returns 0
        every { mockCursor.getString(0) } returns "https://test1.com" andThen "Chapter 1" andThen "Source1" andThen "https://test2.com" andThen "Chapter 2" andThen "Source1"
        every { mockCursor.getLong(0) } returns novelId andThen 1L andThen novelId andThen 2L

        // When
        val result = webPageDao.getAllWebPages(novelId)

        // Then
        assertEquals(2, result.size)
        assertEquals("https://test1.com", result[0].url)
        assertEquals("https://test2.com", result[1].url)
    }

    @Test
    fun `getAllWebPagesFlow should emit list of webpages`() = runTest {
        // Given
        val novelId = 123L
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns true
        every { mockCursor.moveToNext() } returns false
        every { mockCursor.getColumnIndex(any()) } returns 0
        every { mockCursor.getString(0) } returns "https://test.com" andThen "Chapter 1" andThen "Source1"
        every { mockCursor.getLong(0) } returns novelId andThen 1L

        // When
        val result = webPageDao.getAllWebPagesFlow(novelId).first()

        // Then
        assertEquals(1, result.size)
        assertEquals("https://test.com", result[0].url)
    }

    @Test
    fun `getAllWebPages with translator source should filter by source`() = runTest {
        // Given
        val novelId = 123L
        val translatorSource = "Source1"
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns true
        every { mockCursor.moveToNext() } returns false
        every { mockCursor.getColumnIndex(any()) } returns 0
        every { mockCursor.getString(0) } returns "https://test.com" andThen "Chapter 1" andThen translatorSource
        every { mockCursor.getLong(0) } returns novelId andThen 1L

        // When
        val result = webPageDao.getAllWebPages(novelId, translatorSource)

        // Then
        assertEquals(1, result.size)
        assertEquals(translatorSource, result[0].translatorSourceName)
    }

    @Test
    fun `getWebPage with offset should return correct webpage`() = runTest {
        // Given
        val novelId = 123L
        val offset = 5
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns true
        every { mockCursor.getColumnIndex(any()) } returns 0
        every { mockCursor.getString(0) } returns "https://test.com" andThen "Chapter 6" andThen "Source1"
        every { mockCursor.getLong(0) } returns novelId andThen 6L

        // When
        val result = webPageDao.getWebPage(novelId, offset)

        // Then
        assertNotNull(result)
        assertEquals("Chapter 6", result?.chapterName)
    }

    @Test
    fun `deleteWebPages should delete all pages for novel`() = runTest {
        // Given
        val novelId = 123L

        every { dbHelper.writableDatabase.delete(any(), any(), any()) } returns 5

        // When
        webPageDao.deleteWebPages(novelId)

        // Then
        verify { dbHelper.writableDatabase.delete(any(), any(), arrayOf(novelId.toString())) }
    }

    @Test
    fun `deleteWebPage should delete specific page by url`() = runTest {
        // Given
        val url = "https://test.com/chapter1"

        every { dbHelper.writableDatabase.delete(any(), any(), any()) } returns 1

        // When
        webPageDao.deleteWebPage(url)

        // Then
        verify { dbHelper.writableDatabase.delete(any(), any(), arrayOf(url)) }
    }
}
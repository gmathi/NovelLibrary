package io.github.gmathi.novellibrary.service.database

import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.database.Download
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ServiceDatabaseManagerTest {

    private lateinit var dbHelper: DBHelper
    private lateinit var serviceDatabaseManager: ServiceDatabaseManager

    @Before
    fun setUp() {
        dbHelper = mockk(relaxed = true)
        serviceDatabaseManager = ServiceDatabaseManager(dbHelper)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `getAllNovels should return list of novels`() = runTest {
        // Given
        val novels = listOf(
            Novel("Novel 1", "https://test1.com", 1L),
            Novel("Novel 2", "https://test2.com", 1L)
        )
        
        // Mock the database operations
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns true
        every { mockCursor.moveToNext() } returns true andThen false
        every { mockCursor.getColumnIndex(any()) } returns 0
        every { mockCursor.getString(0) } returns "Novel 1" andThen "https://test1.com" andThen "Novel 2" andThen "https://test2.com"
        every { mockCursor.getLong(0) } returns 1L andThen 1L andThen 2L andThen 1L

        // When
        val result = serviceDatabaseManager.getAllNovels()

        // Then
        assertEquals(2, result.size)
        assertEquals("Novel 1", result[0].name)
        assertEquals("Novel 2", result[1].name)
    }

    @Test
    fun `getNovel should return novel when found`() = runTest {
        // Given
        val novelId = 123L
        val expectedNovel = Novel("Test Novel", "https://test.com", 1L)
        expectedNovel.id = novelId
        
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns true
        every { mockCursor.getColumnIndex(any()) } returns 0
        every { mockCursor.getString(0) } returns "Test Novel" andThen "https://test.com"
        every { mockCursor.getLong(0) } returns novelId andThen 1L
        every { dbHelper.getGenres(novelId) } returns listOf("Fantasy")

        // When
        val result = serviceDatabaseManager.getNovel(novelId)

        // Then
        assertNotNull(result)
        assertEquals(expectedNovel.name, result?.name)
        assertEquals(expectedNovel.url, result?.url)
        assertEquals(expectedNovel.id, result?.id)
    }

    @Test
    fun `updateNovelMetaData should update novel metadata`() = runTest {
        // Given
        val novel = Novel("Test Novel", "https://test.com", 1L)
        novel.id = 123L
        novel.metadata["test"] = "value"
        
        every { dbHelper.writableDatabase.update(any(), any(), any(), any()) } returns 1

        // When
        serviceDatabaseManager.updateNovelMetaData(novel)

        // Then
        verify { dbHelper.writableDatabase.update(any(), any(), any(), any()) }
    }

    @Test
    fun `updateChaptersAndReleasesCount should update counts`() = runTest {
        // Given
        val novelId = 123L
        val totalChapters = 100L
        val newReleases = 5L
        
        every { dbHelper.writableDatabase.update(any(), any(), any(), any()) } returns 1

        // When
        serviceDatabaseManager.updateChaptersAndReleasesCount(novelId, totalChapters, newReleases)

        // Then
        verify { dbHelper.writableDatabase.update(any(), any(), any(), any()) }
    }

    @Test
    fun `getAllWebPages should return web pages for novel`() = runTest {
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
        val result = serviceDatabaseManager.getAllWebPages(novelId)

        // Then
        assertEquals(2, result.size)
        assertEquals("https://test1.com", result[0].url)
        assertEquals("https://test2.com", result[1].url)
    }

    @Test
    fun `createWebPage should create web page successfully`() = runTest {
        // Given
        val webPage = WebPage("https://test.com/chapter1", "Chapter 1")
        webPage.novelId = 123L
        
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        every { dbHelper.writableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.count } returns 0
        every { dbHelper.writableDatabase.insert(any(), any(), any()) } returns 1L

        // When
        val result = serviceDatabaseManager.createWebPage(webPage)

        // Then
        assertTrue(result)
        verify { dbHelper.writableDatabase.insert(any(), any(), any()) }
    }

    @Test
    fun `getDownloadItemInQueue should return next download`() = runTest {
        // Given
        val novelId = 123L
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns true
        every { mockCursor.getColumnIndex(any()) } returns 0
        every { mockCursor.getString(0) } returns "https://test.com" andThen "Test Novel" andThen "Chapter 1" andThen "{}"
        every { mockCursor.getLong(0) } returns novelId
        every { mockCursor.getInt(0) } returns Download.STATUS_IN_QUEUE andThen 1

        // When
        val result = serviceDatabaseManager.getDownloadItemInQueue(novelId)

        // Then
        assertNotNull(result)
        assertEquals("https://test.com", result?.webPageUrl)
        assertEquals(novelId, result?.novelId)
    }

    @Test
    fun `getRemainingDownloadsCountForNovel should return correct count`() = runTest {
        // Given
        val novelId = 123L
        val expectedCount = 5
        
        mockkStatic(android.database.DatabaseUtils::class)
        every { android.database.DatabaseUtils.longForQuery(any(), any(), any()) } returns expectedCount.toLong()

        // When
        val result = serviceDatabaseManager.getRemainingDownloadsCountForNovel(novelId)

        // Then
        assertEquals(expectedCount, result)
    }

    @Test
    fun `updateDownloadStatus should update all download statuses`() = runTest {
        // Given
        val status = Download.STATUS_PAUSED
        val expectedResult = 3L
        
        every { dbHelper.writableDatabase.update(any(), any(), any(), any()) } returns expectedResult.toInt()

        // When
        val result = serviceDatabaseManager.updateDownloadStatus(status)

        // Then
        assertEquals(expectedResult, result)
        verify { dbHelper.writableDatabase.update(any(), any(), null, null) }
    }

    @Test
    fun `performSyncTransaction should execute all operations in transaction`() = runTest {
        // Given
        val novel = Novel("Test Novel", "https://test.com", 1L)
        novel.id = 123L
        novel.chaptersCount = 50L
        novel.newReleasesCount = 2L
        
        val chapters = listOf(
            WebPage("https://test.com/ch1", "Chapter 1"),
            WebPage("https://test.com/ch2", "Chapter 2")
        )
        
        val webPageSettings = listOf(
            WebPageSettings("https://test.com/ch1", novel.id),
            WebPageSettings("https://test.com/ch2", novel.id)
        )
        
        val mockDatabase = mockk<android.database.sqlite.SQLiteDatabase>(relaxed = true)
        every { dbHelper.writableDatabase } returns mockDatabase
        every { mockDatabase.update(any(), any(), any(), any()) } returns 1
        every { mockDatabase.insert(any(), any(), any()) } returns 1L
        every { dbHelper.createWebPageSettings(any(), any()) } just Runs

        // When
        serviceDatabaseManager.performSyncTransaction(novel, chapters, webPageSettings)

        // Then
        verify { mockDatabase.beginTransaction() }
        verify { mockDatabase.setTransactionSuccessful() }
        verify { mockDatabase.endTransaction() }
        verify(exactly = 2) { dbHelper.createWebPageSettings(any(), any()) }
    }

    @Test
    fun `hasDownloadsInQueue should return true when downloads exist`() = runTest {
        // Given
        val novelId = 123L
        
        mockkStatic(android.database.DatabaseUtils::class)
        every { android.database.DatabaseUtils.longForQuery(any(), any(), any()) } returns 1L

        // When
        val result = serviceDatabaseManager.hasDownloadsInQueue(novelId)

        // Then
        assertTrue(result)
    }

    @Test
    fun `hasDownloadsInQueue should return false when no downloads exist`() = runTest {
        // Given
        val novelId = 123L
        
        mockkStatic(android.database.DatabaseUtils::class)
        every { android.database.DatabaseUtils.longForQuery(any(), any(), any()) } returns 0L

        // When
        val result = serviceDatabaseManager.hasDownloadsInQueue(novelId)

        // Then
        assertFalse(result)
    }
}
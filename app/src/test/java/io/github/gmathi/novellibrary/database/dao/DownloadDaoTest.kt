package io.github.gmathi.novellibrary.database.dao

import android.database.DatabaseUtils
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.dao.impl.DownloadDaoImpl
import io.github.gmathi.novellibrary.model.database.Download
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DownloadDaoTest {

    private lateinit var dbHelper: DBHelper
    private lateinit var downloadDao: DownloadDao

    @Before
    fun setUp() {
        dbHelper = mockk(relaxed = true)
        downloadDao = DownloadDaoImpl(dbHelper)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `createDownload should create download when not exists`() = runTest {
        // Given
        val download = Download("https://test.com/chapter1", "Test Novel", 123L, "Chapter 1")
        download.orderId = 1

        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        every { dbHelper.writableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.count } returns 0
        every { dbHelper.writableDatabase.insert(any(), any(), any()) } returns 1L

        // When
        downloadDao.createDownload(download)

        // Then
        verify { dbHelper.writableDatabase.insert(any(), any(), any()) }
    }

    @Test
    fun `createDownload should not create download when already exists`() = runTest {
        // Given
        val download = Download("https://test.com/chapter1", "Test Novel", 123L, "Chapter 1")

        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        every { dbHelper.writableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.count } returns 1

        // When
        downloadDao.createDownload(download)

        // Then
        verify(exactly = 0) { dbHelper.writableDatabase.insert(any(), any(), any()) }
    }

    @Test
    fun `getDownload should return download when found`() = runTest {
        // Given
        val webPageUrl = "https://test.com/chapter1"
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns true
        every { mockCursor.getColumnIndex(any()) } returns 0
        every { mockCursor.getString(0) } returns webPageUrl andThen "Test Novel" andThen "Chapter 1" andThen "{}"
        every { mockCursor.getLong(0) } returns 123L
        every { mockCursor.getInt(0) } returns Download.STATUS_IN_QUEUE andThen 1

        // When
        val result = downloadDao.getDownload(webPageUrl)

        // Then
        assertNotNull(result)
        assertEquals(webPageUrl, result?.webPageUrl)
        assertEquals("Test Novel", result?.novelName)
        assertEquals("Chapter 1", result?.chapter)
    }

    @Test
    fun `getDownload should return null when not found`() = runTest {
        // Given
        val webPageUrl = "https://test.com/nonexistent"
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns false

        // When
        val result = downloadDao.getDownload(webPageUrl)

        // Then
        assertNull(result)
    }

    @Test
    fun `getAllDownloads should return list of downloads`() = runTest {
        // Given
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns true
        every { mockCursor.moveToNext() } returns true andThen false
        every { mockCursor.getColumnIndex(any()) } returns 0
        every { mockCursor.getString(0) } returns "https://test1.com" andThen "Novel 1" andThen "Chapter 1" andThen "{}" andThen "https://test2.com" andThen "Novel 2" andThen "Chapter 2" andThen "{}"
        every { mockCursor.getLong(0) } returns 123L andThen 124L
        every { mockCursor.getInt(0) } returns Download.STATUS_IN_QUEUE andThen 1 andThen Download.STATUS_IN_QUEUE andThen 2

        // When
        val result = downloadDao.getAllDownloads()

        // Then
        assertEquals(2, result.size)
        assertEquals("https://test1.com", result[0].webPageUrl)
        assertEquals("https://test2.com", result[1].webPageUrl)
    }

    @Test
    fun `getAllDownloadsFlow should emit list of downloads`() = runTest {
        // Given
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns true
        every { mockCursor.moveToNext() } returns false
        every { mockCursor.getColumnIndex(any()) } returns 0
        every { mockCursor.getString(0) } returns "https://test.com" andThen "Test Novel" andThen "Chapter 1" andThen "{}"
        every { mockCursor.getLong(0) } returns 123L
        every { mockCursor.getInt(0) } returns Download.STATUS_IN_QUEUE andThen 1

        // When
        val result = downloadDao.getAllDownloadsFlow().first()

        // Then
        assertEquals(1, result.size)
        assertEquals("https://test.com", result[0].webPageUrl)
    }

    @Test
    fun `getAllDownloadsForNovel should return downloads for specific novel`() = runTest {
        // Given
        val novelId = 123L
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns true
        every { mockCursor.moveToNext() } returns false
        every { mockCursor.getColumnIndex(any()) } returns 0
        every { mockCursor.getString(0) } returns "https://test.com" andThen "Test Novel" andThen "Chapter 1" andThen "{}"
        every { mockCursor.getLong(0) } returns novelId
        every { mockCursor.getInt(0) } returns Download.STATUS_IN_QUEUE andThen 1

        // When
        val result = downloadDao.getAllDownloadsForNovel(novelId)

        // Then
        assertEquals(1, result.size)
        assertEquals(novelId, result[0].novelId)
    }

    @Test
    fun `getDownloadItemInQueue should return next queued download`() = runTest {
        // Given
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns true
        every { mockCursor.getColumnIndex(any()) } returns 0
        every { mockCursor.getString(0) } returns "https://test.com" andThen "Test Novel" andThen "Chapter 1" andThen "{}"
        every { mockCursor.getLong(0) } returns 123L
        every { mockCursor.getInt(0) } returns Download.STATUS_IN_QUEUE andThen 1

        // When
        val result = downloadDao.getDownloadItemInQueue()

        // Then
        assertNotNull(result)
        assertEquals(Download.STATUS_IN_QUEUE, result?.status)
    }

    @Test
    fun `getDownloadItemInQueue should return null when no queued downloads`() = runTest {
        // Given
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns false

        // When
        val result = downloadDao.getDownloadItemInQueue()

        // Then
        assertNull(result)
    }

    @Test
    fun `hasDownloadsInQueue should return true when novel has queued downloads`() = runTest {
        // Given
        val novelId = 123L
        
        mockkStatic(DatabaseUtils::class)
        every { DatabaseUtils.longForQuery(any(), any(), any()) } returns 1L

        // When
        val result = downloadDao.hasDownloadsInQueue(novelId)

        // Then
        assertTrue(result)
    }

    @Test
    fun `hasDownloadsInQueue should return false when novel has no queued downloads`() = runTest {
        // Given
        val novelId = 123L
        
        mockkStatic(DatabaseUtils::class)
        every { DatabaseUtils.longForQuery(any(), any(), any()) } returns 0L

        // When
        val result = downloadDao.hasDownloadsInQueue(novelId)

        // Then
        assertFalse(result)
    }

    @Test
    fun `getRemainingDownloadsCountForNovel should return correct count`() = runTest {
        // Given
        val novelId = 123L
        val expectedCount = 5
        
        mockkStatic(DatabaseUtils::class)
        every { DatabaseUtils.longForQuery(any(), any(), any()) } returns expectedCount.toLong()

        // When
        val result = downloadDao.getRemainingDownloadsCountForNovel(novelId)

        // Then
        assertEquals(expectedCount, result)
    }

    @Test
    fun `updateDownloadStatusWebPageUrl should update download status`() = runTest {
        // Given
        val status = Download.STATUS_COMPLETED
        val webPageUrl = "https://test.com/chapter1"

        every { dbHelper.writableDatabase.update(any(), any(), any(), any()) } returns 1

        // When
        downloadDao.updateDownloadStatusWebPageUrl(status, webPageUrl)

        // Then
        verify { dbHelper.writableDatabase.update(any(), any(), any(), arrayOf(webPageUrl)) }
    }

    @Test
    fun `deleteDownload should delete download by url`() = runTest {
        // Given
        val webPageUrl = "https://test.com/chapter1"

        every { dbHelper.writableDatabase.delete(any(), any(), any()) } returns 1

        // When
        downloadDao.deleteDownload(webPageUrl)

        // Then
        verify { dbHelper.writableDatabase.delete(any(), any(), arrayOf(webPageUrl)) }
    }

    @Test
    fun `deleteDownloads should delete all downloads for novel`() = runTest {
        // Given
        val novelId = 123L

        every { dbHelper.writableDatabase.delete(any(), any(), any()) } returns 3

        // When
        downloadDao.deleteDownloads(novelId)

        // Then
        verify { dbHelper.writableDatabase.delete(any(), any(), arrayOf(novelId.toString())) }
    }
}
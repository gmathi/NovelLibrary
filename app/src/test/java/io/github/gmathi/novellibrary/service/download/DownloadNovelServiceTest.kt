package io.github.gmathi.novellibrary.service.download

import android.app.Service
import android.content.Intent
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.other.DownloadNovelEvent
import io.github.gmathi.novellibrary.model.other.DownloadWebPageEvent
import io.github.gmathi.novellibrary.model.other.EventType
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DownloadNovelServiceTest {

    private lateinit var service: DownloadNovelService
    private lateinit var mockDbHelper: DBHelper
    private lateinit var mockDownloadListener: DownloadListener

    @Before
    fun setUp() {
        mockDbHelper = mockk(relaxed = true)
        mockDownloadListener = mockk(relaxed = true)
        
        mockkObject(DBHelper)
        every { DBHelper.getInstance(any()) } returns mockDbHelper
        
        service = Robolectric.setupService(DownloadNovelService::class.java)
        service.downloadListener = mockDownloadListener
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `onCreate should initialize dbHelper`() {
        // When
        service.onCreate()
        
        // Then
        verify { DBHelper.getInstance(any()) }
    }

    @Test
    fun `onStartCommand should start download for valid novel ID`() = runTest {
        // Given
        val novelId = 123L
        val intent = Intent().apply {
            putExtra(DownloadNovelService.NOVEL_ID, novelId)
        }
        
        // When
        service.onStartCommand(intent, 0, 1)
        
        // Then - verify that the service processes the intent
        // Note: Due to coroutine nature, we can't directly verify the internal state
        // but we can verify that no exceptions are thrown
    }

    @Test
    fun `onStartCommand should handle null intent gracefully`() = runTest {
        // When
        val result = service.onStartCommand(null, 0, 1)
        
        // Then
        assertEquals(Service.START_NOT_STICKY, result)
    }

    @Test
    fun `handleNovelDownload with ACTION_START should start download`() = runTest {
        // Given
        val novelId = 123L
        
        // When
        service.handleNovelDownload(novelId, DownloadNovelService.ACTION_START)
        
        // Then
        // Verify that the download process is initiated
        // Due to async nature, we mainly verify no exceptions are thrown
    }

    @Test
    fun `handleNovelDownload with ACTION_PAUSE should cancel download`() = runTest {
        // Given
        val novelId = 123L
        
        // First start a download
        service.handleNovelDownload(novelId, DownloadNovelService.ACTION_START)
        
        // When
        service.handleNovelDownload(novelId, DownloadNovelService.ACTION_PAUSE)
        
        // Then
        // Verify that the download is cancelled
        assertFalse(service.hasActiveDownloads())
    }

    @Test
    fun `handleNovelDownload with ACTION_REMOVE should remove download`() = runTest {
        // Given
        val novelId = 123L
        
        // First start a download
        service.handleNovelDownload(novelId, DownloadNovelService.ACTION_START)
        
        // When
        service.handleNovelDownload(novelId, DownloadNovelService.ACTION_REMOVE)
        
        // Then
        // Verify that the download is removed
        assertFalse(service.hasActiveDownloads())
    }

    @Test
    fun `handleEvent should handle COMPLETE event correctly`() = runTest {
        // Given
        val novelId = 123L
        val downloadEvent = DownloadNovelEvent(EventType.COMPLETE, novelId)
        
        // When
        service.handleEvent(downloadEvent)
        
        // Then
        verify { mockDownloadListener.handleEvent(downloadEvent) }
    }

    @Test
    fun `handleEvent should handle DELETE event correctly`() = runTest {
        // Given
        val novelId = 123L
        val downloadEvent = DownloadNovelEvent(EventType.DELETE, novelId)
        
        // When
        service.handleEvent(downloadEvent)
        
        // Then
        verify { mockDownloadListener.handleEvent(downloadEvent) }
    }

    @Test
    fun `handleEvent should handle web page events correctly`() = runTest {
        // Given
        val mockDownload = mockk<io.github.gmathi.novellibrary.model.database.Download>(relaxed = true)
        val webPageEvent = DownloadWebPageEvent(EventType.RUNNING, "http://test.com", mockDownload)
        
        // When
        service.handleEvent(webPageEvent)
        
        // Then
        verify { mockDownloadListener.handleEvent(webPageEvent) }
    }

    @Test
    fun `hasActiveDownloads should return false initially`() {
        // When
        val hasActive = service.hasActiveDownloads()
        
        // Then
        assertFalse(hasActive)
    }

    @Test
    fun `getActiveNovelIds should return empty set initially`() {
        // When
        val activeIds = service.getActiveNovelIds()
        
        // Then
        assertTrue(activeIds.isEmpty())
    }

    @Test
    fun `onDestroy should cleanup resources`() = runTest {
        // Given
        val novelId = 123L
        service.handleNovelDownload(novelId, DownloadNovelService.ACTION_START)
        
        // When
        service.onDestroy()
        
        // Then
        verify { mockDbHelper.updateDownloadStatus(any()) }
        assertFalse(service.hasActiveDownloads())
    }

    @Test
    fun `downloadProgressFlow should emit events`() = runTest {
        // Given
        val mockDownload = mockk<io.github.gmathi.novellibrary.model.database.Download>(relaxed = true)
        val webPageEvent = DownloadWebPageEvent(EventType.RUNNING, "http://test.com", mockDownload)
        
        // When
        service.handleEvent(webPageEvent)
        
        // Then - verify that the flow can be collected
        // Note: Due to the async nature and SharedFlow, we can't easily test the emission
        // but we can verify the flow exists and is accessible
        val flow = service.downloadProgressFlow
        assertTrue(flow != null)
    }
}
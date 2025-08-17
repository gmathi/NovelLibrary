package io.github.gmathi.novellibrary.service.sync

import android.content.Context
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.source.Source
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.mockk.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CoroutineSyncServiceTest {

    private lateinit var context: Context
    private lateinit var dbHelper: DBHelper
    private lateinit var coroutineSyncService: CoroutineSyncService
    private lateinit var networkHelper: NetworkHelper

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        dbHelper = mockk(relaxed = true)
        networkHelper = mockk(relaxed = true)
        
        // Mock NetworkHelper constructor
        mockkConstructor(NetworkHelper::class)
        every { anyConstructed<NetworkHelper>().isConnectedToNetwork() } returns true
        
        coroutineSyncService = CoroutineSyncService(context, dbHelper)
    }

    @After
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    @Test
    fun `performBackgroundSync should return NetworkError when no network`() = runTest {
        // Given
        every { anyConstructed<NetworkHelper>().isConnectedToNetwork() } returns false

        // When
        val result = coroutineSyncService.performBackgroundSync()

        // Then
        assertEquals(SyncResult.NetworkError, result)
    }

    @Test
    fun `performBackgroundSync should return NoUpdates when no novels need sync`() = runTest {
        // Given
        val novels = listOf(
            Novel("Novel 1", "https://test1.com", 1L).apply { id = 1L },
            Novel("Novel 2", "https://test2.com", 1L).apply { id = 2L }
        )
        
        // Mock database operations
        mockDatabaseOperations(novels)
        
        // Mock SourceManager
        val sourceManager = mockk<SourceManager>(relaxed = true)
        mockkConstructor(SourceManager::class)
        every { anyConstructed<SourceManager>().get(any()) } returns mockk<Source>(relaxed = true) {
            every { getChapterList(any()) } returns ArrayList() // Empty list means no updates
        }

        // When
        val result = coroutineSyncService.performBackgroundSync()

        // Then
        assertEquals(SyncResult.NoUpdates, result)
    }

    @Test
    fun `performBackgroundSync should return Success when novels are updated`() = runTest {
        // Given
        val novels = listOf(
            Novel("Novel 1", "https://test1.com", 1L).apply { 
                id = 1L
                metadata["HASH_CODE"] = "100"
            }
        )
        
        val newChapters = listOf(
            WebPage("https://test1.com/ch1", "Chapter 1"),
            WebPage("https://test1.com/ch2", "Chapter 2")
        )
        
        // Mock database operations
        mockDatabaseOperations(novels)
        
        // Mock SourceManager
        mockkConstructor(SourceManager::class)
        val mockSource = mockk<Source>(relaxed = true)
        every { anyConstructed<SourceManager>().get(any()) } returns mockSource
        every { mockSource.getChapterList(any()) } returns newChapters
        
        // Mock database update operations
        every { dbHelper.writableDatabase.update(any(), any(), any(), any()) } returns 1
        every { dbHelper.writableDatabase.insert(any(), any(), any()) } returns 1L
        every { dbHelper.createWebPageSettings(any(), any()) } just Runs

        // When
        val result = coroutineSyncService.performBackgroundSync()

        // Then
        assertTrue(result is SyncResult.Success)
        val successResult = result as SyncResult.Success
        assertEquals(1, successResult.updatedNovels.size)
        assertEquals("Novel 1", successResult.updatedNovels[0].name)
    }

    @Test
    fun `performBackgroundSync should handle source errors gracefully`() = runTest {
        // Given
        val novels = listOf(
            Novel("Novel 1", "https://test1.com", 1L).apply { id = 1L }
        )
        
        // Mock database operations
        mockDatabaseOperations(novels)
        
        // Mock SourceManager to return null (source not found)
        mockkConstructor(SourceManager::class)
        every { anyConstructed<SourceManager>().get(any()) } returns null

        // When
        val result = coroutineSyncService.performBackgroundSync()

        // Then
        assertEquals(SyncResult.NoUpdates, result)
    }

    @Test
    fun `getSyncProgressFlow should emit progress updates`() = runTest {
        // Given
        val novels = listOf(
            Novel("Novel 1", "https://test1.com", 1L).apply { id = 1L },
            Novel("Novel 2", "https://test2.com", 1L).apply { id = 2L }
        )
        
        // Mock database operations
        mockDatabaseOperations(novels)

        // When
        val progressList = coroutineSyncService.getSyncProgressFlow().toList()

        // Then
        assertTrue(progressList.isNotEmpty())
        assertEquals("Starting sync...", progressList.first().message)
        assertEquals("Sync completed", progressList.last().message)
        assertEquals(100, progressList.last().percentage)
    }

    @Test
    fun `isSyncing should return true when service is active`() = runTest {
        // When
        val result = coroutineSyncService.isSyncing()

        // Then
        assertTrue(result)
    }

    @Test
    fun `cancelSync should cancel ongoing operations`() = runTest {
        // When
        coroutineSyncService.cancelSync()

        // Then
        assertFalse(coroutineSyncService.isSyncing())
    }

    @Test
    fun `cleanup should cancel service scope`() = runTest {
        // When
        coroutineSyncService.cleanup()

        // Then
        assertFalse(coroutineSyncService.isSyncing())
    }

    private fun mockDatabaseOperations(novels: List<Novel>) {
        // Mock getAllNovels
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns novels.isNotEmpty()
        
        if (novels.isNotEmpty()) {
            val moveNextResults = novels.drop(1).map { true } + false
            every { mockCursor.moveToNext() } returnsMany moveNextResults
            every { mockCursor.getColumnIndex(any()) } returns 0
            
            val names = novels.map { it.name }
            val urls = novels.map { it.url }
            every { mockCursor.getString(0) } returnsMany (names + urls)
            every { mockCursor.getLong(0) } returnsMany novels.flatMap { listOf(it.id, it.sourceId) }
        }
        
        // Mock getAllWebPages
        every { mockCursor.count } returns 0
        
        // Mock transaction operations
        val mockDatabase = mockk<android.database.sqlite.SQLiteDatabase>(relaxed = true)
        every { dbHelper.writableDatabase } returns mockDatabase
    }
}
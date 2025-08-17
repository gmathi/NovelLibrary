package io.github.gmathi.novellibrary.database.repository

import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.dao.NovelDao
import io.github.gmathi.novellibrary.database.repository.impl.NovelRepositoryImpl
import io.github.gmathi.novellibrary.model.database.Novel
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NovelRepositoryTest {

    private lateinit var novelDao: NovelDao
    private lateinit var dbHelper: DBHelper
    private lateinit var novelRepository: NovelRepository

    @Before
    fun setUp() {
        novelDao = mockk(relaxed = true)
        dbHelper = mockk(relaxed = true)
        novelRepository = NovelRepositoryImpl(novelDao, dbHelper)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `insertNovel should delegate to dao`() = runTest {
        // Given
        val novel = Novel("Test Novel", "https://test.com/novel", 1L)
        val expectedId = 123L
        coEvery { novelDao.insertNovel(novel) } returns expectedId

        // When
        val result = novelRepository.insertNovel(novel)

        // Then
        assertEquals(expectedId, result)
        coVerify { novelDao.insertNovel(novel) }
    }

    @Test
    fun `getNovelByUrl should delegate to dao`() = runTest {
        // Given
        val novelUrl = "https://test.com/novel"
        val expectedNovel = Novel("Test Novel", novelUrl, 1L)
        coEvery { novelDao.getNovelByUrl(novelUrl) } returns expectedNovel

        // When
        val result = novelRepository.getNovelByUrl(novelUrl)

        // Then
        assertEquals(expectedNovel, result)
        coVerify { novelDao.getNovelByUrl(novelUrl) }
    }

    @Test
    fun `getNovel should delegate to dao`() = runTest {
        // Given
        val novelId = 123L
        val expectedNovel = Novel("Test Novel", "https://test.com/novel", 1L)
        expectedNovel.id = novelId
        coEvery { novelDao.getNovel(novelId) } returns expectedNovel

        // When
        val result = novelRepository.getNovel(novelId)

        // Then
        assertEquals(expectedNovel, result)
        coVerify { novelDao.getNovel(novelId) }
    }

    @Test
    fun `getAllNovelsFlow should delegate to dao`() = runTest {
        // Given
        val novels = listOf(
            Novel("Novel 1", "https://test1.com", 1L),
            Novel("Novel 2", "https://test2.com", 1L)
        )
        every { novelDao.getAllNovelsFlow() } returns flowOf(novels)

        // When
        val result = novelRepository.getAllNovelsFlow().first()

        // Then
        assertEquals(novels, result)
        verify { novelDao.getAllNovelsFlow() }
    }

    @Test
    fun `getAllNovels should delegate to dao`() = runTest {
        // Given
        val novels = listOf(
            Novel("Novel 1", "https://test1.com", 1L),
            Novel("Novel 2", "https://test2.com", 1L)
        )
        coEvery { novelDao.getAllNovels() } returns novels

        // When
        val result = novelRepository.getAllNovels()

        // Then
        assertEquals(novels, result)
        coVerify { novelDao.getAllNovels() }
    }

    @Test
    fun `getAllNovelsFlow with section should delegate to dao`() = runTest {
        // Given
        val novelSectionId = 456L
        val novels = listOf(Novel("Novel 1", "https://test1.com", 1L))
        every { novelDao.getAllNovelsFlow(novelSectionId) } returns flowOf(novels)

        // When
        val result = novelRepository.getAllNovelsFlow(novelSectionId).first()

        // Then
        assertEquals(novels, result)
        verify { novelDao.getAllNovelsFlow(novelSectionId) }
    }

    @Test
    fun `updateNovel should delegate to dao`() = runTest {
        // Given
        val novel = Novel("Updated Novel", "https://test.com/novel", 1L)
        novel.id = 123L
        val expectedResult = 1L
        coEvery { novelDao.updateNovel(novel) } returns expectedResult

        // When
        val result = novelRepository.updateNovel(novel)

        // Then
        assertEquals(expectedResult, result)
        coVerify { novelDao.updateNovel(novel) }
    }

    @Test
    fun `updateNovelMetaData should delegate to dao`() = runTest {
        // Given
        val novel = Novel("Test Novel", "https://test.com/novel", 1L)
        novel.id = 123L
        coEvery { novelDao.updateNovelMetaData(novel) } just Runs

        // When
        novelRepository.updateNovelMetaData(novel)

        // Then
        coVerify { novelDao.updateNovelMetaData(novel) }
    }

    @Test
    fun `updateChaptersAndReleasesCount should delegate to dao`() = runTest {
        // Given
        val novelId = 123L
        val totalChapters = 100L
        val newReleases = 5L
        coEvery { novelDao.updateChaptersAndReleasesCount(novelId, totalChapters, newReleases) } just Runs

        // When
        novelRepository.updateChaptersAndReleasesCount(novelId, totalChapters, newReleases)

        // Then
        coVerify { novelDao.updateChaptersAndReleasesCount(novelId, totalChapters, newReleases) }
    }

    @Test
    fun `updateNewReleasesCount should delegate to dao`() = runTest {
        // Given
        val novelId = 123L
        val newReleases = 3L
        coEvery { novelDao.updateNewReleasesCount(novelId, newReleases) } just Runs

        // When
        novelRepository.updateNewReleasesCount(novelId, newReleases)

        // Then
        coVerify { novelDao.updateNewReleasesCount(novelId, newReleases) }
    }

    @Test
    fun `updateBookmarkCurrentWebPageUrl should delegate to dao`() = runTest {
        // Given
        val novelId = 123L
        val currentChapterUrl = "https://test.com/chapter5"
        coEvery { novelDao.updateBookmarkCurrentWebPageUrl(novelId, currentChapterUrl) } just Runs

        // When
        novelRepository.updateBookmarkCurrentWebPageUrl(novelId, currentChapterUrl)

        // Then
        coVerify { novelDao.updateBookmarkCurrentWebPageUrl(novelId, currentChapterUrl) }
    }

    @Test
    fun `deleteNovel should delegate to dao`() = runTest {
        // Given
        val novelId = 123L
        coEvery { novelDao.deleteNovel(novelId) } just Runs

        // When
        novelRepository.deleteNovel(novelId)

        // Then
        coVerify { novelDao.deleteNovel(novelId) }
    }

    @Test
    fun `resetNovel should delegate to dao`() = runTest {
        // Given
        val novel = Novel("Test Novel", "https://test.com/novel", 1L)
        novel.id = 123L
        coEvery { novelDao.resetNovel(novel) } just Runs

        // When
        novelRepository.resetNovel(novel)

        // Then
        coVerify { novelDao.resetNovel(novel) }
    }

    @Test
    fun `withTransaction should execute block within transaction`() = runTest {
        // Given
        val mockDatabase = mockk<android.database.sqlite.SQLiteDatabase>(relaxed = true)
        every { dbHelper.writableDatabase } returns mockDatabase
        var blockExecuted = false

        // When
        novelRepository.withTransaction {
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
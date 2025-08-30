package io.github.gmathi.novellibrary.database.dao.impl

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.dao.NovelDao
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class NovelDaoImplTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var novelDao: NovelDao

    private lateinit var mockDbHelper: DBHelper
    private lateinit var mockSourceManager: SourceManager

    @Before
    fun setup() {
        hiltRule.inject()
        
        mockDbHelper = mockk(relaxed = true)
        mockSourceManager = mockk(relaxed = true)
    }

    @Test
    fun `novelDao should be injected properly via Hilt`() {
        // Then
        assert(::novelDao.isInitialized)
        assert(novelDao is NovelDaoImpl)
    }

    @Test
    fun `insertNovel should create novel and genres`() = runTest {
        // Given
        val novel = Novel("Test Novel", "https://test.com/novel", 1L).apply {
            genres = arrayListOf("Fantasy", "Adventure")
        }
        
        // Create a test instance with mocked dependencies
        val testNovelDao = NovelDaoImpl(mockDbHelper, mockSourceManager)
        
        coEvery { testNovelDao.getNovelId("https://test.com/novel") } returns -1L
        every { mockDbHelper.writableDatabase.insert(any(), any(), any()) } returns 123L
        coEvery { mockDbHelper.createGenre("Fantasy") } returns 1L
        coEvery { mockDbHelper.createGenre("Adventure") } returns 2L
        coEvery { mockDbHelper.createNovelGenre(any()) } returns 1L

        // When
        val result = testNovelDao.insertNovel(novel)

        // Then
        assert(result == 123L)
        coVerify { mockDbHelper.createGenre("Fantasy") }
        coVerify { mockDbHelper.createGenre("Adventure") }
    }

    @Test
    fun `getNovelByUrl should return novel with genres`() = runTest {
        // Given
        val novelUrl = "https://test.com/novel"
        val novel = Novel("Test Novel", novelUrl, 1L).apply {
            id = 123L
        }
        
        // Create a test instance with mocked dependencies
        val testNovelDao = NovelDaoImpl(mockDbHelper, mockSourceManager)
        
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        every { mockDbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns true
        every { mockCursor.getString(any()) } returns "Test Novel" andThen novelUrl
        every { mockCursor.getLong(any()) } returns 123L andThen 1L
        every { mockCursor.getColumnIndex(any()) } returns 0
        coEvery { mockDbHelper.getGenres(123L) } returns arrayListOf("Fantasy")

        // When
        val result = testNovelDao.getNovelByUrl(novelUrl)

        // Then
        assert(result != null)
        assert(result?.name == "Test Novel")
        assert(result?.url == novelUrl)
        assert(result?.id == 123L)
        coVerify { mockDbHelper.getGenres(123L) }
    }

    @Test
    fun `updateNovel should update novel data and genres`() = runTest {
        // Given
        val novel = Novel("Updated Novel", "https://test.com/novel", 1L).apply {
            id = 123L
            genres = arrayListOf("Fantasy", "Romance")
            metadata = hashMapOf("key" to "value")
        }
        
        // Create a test instance with mocked dependencies
        val testNovelDao = NovelDaoImpl(mockDbHelper, mockSourceManager)
        
        every { mockDbHelper.writableDatabase.update(any(), any(), any(), any()) } returns 1
        coEvery { mockDbHelper.createGenre("Fantasy") } returns 1L
        coEvery { mockDbHelper.createGenre("Romance") } returns 2L
        coEvery { mockDbHelper.createNovelGenre(any()) } returns 1L

        // When
        val result = testNovelDao.updateNovel(novel)

        // Then
        assert(result == 1L)
        coVerify { mockDbHelper.createGenre("Fantasy") }
        coVerify { mockDbHelper.createGenre("Romance") }
    }

    @Test
    fun `resetNovel should cleanup and get new novel details`() = runTest {
        // Given
        val novel = Novel("Test Novel", "https://test.com/novel", 1L).apply {
            id = 123L
            novelSectionId = 456L
            orderId = 789L
        }
        
        val newNovel = Novel("Updated Novel", "https://test.com/novel", 1L)
        val mockSource = mockk<io.github.gmathi.novellibrary.model.source.Source>(relaxed = true)
        
        // Create a test instance with mocked dependencies
        val testNovelDao = NovelDaoImpl(mockDbHelper, mockSourceManager)
        
        coEvery { mockDbHelper.cleanupNovelData(novel) } returns Unit
        coEvery { mockSourceManager.get(1L) } returns mockSource
        coEvery { mockSource.getNovelDetails(novel) } returns newNovel
        coEvery { testNovelDao.insertNovel(any()) } returns 124L

        // When
        testNovelDao.resetNovel(novel)

        // Then
        coVerify { mockDbHelper.cleanupNovelData(novel) }
        coVerify { mockSourceManager.get(1L) }
        coVerify { mockSource.getNovelDetails(novel) }
    }

    @Test
    fun `deleteNovel should remove novel from database`() = runTest {
        // Given
        val novelId = 123L
        
        // Create a test instance with mocked dependencies
        val testNovelDao = NovelDaoImpl(mockDbHelper, mockSourceManager)
        
        every { mockDbHelper.writableDatabase.delete(any(), any(), any()) } returns 1

        // When
        testNovelDao.deleteNovel(novelId)

        // Then
        coVerify { mockDbHelper.writableDatabase.delete(any(), any(), arrayOf("123")) }
    }
}
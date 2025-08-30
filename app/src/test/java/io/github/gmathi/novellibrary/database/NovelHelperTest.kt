package io.github.gmathi.novellibrary.database

import android.content.Context
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.mockk.coEvery
import io.mockk.coVerify
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
class NovelHelperTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var novelHelper: NovelHelper

    private lateinit var mockContext: Context
    private lateinit var mockSourceManager: SourceManager
    private lateinit var mockDbHelper: DBHelper

    @Before
    fun setup() {
        hiltRule.inject()
        
        mockContext = mockk(relaxed = true)
        mockSourceManager = mockk(relaxed = true)
        mockDbHelper = mockk(relaxed = true)
    }

    @Test
    fun `resetNovel should cleanup and insert new novel data`() = runTest {
        // Given
        val novel = Novel("Test Novel", "https://test.com/novel", 1L).apply {
            id = 123L
            novelSectionId = 456L
            orderId = 789L
        }
        
        val newNovel = Novel("Test Novel Updated", "https://test.com/novel", 1L).apply {
            id = 124L
        }
        
        val mockSource = mockk<io.github.gmathi.novellibrary.model.source.Source>(relaxed = true)
        
        coEvery { mockSourceManager.get(1L) } returns mockSource
        coEvery { mockSource.getNovelDetails(novel) } returns newNovel
        coEvery { mockDbHelper.cleanupNovelData(novel) } returns Unit
        coEvery { mockDbHelper.insertNovel(any()) } returns 124L

        // Create a test instance with mocked dependencies
        val testNovelHelper = NovelHelper(mockContext, mockSourceManager, mockDbHelper)

        // When
        testNovelHelper.resetNovel(novel)

        // Then
        coVerify { mockDbHelper.cleanupNovelData(novel) }
        coVerify { mockSourceManager.get(1L) }
        coVerify { mockSource.getNovelDetails(novel) }
        coVerify { 
            mockDbHelper.insertNovel(match { insertedNovel ->
                insertedNovel.novelSectionId == 456L && insertedNovel.orderId == 789L
            })
        }
    }

    @Test
    fun `resetNovel should handle null source gracefully`() = runTest {
        // Given
        val novel = Novel("Test Novel", "https://test.com/novel", 1L).apply {
            id = 123L
        }
        
        coEvery { mockSourceManager.get(1L) } returns null
        coEvery { mockDbHelper.cleanupNovelData(novel) } returns Unit

        // Create a test instance with mocked dependencies
        val testNovelHelper = NovelHelper(mockContext, mockSourceManager, mockDbHelper)

        // When
        testNovelHelper.resetNovel(novel)

        // Then
        coVerify { mockDbHelper.cleanupNovelData(novel) }
        coVerify { mockSourceManager.get(1L) }
        coVerify(exactly = 0) { mockDbHelper.insertNovel(any()) }
    }

    @Test
    fun `resetNovel should handle null novel details gracefully`() = runTest {
        // Given
        val novel = Novel("Test Novel", "https://test.com/novel", 1L).apply {
            id = 123L
        }
        
        val mockSource = mockk<io.github.gmathi.novellibrary.model.source.Source>(relaxed = true)
        
        coEvery { mockSourceManager.get(1L) } returns mockSource
        coEvery { mockSource.getNovelDetails(novel) } returns null
        coEvery { mockDbHelper.cleanupNovelData(novel) } returns Unit

        // Create a test instance with mocked dependencies
        val testNovelHelper = NovelHelper(mockContext, mockSourceManager, mockDbHelper)

        // When
        testNovelHelper.resetNovel(novel)

        // Then
        coVerify { mockDbHelper.cleanupNovelData(novel) }
        coVerify { mockSourceManager.get(1L) }
        coVerify { mockSource.getNovelDetails(novel) }
        coVerify(exactly = 0) { mockDbHelper.insertNovel(any()) }
    }

    @Test
    fun `novelHelper should be injected properly via Hilt`() {
        // Then
        assert(::novelHelper.isInitialized)
        assert(novelHelper is NovelHelper)
    }
}
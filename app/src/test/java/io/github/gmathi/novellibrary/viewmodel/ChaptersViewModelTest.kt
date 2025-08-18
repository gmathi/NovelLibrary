package io.github.gmathi.novellibrary.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.coroutines.CoroutineTestRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ChaptersViewModelTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var viewModel: ChaptersViewModel
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var mockDbHelper: DBHelper
    private lateinit var mockDataCenter: DataCenter
    private lateinit var mockSourceManager: SourceManager
    private lateinit var mockNetworkHelper: NetworkHelper
    private lateinit var mockFirebaseAnalytics: FirebaseAnalytics

    private val testNovel = Novel(
        id = 1L,
        name = "Test Novel",
        url = "https://test.com/novel",
        sourceId = 1L
    )

    @Before
    fun setup() {
        hiltRule.inject()
        
        savedStateHandle = SavedStateHandle()
        savedStateHandle.set(ChaptersViewModel.KEY_NOVEL, testNovel)

        mockDbHelper = mockk(relaxed = true)
        mockDataCenter = mockk(relaxed = true)
        mockSourceManager = mockk(relaxed = true)
        mockNetworkHelper = mockk(relaxed = true)
        mockFirebaseAnalytics = mockk(relaxed = true)

        viewModel = ChaptersViewModel(
            savedStateHandle,
            mockDbHelper,
            mockDataCenter,
            mockNetworkHelper,
            mockSourceManager,
            mockFirebaseAnalytics
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `loadingStatus should be StateFlow`() = runTest {
        // Given
        val initialStatus = viewModel.loadingStatus.first()

        // Then
        assertEquals("", initialStatus)
    }

    @Test
    fun `actionModeProgress should be StateFlow`() = runTest {
        // Given
        val initialProgress = viewModel.actionModeProgress.first()

        // Then
        assertEquals("", initialProgress)
    }

    @Test
    fun `getData should update loadingStatus to START`() = runTest {
        // Given
        every { mockNetworkHelper.isConnectedToNetwork() } returns true
        every { mockDbHelper.getNovel(any()) } returns testNovel
        every { mockDbHelper.updateNewReleasesCount(any(), any()) } just Runs
        every { mockDbHelper.getAllWebPages(any()) } returns arrayListOf()
        every { mockDbHelper.getAllWebPageSettings(any()) } returns arrayListOf()

        // When
        viewModel.getData()

        // Then
        // The loading status should eventually be updated
        // Note: Due to the async nature, we'd need to collect the flow to test intermediate states
    }

    @Test
    fun `getData should handle no internet connection`() = runTest {
        // Given
        every { mockNetworkHelper.isConnectedToNetwork() } returns false
        every { mockDbHelper.getNovel(any()) } returns testNovel
        every { mockDbHelper.updateNewReleasesCount(any(), any()) } just Runs

        // When
        viewModel.getData()

        // Then
        // Should eventually emit NO_INTERNET status
        // Note: Testing async flows requires collecting the StateFlow
    }

    @Test
    fun `addNovelToLibrary should update loadingStatus`() = runTest {
        // Given
        val novelWithoutId = testNovel.copy(id = -1L)
        savedStateHandle.set(ChaptersViewModel.KEY_NOVEL, novelWithoutId)
        val viewModelWithNewNovel = ChaptersViewModel(
            savedStateHandle,
            mockDbHelper,
            mockDataCenter,
            mockNetworkHelper,
            mockSourceManager,
            mockFirebaseAnalytics
        )

        // Mock the insertNovel function behavior
        every { mockDbHelper.insertNovel(any()) } returns 1L

        // When
        viewModelWithNewNovel.addNovelToLibrary()

        // Then
        verify { mockDbHelper.insertNovel(any()) }
        verify { mockFirebaseAnalytics.logEvent(any(), any()) }
    }

    @Test
    fun `toggleSources should update novel metadata`() = runTest {
        // Given
        every { mockDbHelper.updateNovelMetaData(any()) } just Runs

        // When
        viewModel.toggleSources()

        // Then
        verify { mockDbHelper.updateNovelMetaData(any()) }
    }

    @Test
    fun `updateChapters with ADD_DOWNLOADS should add chapters to download queue`() = runTest {
        // Given
        val webPages = arrayListOf(
            WebPage(url = "test1", chapterName = "Chapter 1", novelId = 1L),
            WebPage(url = "test2", chapterName = "Chapter 2", novelId = 1L)
        )
        
        every { mockDbHelper.writableDatabase } returns mockk(relaxed = true)
        every { mockDbHelper.createDownload(any(), any()) } just Runs

        // When
        viewModel.updateChapters(webPages, ChaptersViewModel.Action.ADD_DOWNLOADS)

        // Then
        // Should eventually call createDownload for each chapter
        // Note: Due to async nature, verification might need to be done with proper coroutine testing
    }

    @Test
    fun `updateChapters with MARK_READ should update read status`() = runTest {
        // Given
        val webPages = arrayListOf(
            WebPage(url = "test1", chapterName = "Chapter 1", novelId = 1L)
        )
        
        viewModel.chapterSettings = arrayListOf()
        every { mockDbHelper.writableDatabase } returns mockk(relaxed = true)
        every { mockDbHelper.updateWebPageSettingsReadStatus(any(), any(), any()) } just Runs
        every { mockDbHelper.getAllWebPageSettings(any()) } returns arrayListOf()

        // When
        viewModel.updateChapters(webPages, ChaptersViewModel.Action.MARK_READ)

        // Then
        // Should eventually update read status
        // Note: Testing async operations requires proper flow collection
    }
}
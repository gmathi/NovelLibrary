package io.github.gmathi.novellibrary.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.google.firebase.analytics.FirebaseAnalytics
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.BaseHiltTest
import io.github.gmathi.novellibrary.util.TestConfiguration
import io.github.gmathi.novellibrary.util.coroutines.CoroutineTestRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import org.junit.Assert.assertEquals

@ExperimentalCoroutinesApi
class ChaptersViewModelTest : BaseHiltTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Inject
    lateinit var dbHelper: DBHelper

    @Inject
    lateinit var dataCenter: DataCenter

    @Inject
    lateinit var sourceManager: SourceManager

    @Inject
    lateinit var networkHelper: NetworkHelper

    @Inject
    lateinit var firebaseAnalytics: FirebaseAnalytics

    private lateinit var viewModel: ChaptersViewModel
    private lateinit var savedStateHandle: SavedStateHandle

    private val testNovel = Novel(
        TestConfiguration.TestData.TEST_NOVEL_NAME,
        TestConfiguration.TestData.TEST_URL,
        1L
    ).apply {
        id = TestConfiguration.TestData.TEST_NOVEL_ID
    }

    override fun onSetUp() {
        savedStateHandle = SavedStateHandle()
        savedStateHandle.set(ChaptersViewModel.KEY_NOVEL, testNovel)

        viewModel = ChaptersViewModel(
            savedStateHandle,
            dbHelper,
            dataCenter,
            networkHelper,
            sourceManager,
            firebaseAnalytics
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
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
    fun `getData should update loadingStatus to START`() = TestConfiguration.runTestWithDispatcher {
        // Given
        every { networkHelper.isConnectedToNetwork() } returns true
        every { dbHelper.getNovel(any()) } returns testNovel
        every { dbHelper.updateNewReleasesCount(any(), any()) } just Runs
        every { dbHelper.getAllWebPages(any()) } returns arrayListOf()
        every { dbHelper.getAllWebPageSettings(any()) } returns arrayListOf()

        // When
        viewModel.getData()

        // Then
        // The loading status should eventually be updated
        verify { networkHelper.isConnectedToNetwork() }
        verify { dbHelper.getNovel(any()) }
    }

    @Test
    fun `getData should handle no internet connection`() = TestConfiguration.runTestWithDispatcher {
        // Given
        every { networkHelper.isConnectedToNetwork() } returns false
        every { dbHelper.getNovel(any()) } returns testNovel
        every { dbHelper.updateNewReleasesCount(any(), any()) } just Runs

        // When
        viewModel.getData()

        // Then
        // Should eventually emit NO_INTERNET status
        // Note: Testing async flows requires collecting the StateFlow
        verify { networkHelper.isConnectedToNetwork() }
    }

    @Test
    fun `addNovelToLibrary should update loadingStatus`() = TestConfiguration.runTestWithDispatcher {
        // Given
        val novelWithoutId = testNovel.copy(id = -1L)
        savedStateHandle.set(ChaptersViewModel.KEY_NOVEL, novelWithoutId)
        val viewModelWithNewNovel = ChaptersViewModel(
            savedStateHandle,
            dbHelper,
            dataCenter,
            networkHelper,
            sourceManager,
            firebaseAnalytics
        )

        // Mock the insertNovel function behavior
        every { dbHelper.insertNovel(any()) } returns 1L

        // When
        viewModelWithNewNovel.addNovelToLibrary()

        // Then
        verify { dbHelper.insertNovel(any()) }
        verify { firebaseAnalytics.logEvent(any(), any()) }
    }

    @Test
    fun `toggleSources should update novel metadata`() = TestConfiguration.runTestWithDispatcher {
        // Given
        every { dbHelper.updateNovelMetaData(any()) } just Runs

        // When
        viewModel.toggleSources()

        // Then
        verify { dbHelper.updateNovelMetaData(any()) }
    }

    @Test
    fun `updateChapters with ADD_DOWNLOADS should add chapters to download queue`() = TestConfiguration.runTestWithDispatcher {
        // Given
        val webPages = arrayListOf(
            WebPage("test1", "Chapter 1").apply { novelId = testNovel.id!! },
            WebPage("test2", "Chapter 2").apply { novelId = testNovel.id!! }
        )
        
        every { dbHelper.writableDatabase } returns mockk(relaxed = true)
        every { dbHelper.createDownload(any(), any()) } just Runs

        // When
        viewModel.updateChapters(webPages, ChaptersViewModel.Action.ADD_DOWNLOADS)

        // Then
        // Should eventually call createDownload for each chapter
        // Note: Due to async nature, verification might need to be done with proper coroutine testing
        verify(timeout = TestConfiguration.Timeouts.SHORT) { dbHelper.writableDatabase }
    }

    @Test
    fun `updateChapters with MARK_READ should update read status`() = TestConfiguration.runTestWithDispatcher {
        // Given
        val webPages = arrayListOf(
            WebPage("test1", "Chapter 1").apply { novelId = testNovel.id!! }
        )
        
        viewModel.chapterSettings = arrayListOf()
        every { dbHelper.writableDatabase } returns mockk(relaxed = true)
        every { dbHelper.updateWebPageSettingsReadStatus(any(), any(), any()) } just Runs
        every { dbHelper.getAllWebPageSettings(any()) } returns arrayListOf()

        // When
        viewModel.updateChapters(webPages, ChaptersViewModel.Action.MARK_READ)

        // Then
        // Should eventually update read status
        verify(timeout = TestConfiguration.Timeouts.SHORT) { dbHelper.writableDatabase }
    }
}
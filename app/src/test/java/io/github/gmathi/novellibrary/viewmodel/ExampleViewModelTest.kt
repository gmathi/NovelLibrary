package io.github.gmathi.novellibrary.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.UiState
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.coroutines.CoroutineTestRule
import io.github.gmathi.novellibrary.util.getOrAwaitValue
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ExampleViewModelTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var viewModel: ExampleViewModel
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var mockDbHelper: DBHelper
    private lateinit var mockDataCenter: DataCenter
    private lateinit var mockNetworkHelper: NetworkHelper
    private lateinit var mockFirebaseAnalytics: FirebaseAnalytics

    private val testNovels = listOf(
        Novel("Test Novel 1", "https://test1.com", 1L),
        Novel("Test Novel 2", "https://test2.com", 2L),
        Novel("Another Novel", "https://test3.com", 3L)
    )

    @Before
    fun setup() {
        hiltRule.inject()
        
        savedStateHandle = SavedStateHandle()
        mockDbHelper = mockk(relaxed = true)
        mockDataCenter = mockk(relaxed = true)
        mockNetworkHelper = mockk(relaxed = true)
        mockFirebaseAnalytics = mockk(relaxed = true)

        // Setup default mock behaviors
        every { mockNetworkHelper.isConnectedToNetwork() } returns true
        every { mockDbHelper.getAllNovels() } returns testNovels
        every { mockDataCenter.language } returns "en"

        viewModel = ExampleViewModel(
            savedStateHandle,
            mockDbHelper,
            mockDataCenter,
            mockNetworkHelper,
            mockFirebaseAnalytics
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `loadNovels should emit success state with novels`() = runTest {
        // When
        viewModel.loadNovels()

        // Then
        val result = viewModel.novels.getOrAwaitValue()
        assertTrue(result is UiState.Success)
        assertEquals(testNovels, (result as UiState.Success).data)
        
        verify { mockDbHelper.getAllNovels() }
        verify { mockFirebaseAnalytics.logEvent(any(), any()) }
    }

    @Test
    fun `loadNovels should emit error state on exception`() = runTest {
        // Given
        val errorMessage = "Database error"
        every { mockDbHelper.getAllNovels() } throws Exception(errorMessage)

        // When
        viewModel.loadNovels()

        // Then
        val result = viewModel.novels.getOrAwaitValue()
        assertTrue(result is UiState.Error)
        assertTrue((result as UiState.Error).message.contains(errorMessage))
        
        verify { mockFirebaseAnalytics.logEvent("error_occurred", any()) }
    }

    @Test
    fun `selectNovel should load novel details successfully`() = runTest {
        // Given
        val novelId = 1L
        val expectedNovel = testNovels.first { it.id == novelId }
        every { mockDbHelper.getNovel(novelId) } returns expectedNovel

        // When
        viewModel.selectNovel(novelId)

        // Then
        val result = viewModel.selectedNovel.getOrAwaitValue()
        assertTrue(result is UiState.Success)
        assertEquals(expectedNovel, (result as UiState.Success).data)
        
        verify { mockDbHelper.getNovel(novelId) }
        verify { mockFirebaseAnalytics.logEvent("novel_selected", any()) }
    }

    @Test
    fun `selectNovel should emit error when novel not found`() = runTest {
        // Given
        val novelId = 999L
        every { mockDbHelper.getNovel(novelId) } returns null

        // When
        viewModel.selectNovel(novelId)

        // Then
        val result = viewModel.selectedNovel.getOrAwaitValue()
        assertTrue(result is UiState.Error)
        assertEquals("Novel not found", (result as UiState.Error).message)
    }

    @Test
    fun `searchNovels should update search results`() = runTest {
        // Given
        val query = "Test"
        val searchResults = testNovels.filter { it.name.contains(query) }
        every { mockDbHelper.searchNovels(query) } returns searchResults

        // When
        viewModel.searchNovels(query)

        // Then
        val results = viewModel.searchResults.first()
        assertEquals(searchResults, results)
        
        val savedQuery = viewModel.searchQuery.getOrAwaitValue()
        assertEquals(query, savedQuery)
        
        verify { mockDbHelper.searchNovels(query) }
        verify { mockFirebaseAnalytics.logEvent("search_performed", any()) }
    }

    @Test
    fun `searchNovels should clear results for empty query`() = runTest {
        // Given
        val query = ""

        // When
        viewModel.searchNovels(query)

        // Then
        val results = viewModel.searchResults.first()
        assertTrue(results.isEmpty())
        
        verify(exactly = 0) { mockDbHelper.searchNovels(any()) }
    }

    @Test
    fun `refreshNovels should show error when no network`() = runTest {
        // Given
        every { mockNetworkHelper.isConnectedToNetwork() } returns false

        // When
        viewModel.refreshNovels()

        // Then
        val error = viewModel.error.getOrAwaitValue()
        assertEquals("No internet connection available", error)
    }

    @Test
    fun `refreshNovels should load novels when network available`() = runTest {
        // Given
        every { mockNetworkHelper.isConnectedToNetwork() } returns true

        // When
        viewModel.refreshNovels()

        // Then
        val result = viewModel.novels.getOrAwaitValue()
        assertTrue(result is UiState.Success)
        assertEquals(testNovels, (result as UiState.Success).data)
        
        verify { mockFirebaseAnalytics.logEvent("novels_refreshed", any()) }
    }

    @Test
    fun `toggleNovelFavorite should update novel metadata`() = runTest {
        // Given
        val novel = testNovels.first()
        every { mockDbHelper.updateNovelMetaData(any()) } just Runs

        // When
        viewModel.toggleNovelFavorite(novel)

        // Then
        verify { mockDbHelper.updateNovelMetaData(any()) }
        verify { mockFirebaseAnalytics.logEvent("novel_favorite_toggled", any()) }
    }

    @Test
    fun `clearSearch should reset search state`() = runTest {
        // Given - first perform a search
        viewModel.searchNovels("test")

        // When
        viewModel.clearSearch()

        // Then
        val query = viewModel.searchQuery.getOrAwaitValue()
        assertEquals("", query)
        
        val results = viewModel.searchResults.first()
        assertTrue(results.isEmpty())
    }

    @Test
    fun `getUserPreferences should return DataCenter values`() {
        // When
        val preferences = viewModel.getUserPreferences()

        // Then
        assertEquals("dark", preferences["theme"])
        assertEquals("en", preferences["language"])
        assertEquals(true, preferences["auto_backup"])
        assertEquals(false, preferences["sync_enabled"])
    }

    @Test
    fun `isLoading should be false initially`() = runTest {
        // Then
        val isLoading = viewModel.isLoading.first()
        assertEquals(false, isLoading)
    }

    @Test
    fun `error should be null initially`() = runTest {
        // Then
        val error = viewModel.error.first()
        assertEquals(null, error)
    }

    @Test
    fun `viewModel should handle SavedStateHandle correctly`() = runTest {
        // Given
        val novelId = 42L
        val query = "saved query"

        // When
        viewModel.selectNovel(novelId)
        viewModel.searchNovels(query)

        // Then
        assertEquals(query, viewModel.searchQuery.getOrAwaitValue())
        // The novel ID should be saved in SavedStateHandle (internal state)
    }
}
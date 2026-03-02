package io.github.gmathi.novellibrary.settings.viewmodel

import io.github.gmathi.novellibrary.settings.data.repository.SettingsRepositoryDataStore
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for ReaderSettingsViewModel.
 * 
 * Tests:
 * - State management for text and display settings
 * - State management for theme settings
 * - State management for scroll behavior settings
 * - Repository interactions
 * - Error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReaderSettingsViewModelTest {
    
    private lateinit var repository: SettingsRepositoryDataStore
    private lateinit var viewModel: ReaderSettingsViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        setupDefaultRepositoryFlows()
    }
    
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    private fun setupDefaultRepositoryFlows() {
        every { repository.textSize } returns flowOf(16)
        every { repository.fontPath } returns flowOf("")
        every { repository.limitImageWidth } returns flowOf(true)
        every { repository.dayModeBackgroundColor } returns flowOf(0xFFFFFFFF.toInt())
        every { repository.nightModeBackgroundColor } returns flowOf(0xFF000000.toInt())
        every { repository.dayModeTextColor } returns flowOf(0xFF000000.toInt())
        every { repository.nightModeTextColor } returns flowOf(0xFFFFFFFF.toInt())
        every { repository.keepTextColor } returns flowOf(false)
        every { repository.alternativeTextColors } returns flowOf(false)
        every { repository.readerMode } returns flowOf(true)
        every { repository.japSwipe } returns flowOf(false)
        every { repository.showReaderScroll } returns flowOf(true)
        every { repository.enableVolumeScroll } returns flowOf(false)
        every { repository.volumeScrollLength } returns flowOf(100)
        every { repository.keepScreenOn } returns flowOf(false)
        every { repository.enableImmersiveMode } returns flowOf(false)
        every { repository.showNavbarAtChapterEnd } returns flowOf(true)
        every { repository.enableAutoScroll } returns flowOf(false)
        every { repository.autoScrollLength } returns flowOf(100)
        every { repository.autoScrollInterval } returns flowOf(1000)
        every { repository.showChapterComments } returns flowOf(true)
        every { repository.enableClusterPages } returns flowOf(false)
        every { repository.enableDirectionalLinks } returns flowOf(true)
        every { repository.isReaderModeButtonVisible } returns flowOf(true)
    }
    
    @Test
    fun `textSize exposes repository flow`() = runTest {
        // Given
        every { repository.textSize } returns flowOf(20)
        
        // When
        viewModel = ReaderSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.textSize.value shouldBe 20
    }
    
    @Test
    fun `setTextSize calls repository`() = runTest {
        // Given
        viewModel = ReaderSettingsViewModel(repository)
        
        // When
        viewModel.setTextSize(18)
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setTextSize(18) }
    }
    
    @Test
    fun `fontPath exposes repository flow`() = runTest {
        // Given
        every { repository.fontPath } returns flowOf("/path/to/font")
        
        // When
        viewModel = ReaderSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.fontPath.value shouldBe "/path/to/font"
    }
    
    @Test
    fun `setFontPath calls repository`() = runTest {
        // Given
        viewModel = ReaderSettingsViewModel(repository)
        
        // When
        viewModel.setFontPath("/new/font/path")
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setFontPath("/new/font/path") }
    }
    
    @Test
    fun `dayModeBackgroundColor exposes repository flow`() = runTest {
        // Given
        val color = 0xFFEEEEEE.toInt()
        every { repository.dayModeBackgroundColor } returns flowOf(color)
        
        // When
        viewModel = ReaderSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.dayModeBackgroundColor.value shouldBe color
    }
    
    @Test
    fun `setDayModeBackgroundColor calls repository`() = runTest {
        // Given
        viewModel = ReaderSettingsViewModel(repository)
        val color = 0xFFCCCCCC.toInt()
        
        // When
        viewModel.setDayModeBackgroundColor(color)
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setDayModeBackgroundColor(color) }
    }
    
    @Test
    fun `readerMode exposes repository flow`() = runTest {
        // Given
        every { repository.readerMode } returns flowOf(false)
        
        // When
        viewModel = ReaderSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.readerMode.value shouldBe false
    }
    
    @Test
    fun `setReaderMode calls repository`() = runTest {
        // Given
        viewModel = ReaderSettingsViewModel(repository)
        
        // When
        viewModel.setReaderMode(true)
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setReaderMode(true) }
    }
    
    @Test
    fun `enableVolumeScroll exposes repository flow`() = runTest {
        // Given
        every { repository.enableVolumeScroll } returns flowOf(true)
        
        // When
        viewModel = ReaderSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.enableVolumeScroll.value shouldBe true
    }
    
    @Test
    fun `setEnableVolumeScroll calls repository`() = runTest {
        // Given
        viewModel = ReaderSettingsViewModel(repository)
        
        // When
        viewModel.setEnableVolumeScroll(true)
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setEnableVolumeScroll(true) }
    }
    
    @Test
    fun `volumeScrollLength exposes repository flow`() = runTest {
        // Given
        every { repository.volumeScrollLength } returns flowOf(200)
        
        // When
        viewModel = ReaderSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.volumeScrollLength.value shouldBe 200
    }
    
    @Test
    fun `setVolumeScrollLength calls repository`() = runTest {
        // Given
        viewModel = ReaderSettingsViewModel(repository)
        
        // When
        viewModel.setVolumeScrollLength(150)
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setVolumeScrollLength(150) }
    }
    
    @Test
    fun `enableAutoScroll exposes repository flow`() = runTest {
        // Given
        every { repository.enableAutoScroll } returns flowOf(true)
        
        // When
        viewModel = ReaderSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.enableAutoScroll.value shouldBe true
    }
    
    @Test
    fun `setEnableAutoScroll calls repository`() = runTest {
        // Given
        viewModel = ReaderSettingsViewModel(repository)
        
        // When
        viewModel.setEnableAutoScroll(true)
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setEnableAutoScroll(true) }
    }
    
    @Test
    fun `autoScrollInterval exposes repository flow`() = runTest {
        // Given
        every { repository.autoScrollInterval } returns flowOf(2000)
        
        // When
        viewModel = ReaderSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.autoScrollInterval.value shouldBe 2000
    }
    
    @Test
    fun `setAutoScrollInterval calls repository`() = runTest {
        // Given
        viewModel = ReaderSettingsViewModel(repository)
        
        // When
        viewModel.setAutoScrollInterval(1500)
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setAutoScrollInterval(1500) }
    }
    
    @Test
    fun `keepScreenOn exposes repository flow`() = runTest {
        // Given
        every { repository.keepScreenOn } returns flowOf(true)
        
        // When
        viewModel = ReaderSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.keepScreenOn.value shouldBe true
    }
    
    @Test
    fun `setKeepScreenOn calls repository`() = runTest {
        // Given
        viewModel = ReaderSettingsViewModel(repository)
        
        // When
        viewModel.setKeepScreenOn(true)
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setKeepScreenOn(true) }
    }
    
    @Test
    fun `all initial values are correct when repository returns defaults`() = runTest {
        // When
        viewModel = ReaderSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.textSize.value shouldBe 16
        viewModel.fontPath.value shouldBe ""
        viewModel.limitImageWidth.value shouldBe true
        viewModel.readerMode.value shouldBe true
        viewModel.japSwipe.value shouldBe false
        viewModel.enableVolumeScroll.value shouldBe false
        viewModel.volumeScrollLength.value shouldBe 100
        viewModel.keepScreenOn.value shouldBe false
        viewModel.enableAutoScroll.value shouldBe false
        viewModel.autoScrollInterval.value shouldBe 1000
    }
}

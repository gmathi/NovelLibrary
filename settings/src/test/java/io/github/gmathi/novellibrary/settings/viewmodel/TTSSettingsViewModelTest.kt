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
 * Unit tests for TTSSettingsViewModel.
 * 
 * Tests:
 * - State management for TTS settings
 * - Repository interactions
 * - Error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TTSSettingsViewModelTest {
    
    private lateinit var repository: SettingsRepositoryDataStore
    private lateinit var viewModel: TTSSettingsViewModel
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
        every { repository.readAloudNextChapter } returns flowOf(false)
        every { repository.enableScrollingText } returns flowOf(true)
    }
    
    @Test
    fun `readAloudNextChapter exposes repository flow`() = runTest {
        // Given
        every { repository.readAloudNextChapter } returns flowOf(true)
        
        // When
        viewModel = TTSSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.readAloudNextChapter.value shouldBe true
    }
    
    @Test
    fun `setReadAloudNextChapter calls repository`() = runTest {
        // Given
        viewModel = TTSSettingsViewModel(repository)
        
        // When
        viewModel.setReadAloudNextChapter(true)
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setReadAloudNextChapter(true) }
    }
    
    @Test
    fun `enableScrollingText exposes repository flow`() = runTest {
        // Given
        every { repository.enableScrollingText } returns flowOf(false)
        
        // When
        viewModel = TTSSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.enableScrollingText.value shouldBe false
    }
    
    @Test
    fun `setEnableScrollingText calls repository`() = runTest {
        // Given
        viewModel = TTSSettingsViewModel(repository)
        
        // When
        viewModel.setEnableScrollingText(false)
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setEnableScrollingText(false) }
    }
    
    @Test
    fun `all initial values are correct when repository returns defaults`() = runTest {
        // When
        viewModel = TTSSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.readAloudNextChapter.value shouldBe false
        viewModel.enableScrollingText.value shouldBe true
    }
    
    @Test
    fun `multiple settings can be updated independently`() = runTest {
        // Given
        viewModel = TTSSettingsViewModel(repository)
        
        // When
        viewModel.setReadAloudNextChapter(true)
        viewModel.setEnableScrollingText(false)
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setReadAloudNextChapter(true) }
        coVerify { repository.setEnableScrollingText(false) }
    }
}

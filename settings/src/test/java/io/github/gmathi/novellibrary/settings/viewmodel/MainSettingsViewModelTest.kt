package io.github.gmathi.novellibrary.settings.viewmodel

import app.cash.turbine.test
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
 * Unit tests for MainSettingsViewModel.
 * 
 * Tests:
 * - State management
 * - Repository interactions
 * - Error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainSettingsViewModelTest {
    
    private lateinit var repository: SettingsRepositoryDataStore
    private lateinit var viewModel: MainSettingsViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
    }
    
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `isDarkTheme exposes repository flow`() = runTest(testDispatcher) {
        // Given
        every { repository.isDarkTheme } returns flowOf(true)
        every { repository.isDeveloper } returns flowOf(false)
        
        // When
        viewModel = MainSettingsViewModel(repository)
        
        // Then
        viewModel.isDarkTheme.test {
            advanceUntilIdle()
            awaitItem() shouldBe true
        }
    }
    
    @Test
    fun `isDeveloper exposes repository flow`() = runTest(testDispatcher) {
        // Given
        every { repository.isDarkTheme } returns flowOf(false)
        every { repository.isDeveloper } returns flowOf(true)
        
        // When
        viewModel = MainSettingsViewModel(repository)
        
        // Then
        viewModel.isDeveloper.test {
            advanceUntilIdle()
            awaitItem() shouldBe true
        }
    }
    
    @Test
    fun `setDarkTheme calls repository`() = runTest(testDispatcher) {
        // Given
        every { repository.isDarkTheme } returns flowOf(false)
        every { repository.isDeveloper } returns flowOf(false)
        viewModel = MainSettingsViewModel(repository)
        
        // When
        viewModel.setDarkTheme(true)
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setIsDarkTheme(true) }
    }
    
    @Test
    fun `initial values are correct when repository returns defaults`() = runTest(testDispatcher) {
        // Given
        every { repository.isDarkTheme } returns flowOf(false)
        every { repository.isDeveloper } returns flowOf(false)
        
        // When
        viewModel = MainSettingsViewModel(repository)
        
        // Then
        viewModel.isDarkTheme.test {
            advanceUntilIdle()
            awaitItem() shouldBe false
        }
        viewModel.isDeveloper.test {
            advanceUntilIdle()
            awaitItem() shouldBe false
        }
    }
}

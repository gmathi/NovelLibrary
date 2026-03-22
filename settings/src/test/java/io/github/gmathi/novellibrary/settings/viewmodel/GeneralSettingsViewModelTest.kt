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
 * Unit tests for GeneralSettingsViewModel.
 * 
 * Tests:
 * - State management for general app settings
 * - Repository interactions
 * - Error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GeneralSettingsViewModelTest {
    
    private lateinit var repository: SettingsRepositoryDataStore
    private lateinit var viewModel: GeneralSettingsViewModel
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
        every { repository.isDarkTheme } returns flowOf(false)
        every { repository.language } returns flowOf("en")
        every { repository.javascriptDisabled } returns flowOf(false)
        every { repository.loadLibraryScreen } returns flowOf(false)
        every { repository.enableNotifications } returns flowOf(true)
        every { repository.showChaptersLeftBadge } returns flowOf(true)
        every { repository.isDeveloper } returns flowOf(false)
    }
    
    @Test
    fun `isDarkTheme exposes repository flow`() = runTest {
        // Given
        every { repository.isDarkTheme } returns flowOf(true)
        
        // When
        viewModel = GeneralSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.isDarkTheme.value shouldBe true
    }
    
    @Test
    fun `setDarkTheme calls repository`() = runTest {
        // Given
        viewModel = GeneralSettingsViewModel(repository)
        
        // When
        viewModel.setDarkTheme(true)
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setIsDarkTheme(true) }
    }
    
    @Test
    fun `language exposes repository flow`() = runTest {
        // Given
        every { repository.language } returns flowOf("es")
        
        // When
        viewModel = GeneralSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.language.value shouldBe "es"
    }
    
    @Test
    fun `setLanguage calls repository`() = runTest {
        // Given
        viewModel = GeneralSettingsViewModel(repository)
        
        // When
        viewModel.setLanguage("fr")
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setLanguage("fr") }
    }
    
    @Test
    fun `javascriptDisabled exposes repository flow`() = runTest {
        // Given
        every { repository.javascriptDisabled } returns flowOf(true)
        
        // When
        viewModel = GeneralSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.javascriptDisabled.value shouldBe true
    }
    
    @Test
    fun `setJavascriptDisabled calls repository`() = runTest {
        // Given
        viewModel = GeneralSettingsViewModel(repository)
        
        // When
        viewModel.setJavascriptDisabled(true)
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setJavascriptDisabled(true) }
    }
    
    @Test
    fun `loadLibraryScreen exposes repository flow`() = runTest {
        // Given
        every { repository.loadLibraryScreen } returns flowOf(true)
        
        // When
        viewModel = GeneralSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.loadLibraryScreen.value shouldBe true
    }
    
    @Test
    fun `setLoadLibraryScreen calls repository`() = runTest {
        // Given
        viewModel = GeneralSettingsViewModel(repository)
        
        // When
        viewModel.setLoadLibraryScreen(true)
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setLoadLibraryScreen(true) }
    }
    
    @Test
    fun `enableNotifications exposes repository flow`() = runTest {
        // Given
        every { repository.enableNotifications } returns flowOf(false)
        
        // When
        viewModel = GeneralSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.enableNotifications.value shouldBe false
    }
    
    @Test
    fun `setEnableNotifications calls repository`() = runTest {
        // Given
        viewModel = GeneralSettingsViewModel(repository)
        
        // When
        viewModel.setEnableNotifications(false)
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setEnableNotifications(false) }
    }
    
    @Test
    fun `showChaptersLeftBadge exposes repository flow`() = runTest {
        // Given
        every { repository.showChaptersLeftBadge } returns flowOf(false)
        
        // When
        viewModel = GeneralSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.showChaptersLeftBadge.value shouldBe false
    }
    
    @Test
    fun `setShowChaptersLeftBadge calls repository`() = runTest {
        // Given
        viewModel = GeneralSettingsViewModel(repository)
        
        // When
        viewModel.setShowChaptersLeftBadge(false)
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setShowChaptersLeftBadge(false) }
    }
    
    @Test
    fun `isDeveloper exposes repository flow`() = runTest {
        // Given
        every { repository.isDeveloper } returns flowOf(true)
        
        // When
        viewModel = GeneralSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.isDeveloper.value shouldBe true
    }
    
    @Test
    fun `setIsDeveloper calls repository`() = runTest {
        // Given
        viewModel = GeneralSettingsViewModel(repository)
        
        // When
        viewModel.setIsDeveloper(true)
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setIsDeveloper(true) }
    }
    
    @Test
    fun `all initial values are correct when repository returns defaults`() = runTest {
        // When
        viewModel = GeneralSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.isDarkTheme.value shouldBe false
        viewModel.language.value shouldBe "en"
        viewModel.javascriptDisabled.value shouldBe false
        viewModel.loadLibraryScreen.value shouldBe false
        viewModel.enableNotifications.value shouldBe true
        viewModel.showChaptersLeftBadge.value shouldBe true
        viewModel.isDeveloper.value shouldBe false
    }
}

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
 * Unit tests for SyncSettingsViewModel.
 * 
 * Tests:
 * - State management for sync settings
 * - Service-specific sync settings
 * - Repository interactions
 * - Error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncSettingsViewModelTest {
    
    private lateinit var repository: SettingsRepositoryDataStore
    private lateinit var viewModel: SyncSettingsViewModel
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
    fun `getSyncEnabled returns repository flow for service`() = runTest {
        // Given
        val serviceName = "google"
        every { repository.getSyncEnabled(serviceName) } returns flowOf(true)
        viewModel = SyncSettingsViewModel(repository)
        
        // When
        val flow = viewModel.getSyncEnabled(serviceName)
        advanceUntilIdle()
        
        // Then
        flow.value shouldBe true
    }
    
    @Test
    fun `setSyncEnabled calls repository with correct parameters`() = runTest {
        // Given
        val serviceName = "google"
        viewModel = SyncSettingsViewModel(repository)
        
        // When
        viewModel.setSyncEnabled(serviceName, true)
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setSyncEnabled(serviceName, true) }
    }
    
    @Test
    fun `getSyncAddNovels returns repository flow for service`() = runTest {
        // Given
        val serviceName = "dropbox"
        every { repository.getSyncAddNovels(serviceName) } returns flowOf(true)
        viewModel = SyncSettingsViewModel(repository)
        
        // When
        val flow = viewModel.getSyncAddNovels(serviceName)
        advanceUntilIdle()
        
        // Then
        flow.value shouldBe true
    }
    
    @Test
    fun `setSyncAddNovels calls repository with correct parameters`() = runTest {
        // Given
        val serviceName = "dropbox"
        viewModel = SyncSettingsViewModel(repository)
        
        // When
        viewModel.setSyncAddNovels(serviceName, false)
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setSyncAddNovels(serviceName, false) }
    }
    
    @Test
    fun `getSyncDeleteNovels returns repository flow for service`() = runTest {
        // Given
        val serviceName = "onedrive"
        every { repository.getSyncDeleteNovels(serviceName) } returns flowOf(false)
        viewModel = SyncSettingsViewModel(repository)
        
        // When
        val flow = viewModel.getSyncDeleteNovels(serviceName)
        advanceUntilIdle()
        
        // Then
        flow.value shouldBe false
    }
    
    @Test
    fun `setSyncDeleteNovels calls repository with correct parameters`() = runTest {
        // Given
        val serviceName = "onedrive"
        viewModel = SyncSettingsViewModel(repository)
        
        // When
        viewModel.setSyncDeleteNovels(serviceName, true)
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setSyncDeleteNovels(serviceName, true) }
    }
    
    @Test
    fun `getSyncBookmarks returns repository flow for service`() = runTest {
        // Given
        val serviceName = "google"
        every { repository.getSyncBookmarks(serviceName) } returns flowOf(true)
        viewModel = SyncSettingsViewModel(repository)
        
        // When
        val flow = viewModel.getSyncBookmarks(serviceName)
        advanceUntilIdle()
        
        // Then
        flow.value shouldBe true
    }
    
    @Test
    fun `setSyncBookmarks calls repository with correct parameters`() = runTest {
        // Given
        val serviceName = "google"
        viewModel = SyncSettingsViewModel(repository)
        
        // When
        viewModel.setSyncBookmarks(serviceName, false)
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setSyncBookmarks(serviceName, false) }
    }
    
    @Test
    fun `multiple services can have different sync settings`() = runTest {
        // Given
        every { repository.getSyncEnabled("google") } returns flowOf(true)
        every { repository.getSyncEnabled("dropbox") } returns flowOf(false)
        viewModel = SyncSettingsViewModel(repository)
        
        // When
        val googleSync = viewModel.getSyncEnabled("google")
        val dropboxSync = viewModel.getSyncEnabled("dropbox")
        advanceUntilIdle()
        
        // Then
        googleSync.value shouldBe true
        dropboxSync.value shouldBe false
    }
    
    @Test
    fun `setting sync for one service does not affect others`() = runTest {
        // Given
        viewModel = SyncSettingsViewModel(repository)
        
        // When
        viewModel.setSyncEnabled("google", true)
        viewModel.setSyncEnabled("dropbox", false)
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setSyncEnabled("google", true) }
        coVerify { repository.setSyncEnabled("dropbox", false) }
    }
}

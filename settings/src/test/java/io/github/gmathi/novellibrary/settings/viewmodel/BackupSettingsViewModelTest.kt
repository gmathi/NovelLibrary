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
 * Unit tests for BackupSettingsViewModel.
 * 
 * Tests:
 * - State management for backup settings
 * - State management for Google Drive backup settings
 * - Repository interactions
 * - Error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BackupSettingsViewModelTest {
    
    private lateinit var repository: SettingsRepositoryDataStore
    private lateinit var viewModel: BackupSettingsViewModel
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
        every { repository.showBackupHint } returns flowOf(true)
        every { repository.showRestoreHint } returns flowOf(true)
        every { repository.backupFrequency } returns flowOf(24)
        every { repository.lastBackup } returns flowOf(0L)
        every { repository.lastLocalBackupTimestamp } returns flowOf("")
        every { repository.lastCloudBackupTimestamp } returns flowOf("")
        every { repository.lastBackupSize } returns flowOf("")
        every { repository.gdBackupInterval } returns flowOf("daily")
        every { repository.gdAccountEmail } returns flowOf("")
        every { repository.gdInternetType } returns flowOf("wifi")
    }
    
    @Test
    fun `showBackupHint exposes repository flow`() = runTest {
        // Given
        every { repository.showBackupHint } returns flowOf(false)
        
        // When
        viewModel = BackupSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.showBackupHint.value shouldBe false
    }
    
    @Test
    fun `setShowBackupHint calls repository`() = runTest {
        // Given
        viewModel = BackupSettingsViewModel(repository)
        
        // When
        viewModel.setShowBackupHint(false)
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setShowBackupHint(false) }
    }
    
    @Test
    fun `showRestoreHint exposes repository flow`() = runTest {
        // Given
        every { repository.showRestoreHint } returns flowOf(false)
        
        // When
        viewModel = BackupSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.showRestoreHint.value shouldBe false
    }
    
    @Test
    fun `setShowRestoreHint calls repository`() = runTest {
        // Given
        viewModel = BackupSettingsViewModel(repository)
        
        // When
        viewModel.setShowRestoreHint(false)
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setShowRestoreHint(false) }
    }
    
    @Test
    fun `backupFrequency exposes repository flow`() = runTest {
        // Given
        every { repository.backupFrequency } returns flowOf(48)
        
        // When
        viewModel = BackupSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.backupFrequency.value shouldBe 48
    }
    
    @Test
    fun `setBackupFrequency calls repository`() = runTest {
        // Given
        viewModel = BackupSettingsViewModel(repository)
        
        // When
        viewModel.setBackupFrequency(12)
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setBackupFrequency(12) }
    }
    
    @Test
    fun `lastBackup exposes repository flow`() = runTest {
        // Given
        val timestamp = 1234567890L
        every { repository.lastBackup } returns flowOf(timestamp)
        
        // When
        viewModel = BackupSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.lastBackup.value shouldBe timestamp
    }
    
    @Test
    fun `setLastBackup calls repository`() = runTest {
        // Given
        viewModel = BackupSettingsViewModel(repository)
        val timestamp = 9876543210L
        
        // When
        viewModel.setLastBackup(timestamp)
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setLastBackup(timestamp) }
    }
    
    @Test
    fun `lastLocalBackupTimestamp exposes repository flow`() = runTest {
        // Given
        every { repository.lastLocalBackupTimestamp } returns flowOf("2024-01-15 10:30:00")
        
        // When
        viewModel = BackupSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.lastLocalBackupTimestamp.value shouldBe "2024-01-15 10:30:00"
    }
    
    @Test
    fun `setLastLocalBackupTimestamp calls repository`() = runTest {
        // Given
        viewModel = BackupSettingsViewModel(repository)
        
        // When
        viewModel.setLastLocalBackupTimestamp("2024-01-16 11:00:00")
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setLastLocalBackupTimestamp("2024-01-16 11:00:00") }
    }
    
    @Test
    fun `lastCloudBackupTimestamp exposes repository flow`() = runTest {
        // Given
        every { repository.lastCloudBackupTimestamp } returns flowOf("2024-01-15 12:00:00")
        
        // When
        viewModel = BackupSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.lastCloudBackupTimestamp.value shouldBe "2024-01-15 12:00:00"
    }
    
    @Test
    fun `setLastCloudBackupTimestamp calls repository`() = runTest {
        // Given
        viewModel = BackupSettingsViewModel(repository)
        
        // When
        viewModel.setLastCloudBackupTimestamp("2024-01-16 13:00:00")
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setLastCloudBackupTimestamp("2024-01-16 13:00:00") }
    }
    
    @Test
    fun `lastBackupSize exposes repository flow`() = runTest {
        // Given
        every { repository.lastBackupSize } returns flowOf("5.2 MB")
        
        // When
        viewModel = BackupSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.lastBackupSize.value shouldBe "5.2 MB"
    }
    
    @Test
    fun `setLastBackupSize calls repository`() = runTest {
        // Given
        viewModel = BackupSettingsViewModel(repository)
        
        // When
        viewModel.setLastBackupSize("6.8 MB")
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setLastBackupSize("6.8 MB") }
    }
    
    @Test
    fun `gdBackupInterval exposes repository flow`() = runTest {
        // Given
        every { repository.gdBackupInterval } returns flowOf("weekly")
        
        // When
        viewModel = BackupSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.gdBackupInterval.value shouldBe "weekly"
    }
    
    @Test
    fun `setGdBackupInterval calls repository`() = runTest {
        // Given
        viewModel = BackupSettingsViewModel(repository)
        
        // When
        viewModel.setGdBackupInterval("monthly")
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setGdBackupInterval("monthly") }
    }
    
    @Test
    fun `gdAccountEmail exposes repository flow`() = runTest {
        // Given
        every { repository.gdAccountEmail } returns flowOf("user@example.com")
        
        // When
        viewModel = BackupSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.gdAccountEmail.value shouldBe "user@example.com"
    }
    
    @Test
    fun `setGdAccountEmail calls repository`() = runTest {
        // Given
        viewModel = BackupSettingsViewModel(repository)
        
        // When
        viewModel.setGdAccountEmail("newuser@example.com")
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setGdAccountEmail("newuser@example.com") }
    }
    
    @Test
    fun `gdInternetType exposes repository flow`() = runTest {
        // Given
        every { repository.gdInternetType } returns flowOf("any")
        
        // When
        viewModel = BackupSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.gdInternetType.value shouldBe "any"
    }
    
    @Test
    fun `setGdInternetType calls repository`() = runTest {
        // Given
        viewModel = BackupSettingsViewModel(repository)
        
        // When
        viewModel.setGdInternetType("wifi_only")
        advanceUntilIdle()
        
        // Then
        coVerify { repository.setGdInternetType("wifi_only") }
    }
    
    @Test
    fun `all initial values are correct when repository returns defaults`() = runTest {
        // When
        viewModel = BackupSettingsViewModel(repository)
        advanceUntilIdle()
        
        // Then
        viewModel.showBackupHint.value shouldBe true
        viewModel.showRestoreHint.value shouldBe true
        viewModel.backupFrequency.value shouldBe 24
        viewModel.lastBackup.value shouldBe 0L
        viewModel.lastLocalBackupTimestamp.value shouldBe ""
        viewModel.lastCloudBackupTimestamp.value shouldBe ""
        viewModel.lastBackupSize.value shouldBe ""
        viewModel.gdBackupInterval.value shouldBe "daily"
        viewModel.gdAccountEmail.value shouldBe ""
        viewModel.gdInternetType.value shouldBe "wifi"
    }
}

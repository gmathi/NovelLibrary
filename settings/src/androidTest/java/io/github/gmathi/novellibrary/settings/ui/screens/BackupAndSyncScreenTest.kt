package io.github.gmathi.novellibrary.settings.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.gmathi.novellibrary.settings.data.datastore.FakeSettingsDataStore
import io.github.gmathi.novellibrary.settings.data.repository.SettingsRepositoryDataStore
import io.github.gmathi.novellibrary.settings.viewmodel.BackupSettingsViewModel
import io.github.gmathi.novellibrary.settings.viewmodel.SyncSettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for BackupAndSyncScreen.
 * 
 * Tests:
 * - Verify tab navigation (Backup and Sync tabs)
 * - Test backup functionality (create, restore, Google Drive)
 * - Test sync functionality (enable, login, settings selection)
 * - Verify switches (auto-backup, sync enable, sync options)
 * - Test dropdowns (backup interval, network type)
 */
class BackupAndSyncScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createBackupViewModel(): BackupSettingsViewModel {
        val fakeDataStore = FakeSettingsDataStore()
        val repository = SettingsRepositoryDataStore(fakeDataStore)
        return BackupSettingsViewModel(repository)
    }

    private fun createSyncViewModel(): SyncSettingsViewModel {
        val fakeDataStore = FakeSettingsDataStore()
        val repository = SettingsRepositoryDataStore(fakeDataStore)
        return SyncSettingsViewModel(repository)
    }

    @Test
    fun backupAndSyncScreen_displaysTwoTabs() {
        // Given
        val backupViewModel = createBackupViewModel()
        val syncViewModel = createSyncViewModel()

        // When
        composeTestRule.setContent {
            BackupAndSyncScreen(
                backupViewModel = backupViewModel,
                syncViewModel = syncViewModel,
                onNavigateBack = {}
            )
        }

        // Then - Verify both tabs are displayed
        composeTestRule.onNodeWithText("Backup").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sync").assertIsDisplayed()
    }

    @Test
    fun backupAndSyncScreen_backupTab_isSelectedByDefault() {
        // Given
        val backupViewModel = createBackupViewModel()
        val syncViewModel = createSyncViewModel()

        // When
        composeTestRule.setContent {
            BackupAndSyncScreen(
                backupViewModel = backupViewModel,
                syncViewModel = syncViewModel,
                onNavigateBack = {}
            )
        }

        // Then - Backup tab content should be visible
        composeTestRule.onNodeWithText("Local Backup").assertIsDisplayed()
        composeTestRule.onNodeWithText("Google Drive Backup").assertIsDisplayed()
    }

    @Test
    fun backupAndSyncScreen_switchesToSyncTab() {
        // Given
        val backupViewModel = createBackupViewModel()
        val syncViewModel = createSyncViewModel()

        // When
        composeTestRule.setContent {
            BackupAndSyncScreen(
                backupViewModel = backupViewModel,
                syncViewModel = syncViewModel,
                onNavigateBack = {}
            )
        }

        // Click Sync tab
        composeTestRule.onAllNodesWithText("Sync")[0].performClick()
        composeTestRule.waitForIdle()

        // Then - Sync tab content should be visible
        composeTestRule.onNodeWithText("Sync Configuration").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enable Sync").assertIsDisplayed()
    }

    @Test
    fun backupAndSyncScreen_backupTab_displaysLocalBackupSection() {
        // Given
        val backupViewModel = createBackupViewModel()
        val syncViewModel = createSyncViewModel()

        // When
        composeTestRule.setContent {
            BackupAndSyncScreen(
                backupViewModel = backupViewModel,
                syncViewModel = syncViewModel,
                onNavigateBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Local Backup").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create Backup").assertIsDisplayed()
        composeTestRule.onNodeWithText("Restore Backup").assertIsDisplayed()
    }

    @Test
    fun backupAndSyncScreen_backupTab_createBackup_triggersCallback() {
        // Given
        val backupViewModel = createBackupViewModel()
        val syncViewModel = createSyncViewModel()
        var createBackupCalled = false

        // When
        composeTestRule.setContent {
            BackupAndSyncScreen(
                backupViewModel = backupViewModel,
                syncViewModel = syncViewModel,
                onNavigateBack = {},
                onCreateBackup = { createBackupCalled = true }
            )
        }

        // Click Create Backup
        composeTestRule.onNodeWithText("Create Backup").performClick()

        // Then
        assert(createBackupCalled) { "Create backup callback should be called" }
    }

    @Test
    fun backupAndSyncScreen_backupTab_restoreBackup_triggersCallback() {
        // Given
        val backupViewModel = createBackupViewModel()
        val syncViewModel = createSyncViewModel()
        var restoreBackupCalled = false

        // When
        composeTestRule.setContent {
            BackupAndSyncScreen(
                backupViewModel = backupViewModel,
                syncViewModel = syncViewModel,
                onNavigateBack = {},
                onRestoreBackup = { restoreBackupCalled = true }
            )
        }

        // Click Restore Backup
        composeTestRule.onNodeWithText("Restore Backup").performClick()

        // Then
        assert(restoreBackupCalled) { "Restore backup callback should be called" }
    }

    @Test
    fun backupAndSyncScreen_backupTab_displaysGoogleDriveSection() {
        // Given
        val backupViewModel = createBackupViewModel()
        val syncViewModel = createSyncViewModel()

        // When
        composeTestRule.setContent {
            BackupAndSyncScreen(
                backupViewModel = backupViewModel,
                syncViewModel = syncViewModel,
                onNavigateBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Google Drive Backup").assertIsDisplayed()
        composeTestRule.onNodeWithText("Backup Interval").assertIsDisplayed()
        composeTestRule.onNodeWithText("Network Type").assertIsDisplayed()
    }

    @Test
    fun backupAndSyncScreen_backupTab_backupIntervalDropdown_displaysOptions() {
        // Given
        val backupViewModel = createBackupViewModel()
        val syncViewModel = createSyncViewModel()

        // When
        composeTestRule.setContent {
            BackupAndSyncScreen(
                backupViewModel = backupViewModel,
                syncViewModel = syncViewModel,
                onNavigateBack = {}
            )
        }

        // Then - Backup interval should show current value
        composeTestRule.onNodeWithText("Backup Interval").assertIsDisplayed()
        // Default value should be displayed
        composeTestRule.onNode(
            hasText("Backup Interval") and hasAnyAncestor(hasText("Google Drive Backup"))
        ).assertExists()
    }

    @Test
    fun backupAndSyncScreen_syncTab_displaysEnableSyncSwitch() {
        // Given
        val backupViewModel = createBackupViewModel()
        val syncViewModel = createSyncViewModel()

        // When
        composeTestRule.setContent {
            BackupAndSyncScreen(
                backupViewModel = backupViewModel,
                syncViewModel = syncViewModel,
                onNavigateBack = {}
            )
        }

        // Switch to Sync tab
        composeTestRule.onAllNodesWithText("Sync")[0].performClick()
        composeTestRule.waitForIdle()

        // Then
        composeTestRule.onNodeWithText("Enable Sync").assertIsDisplayed()
        composeTestRule.onNodeWithText("Synchronize your library across devices").assertIsDisplayed()
    }

    @Test
    fun backupAndSyncScreen_syncTab_enableSyncSwitch_togglesState() {
        // Given
        val backupViewModel = createBackupViewModel()
        val syncViewModel = createSyncViewModel()

        // When
        composeTestRule.setContent {
            BackupAndSyncScreen(
                backupViewModel = backupViewModel,
                syncViewModel = syncViewModel,
                onNavigateBack = {}
            )
        }

        // Switch to Sync tab
        composeTestRule.onAllNodesWithText("Sync")[0].performClick()
        composeTestRule.waitForIdle()

        // Get initial state
        val initialState = runBlocking { syncViewModel.getSyncEnabled("default").first() }

        // Toggle switch
        composeTestRule.onNodeWithText("Enable Sync").performClick()
        composeTestRule.waitForIdle()

        // Then
        val updatedState = runBlocking { syncViewModel.getSyncEnabled("default").first() }
        assert(updatedState != initialState) { "Sync enabled should toggle" }
    }

    @Test
    fun backupAndSyncScreen_syncTab_displaysLoginButton() {
        // Given
        val backupViewModel = createBackupViewModel()
        val syncViewModel = createSyncViewModel()

        // When
        composeTestRule.setContent {
            BackupAndSyncScreen(
                backupViewModel = backupViewModel,
                syncViewModel = syncViewModel,
                onNavigateBack = {}
            )
        }

        // Switch to Sync tab
        composeTestRule.onAllNodesWithText("Sync")[0].performClick()
        composeTestRule.waitForIdle()

        // Then - Login button should be visible
        composeTestRule.onNodeWithText("Login to Sync").assertIsDisplayed()
    }

    @Test
    fun backupAndSyncScreen_syncTab_loginButton_triggersCallback() {
        // Given
        val backupViewModel = createBackupViewModel()
        val syncViewModel = createSyncViewModel()
        var loginCalled = false

        // When
        composeTestRule.setContent {
            BackupAndSyncScreen(
                backupViewModel = backupViewModel,
                syncViewModel = syncViewModel,
                onNavigateBack = {},
                onSyncLogin = { loginCalled = true }
            )
        }

        // Switch to Sync tab
        composeTestRule.onAllNodesWithText("Sync")[0].performClick()
        composeTestRule.waitForIdle()

        // Click login button
        composeTestRule.onNodeWithText("Login to Sync").performClick()

        // Then
        assert(loginCalled) { "Sync login callback should be called" }
    }

    @Test
    fun backupAndSyncScreen_syncTab_showsSyncOptionsWhenEnabled() {
        // Given
        val backupViewModel = createBackupViewModel()
        val syncViewModel = createSyncViewModel()

        // When
        composeTestRule.setContent {
            BackupAndSyncScreen(
                backupViewModel = backupViewModel,
                syncViewModel = syncViewModel,
                onNavigateBack = {}
            )
        }

        // Switch to Sync tab
        composeTestRule.onAllNodesWithText("Sync")[0].performClick()
        composeTestRule.waitForIdle()

        // Enable sync
        syncViewModel.setSyncEnabled("default", true)
        composeTestRule.waitForIdle()

        // Then - Sync options should be visible
        composeTestRule.onNodeWithText("What to Sync").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sync Added Novels").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sync Deleted Novels").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sync Bookmarks").assertIsDisplayed()
    }

    @Test
    fun backupAndSyncScreen_syncTab_syncAddNovelsSwitch_togglesState() {
        // Given
        val backupViewModel = createBackupViewModel()
        val syncViewModel = createSyncViewModel()

        // When
        composeTestRule.setContent {
            BackupAndSyncScreen(
                backupViewModel = backupViewModel,
                syncViewModel = syncViewModel,
                onNavigateBack = {}
            )
        }

        // Switch to Sync tab and enable sync
        composeTestRule.onAllNodesWithText("Sync")[0].performClick()
        composeTestRule.waitForIdle()
        syncViewModel.setSyncEnabled("default", true)
        composeTestRule.waitForIdle()

        // Get initial state
        val initialState = runBlocking { syncViewModel.getSyncAddNovels("default").first() }

        // Toggle switch
        composeTestRule.onNodeWithText("Sync Added Novels").performClick()
        composeTestRule.waitForIdle()

        // Then
        val updatedState = runBlocking { syncViewModel.getSyncAddNovels("default").first() }
        assert(updatedState != initialState) { "Sync add novels should toggle" }
    }

    @Test
    fun backupAndSyncScreen_navigatesBack() {
        // Given
        val backupViewModel = createBackupViewModel()
        val syncViewModel = createSyncViewModel()
        var navigatedBack = false

        // When
        composeTestRule.setContent {
            BackupAndSyncScreen(
                backupViewModel = backupViewModel,
                syncViewModel = syncViewModel,
                onNavigateBack = { navigatedBack = true }
            )
        }

        // Then
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        assert(navigatedBack) { "Should navigate back" }
    }
}

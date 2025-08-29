package io.github.gmathi.novellibrary.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.util.BaseHiltTest
import io.github.gmathi.novellibrary.util.TestConfiguration
import io.github.gmathi.novellibrary.util.coroutines.CoroutineTestRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

@ExperimentalCoroutinesApi
class GoogleBackupViewModelTest : BaseHiltTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Inject
    lateinit var dataCenter: DataCenter

    private lateinit var viewModel: GoogleBackupViewModel
    private lateinit var savedStateHandle: SavedStateHandle

    override fun onSetUp() {
        savedStateHandle = SavedStateHandle()

        // Configure mock DataCenter properties
        every { dataCenter.gdBackupInterval } returns "Daily"
        every { dataCenter.gdAccountEmail } returns "test@example.com"
        every { dataCenter.gdInternetType } returns "WiFi Only"
        every { dataCenter.lastCloudBackupTimestamp } returns "Never"

        viewModel = GoogleBackupViewModel(savedStateHandle, dataCenter)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `viewModel should be initialized with correct dependencies`() {
        // Then
        assertNotNull(viewModel)
        assertNotNull(viewModel.backupStatus)
        assertNotNull(viewModel.lastUpdatedTimestamp)
        assertNotNull(viewModel.backupInterval)
        assertNotNull(viewModel.googleAccountEmail)
        assertNotNull(viewModel.internetType)
    }

    @Test
    fun `getGoogleSettingsData should update LiveData with DataCenter values`() = TestConfiguration.runTestWithDispatcher {
        // When
        viewModel.onResume()

        // Then
        assertEquals("Daily", viewModel.backupInterval.value)
        assertEquals("test@example.com", viewModel.googleAccountEmail.value)
        assertEquals("WiFi Only", viewModel.internetType.value)
        verify { dataCenter.gdBackupInterval }
        verify { dataCenter.gdAccountEmail }
        verify { dataCenter.gdInternetType }
    }

    @Test
    fun `backup options should have default values`() {
        // Then
        assertEquals(false, viewModel.shouldBackupSimpleText)
        assertEquals(false, viewModel.shouldBackupDatabase)
        assertEquals(false, viewModel.shouldBackupPreferences)
        assertEquals(false, viewModel.shouldBackupFiles)
    }

    @Test
    fun `onBackupIntervalClicked should not throw exception`() {
        // When & Then (should not throw)
        viewModel.onBackupIntervalClicked()
    }

    @Test
    fun `onGoogleAccountClicked should not throw exception`() {
        // When & Then (should not throw)
        viewModel.onGoogleAccountClicked()
    }

    @Test
    fun `onInternetTypeClicked should not throw exception`() {
        // When & Then (should not throw)
        viewModel.onInternetTypeClicked()
    }
}
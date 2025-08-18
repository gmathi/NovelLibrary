package io.github.gmathi.novellibrary.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.util.coroutines.CoroutineTestRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExperimentalCoroutinesApi
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class GoogleBackupViewModelTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var viewModel: GoogleBackupViewModel
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var mockDataCenter: DataCenter

    @Before
    fun setup() {
        hiltRule.inject()
        
        savedStateHandle = SavedStateHandle()
        mockDataCenter = mockk(relaxed = true)

        // Mock DataCenter properties
        every { mockDataCenter.gdBackupInterval } returns "Daily"
        every { mockDataCenter.gdAccountEmail } returns "test@example.com"
        every { mockDataCenter.gdInternetType } returns "WiFi Only"
        every { mockDataCenter.lastCloudBackupTimestamp } returns "Never"

        viewModel = GoogleBackupViewModel(savedStateHandle, mockDataCenter)
    }

    @After
    fun tearDown() {
        unmockkAll()
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
    fun `getGoogleSettingsData should update LiveData with DataCenter values`() = runTest {
        // When
        viewModel.onResume()

        // Then
        assertEquals("Daily", viewModel.backupInterval.value)
        assertEquals("test@example.com", viewModel.googleAccountEmail.value)
        assertEquals("WiFi Only", viewModel.internetType.value)
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
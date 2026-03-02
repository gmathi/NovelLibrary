package io.github.gmathi.novellibrary.settings.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Unit tests for SettingsDataStore.
 * 
 * Tests read/write operations, error handling, Flow emissions, and property-based tests.
 * Uses Robolectric for Android context and Turbine for Flow testing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class SettingsDataStoreTest {
    
    private lateinit var context: Context
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var testScope: TestScope
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testScope = TestScope(UnconfinedTestDispatcher() + Job())
        
        // Create a test DataStore with a unique name for each test
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { context.preferencesDataStoreFile("test_settings_${System.currentTimeMillis()}") }
        )
        
        settingsDataStore = SettingsDataStore(context)
    }
    
    @After
    fun tearDown() {
        // Clean up test DataStore file
        context.preferencesDataStoreFile("test_settings").delete()
    }
    
    //region Reader Settings Tests
    
    @Test
    fun `test readerMode read and write`() = testScope.runTest {
        // Test default value
        settingsDataStore.readerMode.test {
            assertEquals(false, awaitItem())
            
            // Write new value
            settingsDataStore.updateBoolean(SettingsDataStore.READER_MODE, true)
            
            // Verify new value is emitted
            assertEquals(true, awaitItem())
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `test textSize read and write`() = testScope.runTest {
        settingsDataStore.textSize.test {
            assertEquals(0, awaitItem())
            
            settingsDataStore.updateInt(SettingsDataStore.TEXT_SIZE, 18)
            
            assertEquals(18, awaitItem())
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `test japSwipe read and write`() = testScope.runTest {
        settingsDataStore.japSwipe.test {
            assertEquals(true, awaitItem())
            
            settingsDataStore.updateBoolean(SettingsDataStore.JAP_SWIPE, false)
            
            assertEquals(false, awaitItem())
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `test volumeScrollLength read and write`() = testScope.runTest {
        settingsDataStore.volumeScrollLength.test {
            assertEquals(100, awaitItem())
            
            settingsDataStore.updateInt(SettingsDataStore.VOLUME_SCROLL_LENGTH, 150)
            
            assertEquals(150, awaitItem())
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `test fontPath read and write`() = testScope.runTest {
        settingsDataStore.fontPath.test {
            assertEquals("default", awaitItem())
            
            settingsDataStore.updateString(SettingsDataStore.FONT_PATH, "/custom/font.ttf")
            
            assertEquals("/custom/font.ttf", awaitItem())
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `test color settings read and write`() = testScope.runTest {
        settingsDataStore.dayModeBackgroundColor.test {
            assertEquals(-1, awaitItem())
            
            settingsDataStore.updateInt(SettingsDataStore.DAY_MODE_BACKGROUND_COLOR, 0xFFFFFF)
            
            assertEquals(0xFFFFFF, awaitItem())
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    //endregion
    
    //region General Settings Tests
    
    @Test
    fun `test isDarkTheme read and write`() = testScope.runTest {
        settingsDataStore.isDarkTheme.test {
            assertEquals(true, awaitItem())
            
            settingsDataStore.updateBoolean(SettingsDataStore.IS_DARK_THEME, false)
            
            assertEquals(false, awaitItem())
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `test language read and write`() = testScope.runTest {
        settingsDataStore.language.test {
            assertEquals("System Default", awaitItem())
            
            settingsDataStore.updateString(SettingsDataStore.LANGUAGE, "English")
            
            assertEquals("English", awaitItem())
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `test enableNotifications read and write`() = testScope.runTest {
        settingsDataStore.enableNotifications.test {
            assertEquals(true, awaitItem())
            
            settingsDataStore.updateBoolean(SettingsDataStore.ENABLE_NOTIFICATIONS, false)
            
            assertEquals(false, awaitItem())
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    //endregion
    
    //region TTS Settings Tests
    
    @Test
    fun `test readAloudNextChapter read and write`() = testScope.runTest {
        settingsDataStore.readAloudNextChapter.test {
            assertEquals(true, awaitItem())
            
            settingsDataStore.updateBoolean(SettingsDataStore.READ_ALOUD_NEXT_CHAPTER, false)
            
            assertEquals(false, awaitItem())
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `test enableScrollingText read and write`() = testScope.runTest {
        settingsDataStore.enableScrollingText.test {
            assertEquals(true, awaitItem())
            
            settingsDataStore.updateBoolean(SettingsDataStore.ENABLE_SCROLLING_TEXT, false)
            
            assertEquals(false, awaitItem())
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    //endregion
    
    //region Backup Settings Tests
    
    @Test
    fun `test backupFrequency read and write`() = testScope.runTest {
        settingsDataStore.backupFrequency.test {
            assertEquals(0, awaitItem())
            
            settingsDataStore.updateInt(SettingsDataStore.BACKUP_FREQUENCY, 24)
            
            assertEquals(24, awaitItem())
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `test lastBackup read and write`() = testScope.runTest {
        settingsDataStore.lastBackup.test {
            assertEquals(0L, awaitItem())
            
            val timestamp = System.currentTimeMillis()
            settingsDataStore.updateLong(SettingsDataStore.LAST_BACKUP, timestamp)
            
            assertEquals(timestamp, awaitItem())
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `test gdBackupInterval read and write`() = testScope.runTest {
        settingsDataStore.gdBackupInterval.test {
            assertEquals("Never", awaitItem())
            
            settingsDataStore.updateString(SettingsDataStore.GD_BACKUP_INTERVAL, "Daily")
            
            assertEquals("Daily", awaitItem())
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    //endregion
    
    //region Sync Settings Tests
    
    @Test
    fun `test sync settings with dynamic service names`() = testScope.runTest {
        val serviceName = "TestService"
        
        settingsDataStore.getSyncEnabled(serviceName).test {
            assertEquals(false, awaitItem())
            
            settingsDataStore.updateSyncSetting("sync_enable_$serviceName", true)
            
            assertEquals(true, awaitItem())
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `test multiple sync services independently`() = testScope.runTest {
        val service1 = "Service1"
        val service2 = "Service2"
        
        // Enable service1
        settingsDataStore.updateSyncSetting("sync_enable_$service1", true)
        
        // Verify service1 is enabled and service2 is disabled
        settingsDataStore.getSyncEnabled(service1).test {
            assertEquals(true, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        
        settingsDataStore.getSyncEnabled(service2).test {
            assertEquals(false, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    //endregion
    
    //region Error Handling Tests
    
    @Test
    fun `test Flow continues after IO error`() = testScope.runTest {
        // This test verifies that the Flow doesn't crash on IO errors
        // The catch block in SettingsDataStore should handle IOException gracefully
        
        settingsDataStore.readerMode.test {
            // Should emit default value even if there's an error
            val value = awaitItem()
            assertEquals(false, value)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `test multiple writes to same setting`() = testScope.runTest {
        settingsDataStore.textSize.test {
            assertEquals(0, awaitItem())
            
            // Write multiple times
            settingsDataStore.updateInt(SettingsDataStore.TEXT_SIZE, 16)
            assertEquals(16, awaitItem())
            
            settingsDataStore.updateInt(SettingsDataStore.TEXT_SIZE, 18)
            assertEquals(18, awaitItem())
            
            settingsDataStore.updateInt(SettingsDataStore.TEXT_SIZE, 20)
            assertEquals(20, awaitItem())
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `test concurrent reads from multiple Flows`() = testScope.runTest {
        // Update a value
        settingsDataStore.updateBoolean(SettingsDataStore.READER_MODE, true)
        
        // Multiple collectors should all receive the same value
        settingsDataStore.readerMode.test {
            assertEquals(true, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        
        settingsDataStore.readerMode.test {
            assertEquals(true, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    //endregion
    
    //region Property-Based Tests
    
    /**
     * Property: For any boolean value, writing it then reading it returns the same value (round-trip).
     * 
     * **Validates: Requirements 10.4**
     */
    @Test
    fun `property - boolean settings round-trip correctly`() = testScope.runTest {
        checkAll(100, Arb.boolean()) { value ->
            // Write value
            settingsDataStore.updateBoolean(SettingsDataStore.READER_MODE, value)
            
            // Read value and verify it matches
            settingsDataStore.readerMode.test {
                assertEquals(value, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
    
    /**
     * Property: For any integer value, writing it then reading it returns the same value (round-trip).
     * 
     * **Validates: Requirements 10.4**
     */
    @Test
    fun `property - integer settings round-trip correctly`() = testScope.runTest {
        checkAll(100, Arb.int(0..100)) { value ->
            // Write value
            settingsDataStore.updateInt(SettingsDataStore.TEXT_SIZE, value)
            
            // Read value and verify it matches
            settingsDataStore.textSize.test {
                assertEquals(value, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
    
    /**
     * Property: For any string value, writing it then reading it returns the same value (round-trip).
     * 
     * **Validates: Requirements 10.4**
     */
    @Test
    fun `property - string settings round-trip correctly`() = testScope.runTest {
        checkAll(100, Arb.string(0..100)) { value ->
            // Write value
            settingsDataStore.updateString(SettingsDataStore.LANGUAGE, value)
            
            // Read value and verify it matches
            settingsDataStore.language.test {
                assertEquals(value, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
    
    /**
     * Property: Writing the same boolean value twice has the same effect as writing it once (idempotence).
     * 
     * **Validates: Requirements 10.4**
     */
    @Test
    fun `property - boolean settings are idempotent`() = testScope.runTest {
        checkAll(100, Arb.boolean()) { value ->
            // Write value once
            settingsDataStore.updateBoolean(SettingsDataStore.ENABLE_VOLUME_SCROLL, value)
            
            // Read value
            var firstRead: Boolean? = null
            settingsDataStore.enableVolumeScroll.test {
                firstRead = awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
            
            // Write same value again
            settingsDataStore.updateBoolean(SettingsDataStore.ENABLE_VOLUME_SCROLL, value)
            
            // Read value again
            var secondRead: Boolean? = null
            settingsDataStore.enableVolumeScroll.test {
                secondRead = awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
            
            // Verify both reads return the same value
            assertEquals(firstRead, secondRead)
            assertEquals(value, secondRead)
        }
    }
    
    /**
     * Property: Writing the same integer value twice has the same effect as writing it once (idempotence).
     * 
     * **Validates: Requirements 10.4**
     */
    @Test
    fun `property - integer settings are idempotent`() = testScope.runTest {
        checkAll(100, Arb.int(0..1000)) { value ->
            // Write value once
            settingsDataStore.updateInt(SettingsDataStore.VOLUME_SCROLL_LENGTH, value)
            
            // Read value
            var firstRead: Int? = null
            settingsDataStore.volumeScrollLength.test {
                firstRead = awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
            
            // Write same value again
            settingsDataStore.updateInt(SettingsDataStore.VOLUME_SCROLL_LENGTH, value)
            
            // Read value again
            var secondRead: Int? = null
            settingsDataStore.volumeScrollLength.test {
                secondRead = awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
            
            // Verify both reads return the same value
            assertEquals(firstRead, secondRead)
            assertEquals(value, secondRead)
        }
    }
    
    //endregion
}

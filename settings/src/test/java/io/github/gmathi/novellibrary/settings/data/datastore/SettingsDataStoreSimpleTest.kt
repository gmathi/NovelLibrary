package io.github.gmathi.novellibrary.settings.data.datastore

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first

/**
 * Simple unit tests for SettingsDataStore using Kotest.
 * 
 * These tests verify the basic structure and default values of the DataStore.
 * Full integration tests with actual DataStore operations are in the integration test suite.
 */
class SettingsDataStoreSimpleTest : FunSpec({
    
    test("SettingsDataStore preference keys should be defined correctly") {
        // Verify that all preference keys are properly defined
        SettingsDataStore.READER_MODE.name shouldBe "cleanPages"
        SettingsDataStore.TEXT_SIZE.name shouldBe "textSize"
        SettingsDataStore.JAP_SWIPE.name shouldBe "japSwipe"
        SettingsDataStore.SHOW_READER_SCROLL.name shouldBe "showReaderScroll"
        SettingsDataStore.ENABLE_VOLUME_SCROLL.name shouldBe "volumeScroll"
        SettingsDataStore.VOLUME_SCROLL_LENGTH.name shouldBe "scrollLength"
        SettingsDataStore.KEEP_SCREEN_ON.name shouldBe "keepScreenOn"
        SettingsDataStore.FONT_PATH.name shouldBe "fontPath"
    }
    
    test("General settings preference keys should be defined correctly") {
        SettingsDataStore.IS_DARK_THEME.name shouldBe "isDarkTheme"
        SettingsDataStore.LANGUAGE.name shouldBe "language"
        SettingsDataStore.JAVASCRIPT_DISABLED.name shouldBe "javascript"
        SettingsDataStore.LOAD_LIBRARY_SCREEN.name shouldBe "loadLibraryScreen"
        SettingsDataStore.ENABLE_NOTIFICATIONS.name shouldBe "enableNotifications"
        SettingsDataStore.IS_DEVELOPER.name shouldBe "developer"
    }
    
    test("TTS settings preference keys should be defined correctly") {
        SettingsDataStore.READ_ALOUD_NEXT_CHAPTER.name shouldBe "readAloudNextChapter"
        SettingsDataStore.ENABLE_SCROLLING_TEXT.name shouldBe "scrollingText"
    }
    
    test("Backup settings preference keys should be defined correctly") {
        SettingsDataStore.SHOW_BACKUP_HINT.name shouldBe "showBackupHint"
        SettingsDataStore.SHOW_RESTORE_HINT.name shouldBe "showRestoreHint"
        SettingsDataStore.BACKUP_FREQUENCY.name shouldBe "backupFrequencyHours"
        SettingsDataStore.LAST_BACKUP.name shouldBe "lastBackupMilliseconds"
        SettingsDataStore.GD_BACKUP_INTERVAL.name shouldBe "gdBackupInterval"
        SettingsDataStore.GD_ACCOUNT_EMAIL.name shouldBe "gdAccountEmail"
    }
    
    test("Color settings preference keys should be defined correctly") {
        SettingsDataStore.DAY_MODE_BACKGROUND_COLOR.name shouldBe "dayModeBackgroundColor"
        SettingsDataStore.NIGHT_MODE_BACKGROUND_COLOR.name shouldBe "nightModeBackgroundColor"
        SettingsDataStore.DAY_MODE_TEXT_COLOR.name shouldBe "dayModeTextColor"
        SettingsDataStore.NIGHT_MODE_TEXT_COLOR.name shouldBe "nightModeTextColor"
    }
    
    test("Auto scroll settings preference keys should be defined correctly") {
        SettingsDataStore.ENABLE_AUTO_SCROLL.name shouldBe "enableAutoScroll"
        SettingsDataStore.AUTO_SCROLL_LENGTH.name shouldBe "autoScrollLength"
        SettingsDataStore.AUTO_SCROLL_INTERVAL.name shouldBe "autoScrollInterval"
    }
})

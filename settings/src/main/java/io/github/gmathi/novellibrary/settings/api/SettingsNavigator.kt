package io.github.gmathi.novellibrary.settings.api

import android.content.Context
import android.content.Intent

/**
 * Public API for navigating to settings screens.
 * This is the primary interface between the app module and settings module.
 * Uses reflection to avoid compile-time dependencies on activity classes.
 */
object SettingsNavigator {
    
    private const val SETTINGS_PACKAGE = "io.github.gmathi.novellibrary.settings.activity"
    private const val READER_PACKAGE = "$SETTINGS_PACKAGE.reader"
    
    /**
     * Opens the main settings screen.
     */
    fun openMainSettings(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.MainSettingsActivity")
    }
    
    /**
     * Opens the general settings screen.
     */
    fun openGeneralSettings(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.GeneralSettingsActivity")
    }
    
    /**
     * Opens the backup settings screen.
     */
    fun openBackupSettings(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.BackupSettingsActivity")
    }
    
    /**
     * Opens the sync settings screen.
     */
    fun openSyncSettings(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.SyncSettingsActivity")
    }
    
    /**
     * Opens the sync login screen.
     */
    fun openSyncLogin(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.SyncLoginActivity")
    }
    
    /**
     * Opens the sync settings selection screen.
     */
    fun openSyncSettingsSelection(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.SyncSettingsSelectionActivity")
    }
    
    /**
     * Opens the Google backup screen.
     */
    fun openGoogleBackup(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.GoogleBackupActivity")
    }
    
    /**
     * Opens the TTS (Text-to-Speech) settings screen.
     */
    fun openTTSSettings(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.TTSSettingsActivity")
    }
    
    /**
     * Opens the language selection screen.
     */
    fun openLanguage(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.LanguageActivity")
    }
    
    /**
     * Opens the CloudFlare bypass settings screen.
     */
    fun openCloudFlareBypass(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.CloudFlareBypassActivity")
    }
    
    /**
     * Opens the mention settings screen.
     */
    fun openMentionSettings(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.MentionSettingsActivity")
    }
    
    /**
     * Opens the contributions screen.
     */
    fun openContributions(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.ContributionsActivity")
    }
    
    /**
     * Opens the copyright information screen.
     */
    fun openCopyright(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.CopyrightActivity")
    }
    
    /**
     * Opens the libraries used screen.
     */
    fun openLibrariesUsed(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.LibrariesUsedActivity")
    }
    
    /**
     * Opens the reader settings screen.
     */
    fun openReaderSettings(context: Context) {
        launchActivity(context, "$READER_PACKAGE.ReaderSettingsActivity")
    }
    
    /**
     * Opens the reader background settings screen.
     */
    fun openReaderBackgroundSettings(context: Context) {
        launchActivity(context, "$READER_PACKAGE.ReaderBackgroundSettingsActivity")
    }
    
    /**
     * Opens the scroll behaviour settings screen.
     */
    fun openScrollBehaviourSettings(context: Context) {
        launchActivity(context, "$READER_PACKAGE.ScrollBehaviourSettingsActivity")
    }
    
    /**
     * Opens the base settings screen (typically not called directly).
     */
    fun openBaseSettings(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.BaseSettingsActivity")
    }
    
    /**
     * Helper method to launch an activity using reflection.
     * This avoids compile-time dependencies on activity classes.
     */
    private fun launchActivity(context: Context, className: String) {
        try {
            val activityClass = Class.forName(className)
            val intent = Intent(context, activityClass)
            context.startActivity(intent)
        } catch (e: ClassNotFoundException) {
            throw IllegalStateException("Settings activity not found: $className", e)
        }
    }
}

package io.github.gmathi.novellibrary.settings.api

/**
 * Callback interface for settings module to communicate with the app module.
 * This interface allows settings activities to trigger app-level actions without
 * having direct compile-time dependencies on app module classes.
 * 
 * The app module should implement this interface and provide an instance to
 * settings activities that need to trigger these actions.
 */
interface SettingsCallbacks {
    
    /**
     * Called when the app theme has been changed.
     * The app should recreate activities or apply the new theme.
     */
    fun onThemeChanged()
    
    /**
     * Called when the app language has been changed.
     * The app should update the locale and recreate activities.
     */
    fun onLanguageChanged()
    
    /**
     * Called when the user requests to clear the app cache.
     * The app should clear cached data and notify the user.
     */
    fun onCacheClearRequested()
    
    /**
     * Called when the user requests to restart the app.
     * The app should restart itself completely.
     */
    fun onAppRestartRequested()
    
    /**
     * Called when reader settings have been changed.
     * The app should update reader configuration if a reader is currently active.
     */
    fun onReaderSettingsChanged()
    
    /**
     * Called when backup settings have been changed.
     * The app should update backup scheduling if applicable.
     */
    fun onBackupSettingsChanged()
    
    /**
     * Called when sync settings have been changed.
     * The app should update sync configuration and potentially trigger a sync.
     */
    fun onSyncSettingsChanged()
}

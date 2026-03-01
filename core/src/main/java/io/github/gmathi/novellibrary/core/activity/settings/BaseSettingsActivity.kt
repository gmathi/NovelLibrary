package io.github.gmathi.novellibrary.core.activity.settings

import android.os.Bundle
import io.github.gmathi.novellibrary.core.activity.BaseActivity

/**
 * Abstract base class for settings activities.
 * Extends BaseActivity with settings-specific abstractions.
 * Defines template methods for common settings patterns.
 */
abstract class BaseSettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSettingsRecyclerView()
    }

    /**
     * Get the list of settings items to display.
     * Subclasses must implement this to provide settings data.
     * 
     * @return List of settings items (generic type to avoid dependencies)
     */
    protected abstract fun getSettingsItems(): List<Any>

    /**
     * Setup the RecyclerView for displaying settings.
     * Subclasses must implement this to configure the RecyclerView with adapter and decorations.
     */
    protected abstract fun setupSettingsRecyclerView()

    /**
     * Handle click events on settings items.
     * Subclasses must implement this to respond to user interactions.
     * 
     * @param item The settings item that was clicked
     * @param position The position of the item in the list
     */
    protected abstract fun onSettingsItemClick(item: Any, position: Int)
}

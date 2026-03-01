package io.github.gmathi.novellibrary.core.activity

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.gmathi.novellibrary.core.system.DataAccessor

/**
 * Abstract base class for all activities in the application.
 * Implements DataAccessor interface with abstract properties that subclasses must provide.
 * Defines template methods for common activity lifecycle and edge-to-edge display setup.
 */
abstract class BaseActivity : AppCompatActivity(), DataAccessor {

    /**
     * Firebase Analytics instance - provided by concrete implementation through dependency injection.
     */
    abstract override val firebaseAnalytics: Any
    
    /**
     * Data center for preferences - provided by concrete implementation through dependency injection.
     */
    abstract override val dataCenter: Any
    
    /**
     * Database helper - provided by concrete implementation through dependency injection.
     */
    abstract override val dbHelper: Any
    
    /**
     * Source manager - provided by concrete implementation through dependency injection.
     */
    abstract override val sourceManager: Any
    
    /**
     * Network helper - provided by concrete implementation through dependency injection.
     */
    abstract override val networkHelper: Any
    
    /**
     * Returns the context for this activity.
     */
    override fun getContext(): Context? = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
    }

    override fun onContentChanged() {
        super.onContentChanged()
        applyWindowInsets()
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(getLocaleContext(newBase))
    }

    /**
     * Setup edge-to-edge display for this activity.
     * Subclasses must implement this to configure window insets behavior.
     */
    protected abstract fun setupEdgeToEdge()

    /**
     * Apply window insets to views in this activity.
     * Subclasses must implement this to handle system window insets.
     */
    protected abstract fun applyWindowInsets()

    /**
     * Get locale-aware context for this activity.
     * Subclasses must implement this to provide locale management.
     * 
     * @param context The base context to wrap with locale
     * @return Context with appropriate locale applied
     */
    protected abstract fun getLocaleContext(context: Context): Context
}

package io.github.gmathi.novellibrary.core.fragment

import android.content.Context
import androidx.fragment.app.Fragment
import io.github.gmathi.novellibrary.core.system.DataAccessor

/**
 * Abstract base class for all fragments in the application.
 * Implements DataAccessor interface with abstract properties that subclasses must provide.
 * Provides template methods for common fragment operations.
 */
abstract class BaseFragment : Fragment(), DataAccessor {

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
     * Returns the context for this fragment.
     * Subclasses can override if custom context handling is needed.
     */
    override fun getContext(): Context? = super.getContext()
}

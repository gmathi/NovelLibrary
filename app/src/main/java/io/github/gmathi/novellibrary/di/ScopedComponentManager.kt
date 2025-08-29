package io.github.gmathi.novellibrary.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.android.scopes.FragmentScoped
import dagger.hilt.android.scopes.ViewModelScoped
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages scoped components to prevent memory leaks in Hilt migration
 * 
 * Provides proper scoping for different component lifecycles and manages
 * weak references to prevent memory leaks.
 */
@Singleton
class ScopedComponentManager @Inject constructor() {
    
    private val activityComponents = mutableMapOf<String, WeakReference<Any>>()
    private val fragmentComponents = mutableMapOf<String, WeakReference<Any>>()
    private val viewModelComponents = mutableMapOf<String, WeakReference<Any>>()
    
    /**
     * Registers an activity-scoped component
     */
    fun registerActivityComponent(key: String, component: Any) {
        activityComponents[key] = WeakReference(component)
    }
    
    /**
     * Registers a fragment-scoped component
     */
    fun registerFragmentComponent(key: String, component: Any) {
        fragmentComponents[key] = WeakReference(component)
    }
    
    /**
     * Registers a ViewModel-scoped component
     */
    fun registerViewModelComponent(key: String, component: Any) {
        viewModelComponents[key] = WeakReference(component)
    }
    
    /**
     * Cleans up null weak references to prevent memory leaks
     */
    fun cleanupNullReferences() {
        activityComponents.entries.removeAll { it.value.get() == null }
        fragmentComponents.entries.removeAll { it.value.get() == null }
        viewModelComponents.entries.removeAll { it.value.get() == null }
    }
    
    /**
     * Gets memory usage statistics for scoped components
     */
    fun getComponentStats(): ComponentStats {
        cleanupNullReferences()
        return ComponentStats(
            activeActivityComponents = activityComponents.size,
            activeFragmentComponents = fragmentComponents.size,
            activeViewModelComponents = viewModelComponents.size
        )
    }
    
    data class ComponentStats(
        val activeActivityComponents: Int,
        val activeFragmentComponents: Int,
        val activeViewModelComponents: Int
    )
}

/**
 * Activity-scoped module for components that should live with activity lifecycle
 */
@Module
@InstallIn(ActivityComponent::class)
object ActivityScopedModule {
    
    @Provides
    @ActivityScoped
    fun provideActivityScopedManager(): ActivityScopedManager {
        return ActivityScopedManager()
    }
}

/**
 * Fragment-scoped module for components that should live with fragment lifecycle
 */
@Module
@InstallIn(FragmentComponent::class)
object FragmentScopedModule {
    
    @Provides
    @FragmentScoped
    fun provideFragmentScopedManager(): FragmentScopedManager {
        return FragmentScopedManager()
    }
}

/**
 * ViewModel-scoped module for components that should live with ViewModel lifecycle
 */
@Module
@InstallIn(ViewModelComponent::class)
object ViewModelScopedModule {
    
    @Provides
    @ViewModelScoped
    fun provideViewModelScopedManager(): ViewModelScopedManager {
        return ViewModelScopedManager()
    }
}

/**
 * Manager for activity-scoped resources
 */
class ActivityScopedManager {
    private val resources = mutableListOf<WeakReference<Any>>()
    
    fun addResource(resource: Any) {
        resources.add(WeakReference(resource))
    }
    
    fun cleanup() {
        resources.clear()
    }
}

/**
 * Manager for fragment-scoped resources
 */
class FragmentScopedManager {
    private val resources = mutableListOf<WeakReference<Any>>()
    
    fun addResource(resource: Any) {
        resources.add(WeakReference(resource))
    }
    
    fun cleanup() {
        resources.clear()
    }
}

/**
 * Manager for ViewModel-scoped resources
 */
class ViewModelScopedManager {
    private val resources = mutableListOf<WeakReference<Any>>()
    
    fun addResource(resource: Any) {
        resources.add(WeakReference(resource))
    }
    
    fun cleanup() {
        resources.clear()
    }
}
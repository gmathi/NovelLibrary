package io.github.gmathi.novellibrary.viewmodel

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import dagger.hilt.android.lifecycle.HiltViewModel

/**
 * Utility functions for ViewModel creation and management with Hilt.
 * 
 * These utilities provide convenient ways to create and manage ViewModels
 * in a Hilt-enabled application while maintaining type safety and proper scoping.
 */

/**
 * Extension function for Fragments to get Hilt ViewModels with type safety.
 * This is a convenience function that wraps the standard `by viewModels()` delegate
 * with additional type safety and documentation.
 * 
 * Usage:
 * ```kotlin
 * @AndroidEntryPoint
 * class MyFragment : Fragment() {
 *     private val viewModel: MyViewModel by hiltViewModels()
 * }
 * ```
 * 
 * @return Lazy delegate for the ViewModel
 */
inline fun <reified VM : ViewModel> Fragment.hiltViewModels(): Lazy<VM> {
    return viewModels()
}

/**
 * Custom ViewModelProvider.Factory for creating ViewModels with custom parameters.
 * This is useful when you need to pass additional parameters to ViewModels
 * that are not provided by Hilt's dependency injection.
 * 
 * Usage:
 * ```kotlin
 * class CustomViewModelFactory<T : ViewModel>(
 *     private val create: () -> T
 * ) : ViewModelProvider.Factory {
 *     override fun <T : ViewModel> create(modelClass: Class<T>): T {
 *         return create() as T
 *     }
 * }
 * ```
 */
class CustomViewModelFactory<T : ViewModel>(
    private val create: () -> T
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return create() as T
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return create() as T
    }
}

/**
 * Helper function to create a custom ViewModelProvider.Factory.
 * 
 * Usage:
 * ```kotlin
 * val factory = viewModelFactory { MyViewModel(customParam) }
 * val viewModel: MyViewModel by viewModels { factory }
 * ```
 * 
 * @param create Function to create the ViewModel instance
 * @return ViewModelProvider.Factory instance
 */
inline fun <reified T : ViewModel> viewModelFactory(
    crossinline create: () -> T
): ViewModelProvider.Factory {
    return CustomViewModelFactory { create() }
}

/**
 * Annotation to mark ViewModels that should be created with Hilt.
 * This is a documentation annotation to make it clear which ViewModels
 * are managed by Hilt's dependency injection.
 * 
 * Usage:
 * ```kotlin
 * @HiltViewModel
 * @HiltManaged
 * class MyViewModel @Inject constructor(
 *     private val repository: MyRepository
 * ) : BaseViewModel()
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class HiltManaged

/**
 * Validation function to ensure ViewModels are properly annotated with @HiltViewModel.
 * This is a compile-time check to help developers remember to add the annotation.
 * 
 * @param viewModelClass The ViewModel class to validate
 * @throws IllegalStateException if the ViewModel is not properly annotated
 */
inline fun <reified T : ViewModel> validateHiltViewModel(viewModelClass: Class<T>) {
    if (!viewModelClass.isAnnotationPresent(HiltViewModel::class.java)) {
        throw IllegalStateException(
            "ViewModel ${viewModelClass.simpleName} must be annotated with @HiltViewModel. " +
            "Add @HiltViewModel annotation to the class and ensure constructor parameters are injected with @Inject."
        )
    }
}

/**
 * Extension function to safely cast ViewModel to expected type with validation.
 * 
 * @param expectedClass The expected ViewModel class
 * @return The ViewModel cast to the expected type
 * @throws ClassCastException if the ViewModel is not of the expected type
 */
fun <T : ViewModel> ViewModel.safeCast(expectedClass: Class<T>): T {
    if (!expectedClass.isInstance(this)) {
        throw ClassCastException(
            "Expected ViewModel of type ${expectedClass.simpleName}, " +
            "but got ${this::class.java.simpleName}"
        )
    }
    return expectedClass.cast(this)
}
package io.github.gmathi.novellibrary.util

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.MockKAnnotations
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Base class for Hilt unit tests that provides common setup and configuration.
 * Extend this class for tests that need Hilt dependency injection.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [28],
    application = HiltTestApplication::class
)
abstract class BaseHiltTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    open fun setUp() {
        // Initialize MockK annotations
        MockKAnnotations.init(this, relaxUnitFun = true)
        
        // Initialize Hilt
        hiltRule.inject()
        
        // Additional setup can be overridden in subclasses
        onSetUp()
    }

    /**
     * Override this method in subclasses for additional setup.
     */
    protected open fun onSetUp() {
        // Default implementation does nothing
    }

    /**
     * Override this method in subclasses for cleanup.
     */
    protected open fun onTearDown() {
        // Default implementation does nothing
    }
}
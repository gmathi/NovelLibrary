package io.github.gmathi.novellibrary.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.MockKAnnotations
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

/**
 * Base class for Hilt Android instrumentation tests that provides common setup and configuration.
 * Extend this class for integration tests that need Hilt dependency injection on real Android devices.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
abstract class BaseHiltAndroidTest {

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
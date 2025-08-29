package io.github.gmathi.novellibrary.util

import dagger.hilt.android.testing.HiltAndroidRule
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.extension.ExtensionManager
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.mockk
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Utility class for Hilt testing that provides common mock configurations
 * and test setup functionality.
 */
object HiltTestUtils {

    /**
     * Creates a mock DBHelper with common test configurations.
     */
    fun createMockDBHelper(): DBHelper {
        return mockk<DBHelper>(relaxed = true) {
            // Add common mock behaviors here
        }
    }

    /**
     * Creates a mock DataCenter with common test configurations.
     */
    fun createMockDataCenter(): DataCenter {
        return mockk<DataCenter>(relaxed = true) {
            // Add common mock behaviors here
        }
    }

    /**
     * Creates a mock NetworkHelper with common test configurations.
     */
    fun createMockNetworkHelper(): NetworkHelper {
        return mockk<NetworkHelper>(relaxed = true) {
            // Add common mock behaviors here
        }
    }

    /**
     * Creates a mock SourceManager with common test configurations.
     */
    fun createMockSourceManager(): SourceManager {
        return mockk<SourceManager>(relaxed = true) {
            // Add common mock behaviors here
        }
    }

    /**
     * Creates a mock ExtensionManager with common test configurations.
     */
    fun createMockExtensionManager(): ExtensionManager {
        return mockk<ExtensionManager>(relaxed = true) {
            // Add common mock behaviors here
        }
    }

    /**
     * Clears all mocks to ensure clean state between tests.
     */
    fun clearMocks() {
        clearAllMocks()
    }
}

/**
 * Custom test rule that combines HiltAndroidRule with MockK initialization
 * and cleanup for comprehensive test setup.
 */
class HiltMockRule : TestRule {
    
    private val hiltRule = HiltAndroidRule(this)
    
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                // Initialize MockK
                MockKAnnotations.init(this@HiltMockRule)
                
                try {
                    // Initialize Hilt
                    hiltRule.inject()
                    
                    // Run the test
                    base.evaluate()
                } finally {
                    // Clean up mocks
                    HiltTestUtils.clearMocks()
                }
            }
        }
    }
    
    /**
     * Inject dependencies into the test class.
     */
    fun inject() {
        hiltRule.inject()
    }
}
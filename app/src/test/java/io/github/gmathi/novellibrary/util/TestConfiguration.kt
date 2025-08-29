package io.github.gmathi.novellibrary.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

/**
 * Configuration class for test setup and utilities.
 * Provides common test configurations and helper methods.
 */
@OptIn(ExperimentalCoroutinesApi::class)
object TestConfiguration {

    /**
     * Default test dispatcher for coroutine tests.
     */
    val testDispatcher: TestDispatcher = StandardTestDispatcher()

    /**
     * Default test scope for coroutine tests.
     */
    val testScope: TestScope = TestScope(testDispatcher)

    /**
     * Runs a test with the default test dispatcher and scope.
     */
    fun runTestWithDispatcher(testBody: suspend TestScope.() -> Unit) {
        runTest(testDispatcher) {
            testBody()
        }
    }

    /**
     * Common test timeouts in milliseconds.
     */
    object Timeouts {
        const val SHORT = 1000L
        const val MEDIUM = 5000L
        const val LONG = 10000L
    }

    /**
     * Common test data for consistent testing.
     */
    object TestData {
        const val TEST_NOVEL_ID = 12345L
        const val TEST_NOVEL_NAME = "Test Novel"
        const val TEST_CHAPTER_ID = 67890L
        const val TEST_CHAPTER_NAME = "Test Chapter"
        const val TEST_URL = "https://test.example.com"
    }

    /**
     * Mock configuration constants.
     */
    object MockConfig {
        const val RELAXED = true
        const val RELAXED_UNIT_FUN = true
    }
}
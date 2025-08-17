package io.github.gmathi.novellibrary.util.coroutines

import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Test suite for all coroutine infrastructure components.
 * Runs all coroutine-related unit tests together.
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    CoroutineScopesTest::class,
    CoroutineErrorHandlerTest::class,
    DispatcherProviderTest::class,
    CoroutineUtilsTest::class
)
class CoroutineInfrastructureTestSuite
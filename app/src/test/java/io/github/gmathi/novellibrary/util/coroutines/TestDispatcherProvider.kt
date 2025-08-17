package io.github.gmathi.novellibrary.util.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/**
 * Test implementation of DispatcherProvider for unit testing.
 * Uses TestDispatcher to control coroutine execution in tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestDispatcherProvider(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : DispatcherProvider {
    override val main: CoroutineDispatcher = testDispatcher
    override val io: CoroutineDispatcher = testDispatcher
    override val default: CoroutineDispatcher = testDispatcher
    override val unconfined: CoroutineDispatcher = testDispatcher
}
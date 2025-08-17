package io.github.gmathi.novellibrary.util.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for DispatcherProvider implementations.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DispatcherProviderTest {
    
    @Test
    fun `DefaultDispatcherProvider should provide correct dispatchers`() {
        val provider = DefaultDispatcherProvider()
        
        assertEquals("Should provide Main dispatcher", Dispatchers.Main, provider.main)
        assertEquals("Should provide IO dispatcher", Dispatchers.IO, provider.io)
        assertEquals("Should provide Default dispatcher", Dispatchers.Default, provider.default)
        assertEquals("Should provide Unconfined dispatcher", Dispatchers.Unconfined, provider.unconfined)
    }
    
    @Test
    fun `TestDispatcherProvider should provide test dispatchers`() {
        val testDispatcher = UnconfinedTestDispatcher()
        val provider = TestDispatcherProvider(testDispatcher)
        
        assertEquals("Should provide test dispatcher for main", testDispatcher, provider.main)
        assertEquals("Should provide test dispatcher for io", testDispatcher, provider.io)
        assertEquals("Should provide test dispatcher for default", testDispatcher, provider.default)
        assertEquals("Should provide test dispatcher for unconfined", testDispatcher, provider.unconfined)
    }
    
    @Test
    fun `TestDispatcherProvider should use UnconfinedTestDispatcher by default`() {
        val provider = TestDispatcherProvider()
        
        assertNotNull("Main dispatcher should not be null", provider.main)
        assertNotNull("IO dispatcher should not be null", provider.io)
        assertNotNull("Default dispatcher should not be null", provider.default)
        assertNotNull("Unconfined dispatcher should not be null", provider.unconfined)
        
        // All dispatchers should be the same instance
        assertSame("All dispatchers should be same instance", provider.main, provider.io)
        assertSame("All dispatchers should be same instance", provider.main, provider.default)
        assertSame("All dispatchers should be same instance", provider.main, provider.unconfined)
    }
}
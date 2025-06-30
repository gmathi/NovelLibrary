import io.github.gmathi.novellibrary.util.system.ObjectPool
import io.github.gmathi.novellibrary.util.system.useObject
import org.junit.Assert.*
import org.junit.Test

class ObjectPoolTest {
    
    @Test
    fun testAcquireCreatesNewObjectWhenPoolEmpty() {
        // Given
        var factoryCallCount = 0
        val pool = ObjectPool(
            factory = { 
                factoryCallCount++
                "Object$factoryCallCount"
            },
            reset = { /* no reset needed for strings */ },
            maxSize = 3
        )
        
        // When
        val obj1 = pool.acquire()
        val obj2 = pool.acquire()
        
        // Then
        assertEquals("Object1", obj1)
        assertEquals("Object2", obj2)
        assertEquals(2, factoryCallCount)
        assertEquals(0, pool.size())
    }
    
    @Test
    fun testReleaseAddsObjectToPool() {
        // Given
        var factoryCallCount = 0
        val pool = ObjectPool(
            factory = { 
                factoryCallCount++
                "Object$factoryCallCount"
            },
            reset = { /* no reset needed for strings */ },
            maxSize = 3
        )
        
        // When
        val obj1 = pool.acquire()
        pool.release(obj1)
        
        // Then
        assertEquals(1, pool.size())
        assertFalse(pool.isPoolEmpty())
    }
    
    @Test
    fun testAcquireReusesObjectFromPool() {
        // Given
        var factoryCallCount = 0
        val pool = ObjectPool(
            factory = { 
                factoryCallCount++
                "Object$factoryCallCount"
            },
            reset = { /* no reset needed for strings */ },
            maxSize = 3
        )
        
        // When
        val obj1 = pool.acquire()
        pool.release(obj1)
        val obj2 = pool.acquire()
        
        // Then
        assertEquals("Object1", obj1)
        assertEquals("Object1", obj2) // Same object reused
        assertEquals(1, factoryCallCount) // Factory called only once
        assertEquals(0, pool.size()) // Pool is empty after reuse
    }
    
    @Test
    fun testPoolDoesNotExceedMaxSize() {
        // Given
        var factoryCallCount = 0
        val pool = ObjectPool(
            factory = { 
                factoryCallCount++
                "Object$factoryCallCount"
            },
            reset = { /* no reset needed for strings */ },
            maxSize = 2
        )
        
        // When
        val obj1 = pool.acquire()
        val obj2 = pool.acquire()
        val obj3 = pool.acquire()
        
        pool.release(obj1)
        pool.release(obj2)
        pool.release(obj3) // This should not be added to pool
        
        // Then
        assertEquals(2, pool.size()) // Only 2 objects in pool
        assertTrue(pool.isPoolFull())
    }
    
    @Test
    fun testResetFunctionIsCalledOnRelease() {
        // Given
        var resetCallCount = 0
        val resetObjects = mutableListOf<String>()
        
        val pool = ObjectPool(
            factory = { "NewObject" },
            reset = { obj -> 
                resetCallCount++
                resetObjects.add(obj)
            },
            maxSize = 3
        )
        
        // When
        val obj1 = pool.acquire()
        pool.release(obj1)
        
        // Then
        assertEquals(1, resetCallCount)
        assertEquals("NewObject", resetObjects[0])
    }
    
    @Test
    fun testClearEmptiesPool() {
        // Given
        val pool = ObjectPool(
            factory = { "Object" },
            reset = { /* no reset needed */ },
            maxSize = 3
        )
        
        val obj1 = pool.acquire()
        val obj2 = pool.acquire()
        pool.release(obj1)
        pool.release(obj2)
        
        // When
        pool.clear()
        
        // Then
        assertEquals(0, pool.size())
        assertTrue(pool.isPoolEmpty())
    }
    
    @Test
    fun testUseExtensionFunction() {
        // Given
        var factoryCallCount = 0
        val pool = ObjectPool(
            factory = { 
                factoryCallCount++
                "Object$factoryCallCount"
            },
            reset = { /* no reset needed */ },
            maxSize = 3
        )
        
        // When
        val result = pool.useObject { obj: String ->
            assertEquals("Object1", obj)
            "Result"
        }
        
        // Then
        assertEquals("Result", result)
        assertEquals(1, factoryCallCount)
        assertEquals(1, pool.size()) // Object was returned to pool
    }
    
    @Test
    fun testUseExtensionFunctionWithException() {
        // Given
        var factoryCallCount = 0
        val pool = ObjectPool(
            factory = { 
                factoryCallCount++
                "Object$factoryCallCount"
            },
            reset = { /* no reset needed */ },
            maxSize = 3
        )
        
        // When & Then
        try {
            pool.useObject { obj: String ->
                assertEquals("Object1", obj)
                throw RuntimeException("Test exception")
            }
            fail("Expected exception was not thrown")
        } catch (e: RuntimeException) {
            assertEquals("Test exception", e.message)
        }
        
        assertEquals(1, factoryCallCount)
        assertEquals(1, pool.size()) // Object was still returned to pool despite exception
    }
} 
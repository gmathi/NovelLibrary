package io.github.gmathi.novellibrary.core.system

import android.content.Context
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk

/**
 * Unit tests for DataAccessor interface.
 * Tests interface implementation contracts.
 */
class DataAccessorTest : StringSpec({
    
    "DataAccessor interface should define required properties" {
        val accessor = object : DataAccessor {
            override val firebaseAnalytics: Any = "analytics"
            override val dataCenter: Any = "dataCenter"
            override val dbHelper: Any = "dbHelper"
            override val sourceManager: Any = "sourceManager"
            override val networkHelper: Any = "networkHelper"
            override fun getContext(): Context? = mockk()
        }
        
        accessor.firebaseAnalytics shouldNotBe null
        accessor.dataCenter shouldNotBe null
        accessor.dbHelper shouldNotBe null
        accessor.sourceManager shouldNotBe null
        accessor.networkHelper shouldNotBe null
        accessor.getContext() shouldNotBe null
    }
    
    "DataAccessor getContext can return null" {
        val accessor = object : DataAccessor {
            override val firebaseAnalytics: Any = mockk()
            override val dataCenter: Any = mockk()
            override val dbHelper: Any = mockk()
            override val sourceManager: Any = mockk()
            override val networkHelper: Any = mockk()
            override fun getContext(): Context? = null
        }
        
        accessor.getContext() shouldBe null
    }
    
    "DataAccessor uses generic types for dependencies" {
        // This test validates that DataAccessor uses Any type for dependencies
        // allowing concrete implementations to provide specific types
        
        val accessor = object : DataAccessor {
            override val firebaseAnalytics: Any = "string_analytics"
            override val dataCenter: Any = 123
            override val dbHelper: Any = true
            override val sourceManager: Any = listOf(1, 2, 3)
            override val networkHelper: Any = mapOf("key" to "value")
            override fun getContext(): Context? = mockk()
        }
        
        // Verify that different types can be used
        (accessor.firebaseAnalytics as String) shouldBe "string_analytics"
        (accessor.dataCenter as Int) shouldBe 123
        (accessor.dbHelper as Boolean) shouldBe true
        (accessor.sourceManager as List<*>).size shouldBe 3
        (accessor.networkHelper as Map<*, *>).size shouldBe 1
    }
})

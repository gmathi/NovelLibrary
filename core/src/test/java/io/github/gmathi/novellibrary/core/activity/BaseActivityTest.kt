package io.github.gmathi.novellibrary.core.activity

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Unit tests for BaseActivity abstract class.
 * Tests abstract method contracts and interface implementation.
 */
class BaseActivityTest : StringSpec({
    
    "BaseActivity should have required abstract properties from DataAccessor" {
        // This test validates that BaseActivity implements DataAccessor interface
        // The test passes if the class implements the interface
        
        val interfaces = BaseActivity::class.java.interfaces.map { it.simpleName }
        interfaces.contains("DataAccessor") shouldBe true
    }
    
    "BaseActivity should have abstract methods for edge-to-edge display" {
        // This test validates that BaseActivity has abstract methods
        // setupEdgeToEdge(), applyWindowInsets(), and getLocaleContext()
        
        val methods = BaseActivity::class.java.declaredMethods.map { it.name }
        
        methods.contains("setupEdgeToEdge") shouldBe true
        methods.contains("applyWindowInsets") shouldBe true
        methods.contains("getLocaleContext") shouldBe true
    }
    
    "BaseActivity should extend AppCompatActivity" {
        // Verify BaseActivity extends AppCompatActivity
        val superclass = BaseActivity::class.java.superclass
        superclass.simpleName shouldBe "AppCompatActivity"
    }
})

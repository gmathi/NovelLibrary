package io.github.gmathi.novellibrary.core.fragment

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Unit tests for BaseFragment abstract class.
 * Tests abstract property contracts and interface implementation.
 */
class BaseFragmentTest : StringSpec({
    
    "BaseFragment should have required abstract properties from DataAccessor" {
        // This test validates that BaseFragment implements DataAccessor interface
        
        val interfaces = BaseFragment::class.java.interfaces.map { it.simpleName }
        interfaces.contains("DataAccessor") shouldBe true
    }
    
    "BaseFragment should extend Fragment" {
        // Verify BaseFragment extends Fragment
        val superclass = BaseFragment::class.java.superclass
        superclass.simpleName shouldBe "Fragment"
    }
})

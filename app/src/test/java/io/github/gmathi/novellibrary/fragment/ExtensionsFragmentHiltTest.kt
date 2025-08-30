package io.github.gmathi.novellibrary.fragment

import android.content.Context
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.gmathi.novellibrary.extension.ExtensionManager
import io.github.gmathi.novellibrary.extension.model.Extension
import io.github.gmathi.novellibrary.extension.model.ExtensionItem
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for ExtensionsFragment with pure Hilt injection
 * Validates that the fragment works correctly without any Injekt dependencies
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ExtensionsFragmentHiltTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var extensionManager: ExtensionManager

    private lateinit var scenario: FragmentScenario<ExtensionsFragment>

    @Before
    fun setup() {
        hiltRule.inject()
        
        // Mock the extension flows to prevent network calls during testing
        every { extensionManager.installedExtensionsFlow } returns flowOf(emptyList())
        every { extensionManager.untrustedExtensionsFlow } returns flowOf(emptyList())
        every { extensionManager.availableExtensionsFlow } returns flowOf(emptyList())
        every { extensionManager.findAvailableExtensions() } returns Unit
    }

    @Test
    fun `extensionManager is properly injected`() {
        assertNotNull(extensionManager)
    }

    @Test
    fun `fragment launches successfully with Hilt injection`() {
        scenario = launchFragmentInContainer<ExtensionsFragment>()
        
        scenario.onFragment { fragment ->
            // Verify that the fragment has been created successfully
            assertNotNull(fragment)
            
            // Verify that the ExtensionManager is injected
            assertNotNull(fragment.extensionManager)
        }
    }

    @Test
    fun `fragment uses requireContext applicationContext instead of Injekt get`() {
        scenario = launchFragmentInContainer<ExtensionsFragment>()
        
        scenario.onFragment { fragment ->
            // Create test data
            val testExtensions = Triple(
                emptyList<Extension.Installed>(),
                emptyList<Extension.Untrusted>(),
                listOf(createTestAvailableExtension())
            )
            
            // Call toItems method through reflection to test context access
            val toItemsMethod = fragment.javaClass.getDeclaredMethod(
                "toItems",
                Triple::class.java
            )
            toItemsMethod.isAccessible = true
            
            // This should not throw an exception and should use requireContext().applicationContext
            val result = toItemsMethod.invoke(fragment, testExtensions) as List<ExtensionItem>
            
            // Verify that the method executed successfully
            assertNotNull(result)
        }
    }

    @Test
    fun `fragment handles empty extension lists correctly`() {
        scenario = launchFragmentInContainer<ExtensionsFragment>()
        
        scenario.onFragment { fragment ->
            val emptyExtensions = Triple(
                emptyList<Extension.Installed>(),
                emptyList<Extension.Untrusted>(),
                emptyList<Extension.Available>()
            )
            
            val toItemsMethod = fragment.javaClass.getDeclaredMethod(
                "toItems",
                Triple::class.java
            )
            toItemsMethod.isAccessible = true
            
            val result = toItemsMethod.invoke(fragment, emptyExtensions) as List<ExtensionItem>
            
            assertTrue(result.isEmpty())
        }
    }

    @Test
    fun `fragment processes available extensions correctly`() {
        scenario = launchFragmentInContainer<ExtensionsFragment>()
        
        scenario.onFragment { fragment ->
            val extensionsWithAvailable = Triple(
                emptyList<Extension.Installed>(),
                emptyList<Extension.Untrusted>(),
                listOf(createTestAvailableExtension())
            )
            
            val toItemsMethod = fragment.javaClass.getDeclaredMethod(
                "toItems",
                Triple::class.java
            )
            toItemsMethod.isAccessible = true
            
            val result = toItemsMethod.invoke(fragment, extensionsWithAvailable) as List<ExtensionItem>
            
            // Should have at least one item (the available extension)
            assertTrue(result.isNotEmpty())
        }
    }

    private fun createTestAvailableExtension(): Extension.Available {
        return Extension.Available(
            name = "Test Extension",
            pkgName = "com.test.extension",
            versionName = "1.0.0",
            versionCode = 1,
            lang = "en",
            nsfw = false,
            apkName = "test.apk",
            iconUrl = "https://example.com/icon.png"
        )
    }
}
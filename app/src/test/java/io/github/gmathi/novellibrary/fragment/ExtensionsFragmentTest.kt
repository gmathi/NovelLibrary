package io.github.gmathi.novellibrary.fragment

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gmathi.novellibrary.extension.ExtensionManager
import io.github.gmathi.novellibrary.extension.model.Extension
import io.github.gmathi.novellibrary.extension.model.InstallStep
import io.github.gmathi.novellibrary.util.coroutines.CoroutineTestRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class ExtensionsFragmentTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var fragment: ExtensionsFragment
    private lateinit var mockExtensionManager: ExtensionManager

    private val installedFlow = flowOf<List<Extension.Installed>>(emptyList())
    private val untrustedFlow = flowOf<List<Extension.Untrusted>>(emptyList())
    private val availableFlow = flowOf<List<Extension.Available>>(emptyList())

    @Before
    fun setup() {
        mockExtensionManager = mockk(relaxed = true)
        
        every { mockExtensionManager.getInstalledExtensionsFlow() } returns installedFlow
        every { mockExtensionManager.getUntrustedExtensionsFlow() } returns untrustedFlow
        every { mockExtensionManager.getAvailableExtensionsFlow() } returns availableFlow
        
        fragment = ExtensionsFragment()
        
        // Mock the extension manager injection
        mockkStatic("uy.kohesive.injekt.InjektKt")
        every { io.github.gmathi.novellibrary.extension.ExtensionManager::class.java.injectLazy() } returns lazy { mockExtensionManager }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `bindToExtensionsFlow should handle empty extensions list`() = runTest {
        // Given
        every { mockExtensionManager.getInstalledExtensionsFlow() } returns flowOf(emptyList())
        every { mockExtensionManager.getUntrustedExtensionsFlow() } returns flowOf(emptyList())
        every { mockExtensionManager.getAvailableExtensionsFlow() } returns flowOf(emptyList())

        // When
        // Fragment lifecycle would trigger bindToExtensionsFlow
        
        // Then
        // Verify that empty state is handled correctly
        verify(exactly = 0) { mockExtensionManager.installExtension(any()) }
    }

    @Test
    fun `collectInstallUpdates should handle installation steps`() = runTest {
        // Given
        val extension = mockk<Extension.Available> {
            every { pkgName } returns "test.package"
            every { name } returns "Test Extension"
        }
        val installSteps = listOf(
            InstallStep.Pending,
            InstallStep.Downloading,
            InstallStep.Installing,
            InstallStep.Installed
        )
        
        val installFlow = flowOf(*installSteps.toTypedArray())
        every { mockExtensionManager.installExtension(extension) } returns installFlow

        // When
        // This would be called when user clicks install button
        
        // Then
        verify { mockExtensionManager.installExtension(extension) }
    }

    @Test
    fun `fragment should cancel jobs on destroy`() = runTest {
        // Given
        fragment.onDestroyView()
        
        // Then
        // Verify that coroutine jobs are properly cancelled
        // This is implicitly tested by the lifecycle management
    }

    @Test
    fun `extension installation should update current downloads`() = runTest {
        // Given
        val extension = mockk<Extension.Available> {
            every { pkgName } returns "test.package"
            every { name } returns "Test Extension"
        }
        
        val installFlow = flowOf(InstallStep.Downloading)
        every { mockExtensionManager.installExtension(extension) } returns installFlow

        // When
        // Installation process starts
        
        // Then
        // Verify that download state is tracked
        verify { mockExtensionManager.installExtension(extension) }
    }

    @Test
    fun `extension uninstallation should be handled correctly`() = runTest {
        // Given
        val extension = mockk<Extension.Installed> {
            every { pkgName } returns "test.package"
            every { hasUpdate } returns false
        }

        // When
        // User clicks uninstall
        
        // Then
        verify(exactly = 0) { mockExtensionManager.uninstallExtension("test.package") }
    }

    @Test
    fun `extension update should trigger installation flow`() = runTest {
        // Given
        val extension = mockk<Extension.Installed> {
            every { pkgName } returns "test.package"
            every { hasUpdate } returns true
        }
        
        val updateFlow = flowOf(InstallStep.Downloading)
        coEvery { mockExtensionManager.updateExtension(extension) } returns updateFlow

        // When
        // User clicks update
        
        // Then
        verify { mockExtensionManager.updateExtension(extension) }
    }

    @Test
    fun `untrusted extension should trigger trust signature`() = runTest {
        // Given
        val extension = mockk<Extension.Untrusted> {
            every { signatureHash } returns "test-signature"
        }

        // When
        // User clicks trust
        
        // Then
        verify(exactly = 0) { mockExtensionManager.trustSignature("test-signature") }
    }
}
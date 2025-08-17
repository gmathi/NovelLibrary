package io.github.gmathi.novellibrary.extension

import android.content.Context
import io.github.gmathi.novellibrary.extension.api.ExtensionGithubApi
import io.github.gmathi.novellibrary.extension.model.Extension
import io.github.gmathi.novellibrary.extension.model.InstallStep
import io.github.gmathi.novellibrary.extension.util.ExtensionInstaller
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExtensionCommunicationTest {

    private lateinit var context: Context
    private lateinit var dataCenter: DataCenter
    private lateinit var sourceManager: SourceManager
    private lateinit var extensionManager: ExtensionManager
    private lateinit var mockApi: ExtensionGithubApi
    private lateinit var mockInstaller: ExtensionInstaller

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        dataCenter = mockk(relaxed = true)
        sourceManager = mockk(relaxed = true)
        mockApi = mockk()
        mockInstaller = mockk()
        
        extensionManager = ExtensionManager(context, dataCenter)
        
        // Mock the private fields
        val apiField = ExtensionManager::class.java.getDeclaredField("api")
        apiField.isAccessible = true
        apiField.set(extensionManager, mockApi)
        
        val installerField = ExtensionManager::class.java.getDeclaredField("installer")
        installerField.isAccessible = true
        installerField.set(extensionManager, mockInstaller)
    }

    @Test
    fun `test extension flows emit updates correctly`() = runTest {
        // Test that StateFlows properly emit updates when extensions change
        val initialInstalled = extensionManager.getInstalledExtensionsFlow().first()
        assertTrue(initialInstalled.isEmpty())

        // Simulate extension installation by updating the internal list
        val installedExtension = Extension.Installed(
            name = "Test Extension",
            pkgName = "com.test.extension",
            versionName = "1.0",
            versionCode = 1,
            lang = "en",
            nsfw = false,
            sources = emptyList(),
            pkgFactory = null,
            isUnofficial = false,
            hasUpdate = false,
            isObsolete = false
        )

        // Use reflection to update the installed extensions
        val extensionsField = ExtensionManager::class.java.getDeclaredField("installedExtensions")
        extensionsField.isAccessible = true
        extensionsField.set(extensionManager, listOf(installedExtension))

        // The flow should now emit the updated list
        val updatedInstalled = extensionManager.getInstalledExtensionsFlow().first()
        assertEquals(1, updatedInstalled.size)
        assertEquals("Test Extension", updatedInstalled.first().name)
    }

    @Test
    fun `test installation flow communication`() = runTest {
        val extension = Extension.Available(
            name = "Test Extension",
            pkgName = "com.test.extension",
            versionName = "1.0",
            versionCode = 1,
            lang = "en",
            nsfw = false,
            apkName = "test.apk",
            iconUrl = "test_icon.png"
        )

        val installSteps = listOf(
            InstallStep.Pending,
            InstallStep.Downloading,
            InstallStep.Installing,
            InstallStep.Installed
        )

        val installFlow = flowOf(*installSteps.toTypedArray())
        every { mockInstaller.downloadAndInstall(any(), any()) } returns installFlow
        every { mockApi.getApkUrl(extension) } returns "https://test.com/test.apk"

        val resultFlow = extensionManager.installExtension(extension)
        val collectedSteps = resultFlow.take(4).toList()

        assertEquals(4, collectedSteps.size)
        assertEquals(InstallStep.Pending, collectedSteps[0])
        assertEquals(InstallStep.Downloading, collectedSteps[1])
        assertEquals(InstallStep.Installing, collectedSteps[2])
        assertEquals(InstallStep.Installed, collectedSteps[3])
    }

    @Test
    fun `test update flow communication`() = runTest {
        val installedExtension = Extension.Installed(
            name = "Test Extension",
            pkgName = "com.test.extension",
            versionName = "1.0",
            versionCode = 1,
            lang = "en",
            nsfw = false,
            sources = emptyList(),
            pkgFactory = null,
            isUnofficial = false,
            hasUpdate = true,
            isObsolete = false
        )

        val availableExtension = Extension.Available(
            name = "Test Extension",
            pkgName = "com.test.extension",
            versionName = "2.0",
            versionCode = 2,
            lang = "en",
            nsfw = false,
            apkName = "test.apk",
            iconUrl = "test_icon.png"
        )

        // Set up available extensions
        val availableExtensionsField = ExtensionManager::class.java.getDeclaredField("availableExtensions")
        availableExtensionsField.isAccessible = true
        availableExtensionsField.set(extensionManager, listOf(availableExtension))

        val updateSteps = listOf(InstallStep.Downloading, InstallStep.Installed)
        val updateFlow = flowOf(*updateSteps.toTypedArray())
        every { mockInstaller.downloadAndInstall(any(), any()) } returns updateFlow
        every { mockApi.getApkUrl(availableExtension) } returns "https://test.com/test.apk"

        val resultFlow = extensionManager.updateExtension(installedExtension)
        assertNotNull(resultFlow)

        val collectedSteps = resultFlow!!.take(2).toList()
        assertEquals(2, collectedSteps.size)
        assertEquals(InstallStep.Downloading, collectedSteps[0])
        assertEquals(InstallStep.Installed, collectedSteps[1])
    }

    @Test
    fun `test extension lifecycle management`() = runTest {
        // Test that extension manager properly handles extension lifecycle events
        extensionManager.init(sourceManager)

        // Verify that source manager is initialized
        verify { sourceManager.registerSource(any()) }
    }

    @Test
    fun `test extension status updates`() = runTest {
        val availableExtension = Extension.Available(
            name = "Test Extension",
            pkgName = "com.test.extension",
            versionName = "2.0",
            versionCode = 2,
            lang = "en",
            nsfw = false,
            apkName = "test.apk",
            iconUrl = "test_icon.png"
        )

        val installedExtension = Extension.Installed(
            name = "Test Extension",
            pkgName = "com.test.extension",
            versionName = "1.0",
            versionCode = 1,
            lang = "en",
            nsfw = false,
            sources = emptyList(),
            pkgFactory = null,
            isUnofficial = false,
            hasUpdate = false,
            isObsolete = false
        )

        // Set up installed extensions
        val installedExtensionsField = ExtensionManager::class.java.getDeclaredField("installedExtensions")
        installedExtensionsField.isAccessible = true
        installedExtensionsField.set(extensionManager, listOf(installedExtension))

        // Set available extensions (this should trigger status update)
        val availableExtensionsField = ExtensionManager::class.java.getDeclaredField("availableExtensions")
        availableExtensionsField.isAccessible = true
        availableExtensionsField.set(extensionManager, listOf(availableExtension))

        // Trigger the status update method
        val updateMethod = ExtensionManager::class.java.getDeclaredMethod(
            "updatedInstalledExtensionsStatuses",
            List::class.java
        )
        updateMethod.isAccessible = true
        updateMethod.invoke(extensionManager, listOf(availableExtension))

        // Verify that the extension now has an update available
        val updatedExtensions = extensionManager.installedExtensions
        assertTrue(updatedExtensions.first().hasUpdate)
    }

    @Test
    fun `test cancellation handling in flows`() = runTest {
        val extension = Extension.Available(
            name = "Test Extension",
            pkgName = "com.test.extension",
            versionName = "1.0",
            versionCode = 1,
            lang = "en",
            nsfw = false,
            apkName = "test.apk",
            iconUrl = "test_icon.png"
        )

        // Create a flow that would normally continue indefinitely
        val installFlow = flowOf(InstallStep.Pending, InstallStep.Downloading)
        every { mockInstaller.downloadAndInstall(any(), any()) } returns installFlow
        every { mockApi.getApkUrl(extension) } returns "https://test.com/test.apk"

        val resultFlow = extensionManager.installExtension(extension)
        
        // Take only first step and verify cancellation works
        val firstStep = resultFlow.first()
        assertEquals(InstallStep.Pending, firstStep)
    }
}
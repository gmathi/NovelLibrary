package io.github.gmathi.novellibrary.extension

import android.content.Context
import android.graphics.drawable.Drawable
import io.github.gmathi.novellibrary.extension.api.ExtensionGithubApi
import io.github.gmathi.novellibrary.extension.model.Extension
import io.github.gmathi.novellibrary.extension.model.InstallStep
import io.github.gmathi.novellibrary.extension.model.LoadResult
import io.github.gmathi.novellibrary.extension.util.ExtensionInstaller
import io.github.gmathi.novellibrary.extension.util.ExtensionLoader
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.Source
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExtensionManagerTest {

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
        
        // Mock the private fields using reflection or dependency injection
        val apiField = ExtensionManager::class.java.getDeclaredField("api")
        apiField.isAccessible = true
        apiField.set(extensionManager, mockApi)
    }

    @Test
    fun `test extension flows are properly initialized`() = runTest {
        // Test that flows are initialized with empty lists
        val installedExtensions = extensionManager.getInstalledExtensionsFlow().first()
        val availableExtensions = extensionManager.getAvailableExtensionsFlow().first()
        val untrustedExtensions = extensionManager.getUntrustedExtensionsFlow().first()

        assertTrue(installedExtensions.isEmpty())
        assertTrue(availableExtensions.isEmpty())
        assertTrue(untrustedExtensions.isEmpty())
    }

    @Test
    fun `test findAvailableExtensions updates flow`() = runTest {
        val mockExtensions = listOf(
            Extension.Available(
                name = "Test Extension",
                pkgName = "com.test.extension",
                versionName = "1.0",
                versionCode = 1,
                lang = "en",
                nsfw = false,
                apkName = "test.apk",
                iconUrl = "test_icon.png"
            )
        )

        coEvery { mockApi.findExtensions() } returns mockExtensions

        extensionManager.findAvailableExtensions()

        // Wait for the coroutine to complete and check the flow
        val availableExtensions = extensionManager.getAvailableExtensionsFlow().first()
        assertEquals(1, availableExtensions.size)
        assertEquals("Test Extension", availableExtensions.first().name)
    }

    @Test
    fun `test findAvailableExtensions handles exceptions`() = runTest {
        coEvery { mockApi.findExtensions() } throws RuntimeException("Network error")
        every { context.toast(any<String>()) } returns Unit

        extensionManager.findAvailableExtensions()

        // Should handle exception gracefully and result in empty list
        val availableExtensions = extensionManager.getAvailableExtensionsFlow().first()
        assertTrue(availableExtensions.isEmpty())
    }

    @Test
    fun `test installExtension returns flow`() = runTest {
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

        val mockInstallFlow = flowOf(InstallStep.Pending, InstallStep.Downloading, InstallStep.Installed)
        every { mockInstaller.downloadAndInstall(any(), any()) } returns mockInstallFlow
        every { mockApi.getApkUrl(extension) } returns "https://test.com/test.apk"

        // Mock the installer field
        val installerField = ExtensionManager::class.java.getDeclaredField("installer")
        installerField.isAccessible = true
        installerField.set(extensionManager, mockInstaller)

        val installFlow = extensionManager.installExtension(extension)
        assertNotNull(installFlow)
    }

    @Test
    fun `test updateExtension returns null for non-existent extension`() = runTest {
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

        val result = extensionManager.updateExtension(installedExtension)
        assertEquals(null, result)
    }

    @Test
    fun `test getAppIconForSource returns correct icon`() {
        val mockSource = mockk<Source> {
            every { id } returns 1L
        }
        
        val mockDrawable = mockk<Drawable>()
        val installedExtension = Extension.Installed(
            name = "Test Extension",
            pkgName = "com.test.extension",
            versionName = "1.0",
            versionCode = 1,
            lang = "en",
            nsfw = false,
            sources = listOf(mockSource),
            pkgFactory = null,
            isUnofficial = false,
            hasUpdate = false,
            isObsolete = false
        )

        // Set installed extensions
        val extensionsField = ExtensionManager::class.java.getDeclaredField("installedExtensions")
        extensionsField.isAccessible = true
        extensionsField.set(extensionManager, listOf(installedExtension))

        every { context.packageManager.getApplicationIcon("com.test.extension") } returns mockDrawable

        val result = extensionManager.getAppIconForSource(mockSource)
        assertNotNull(result)
    }
}
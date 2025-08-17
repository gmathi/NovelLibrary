package io.github.gmathi.novellibrary.extension

import android.content.Context
import android.graphics.drawable.Drawable
import io.github.gmathi.novellibrary.extension.api.ExtensionGithubApi
import io.github.gmathi.novellibrary.extension.model.Extension
import io.github.gmathi.novellibrary.extension.model.InstallStep
import io.github.gmathi.novellibrary.extension.model.LoadResult
import io.github.gmathi.novellibrary.extension.util.ExtensionInstallReceiver
import io.github.gmathi.novellibrary.extension.util.ExtensionInstaller
import io.github.gmathi.novellibrary.extension.util.ExtensionLoader
import io.github.gmathi.novellibrary.extension.util.getApplicationIcon
import io.github.gmathi.novellibrary.model.source.Source
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.util.lang.launchNow
import io.github.gmathi.novellibrary.util.system.toast
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * The manager of extensions installed as another apk which extend the available sources. It handles
 * the retrieval of remotely available extensions as well as installing, updating and removing them.
 * To avoid malicious distribution, every extension must be signed and it will only be loaded if its
 * signature is trusted, otherwise the user will be prompted with a warning to trust it before being
 * loaded.
 *
 * @param context The application context.
 * @param preferences The application preferences.
 */
class ExtensionManager(
    private val context: Context,
    private val dataCenter: DataCenter = Injekt.get()
) {

    /**
     * API where all the available extensions can be found.
     */
    private val api = ExtensionGithubApi()

    /**
     * The installer which installs, updates and uninstalls the extensions.
     */
    private val installer by lazy { ExtensionInstaller(context) }

    /**
     * StateFlow used to notify the installed extensions.
     */
    private val _installedExtensionsFlow = MutableStateFlow<List<Extension.Installed>>(emptyList())
    val installedExtensionsFlow: Flow<List<Extension.Installed>> = _installedExtensionsFlow.asStateFlow()

    private val iconMap = mutableMapOf<String, Drawable>()

    /**
     * List of the currently installed extensions.
     */
    var installedExtensions = emptyList<Extension.Installed>()
        private set(value) {
            field = value
            _installedExtensionsFlow.value = value
        }

    fun getAppIconForSource(source: Source): Drawable? {
        val pkgName = installedExtensions.find { ext -> ext.sources.any { it.id == source.id } }?.pkgName
        if (pkgName != null) {
            return iconMap[pkgName] ?: iconMap.getOrPut(pkgName) { context.packageManager.getApplicationIcon(pkgName) }
        }
        return null
    }

    fun getAppIconForSource(source: Source, context: Context): Drawable? {
        val extension = installedExtensions.find { ext -> ext.sources.any { it.id == source.id } }
        return extension?.getApplicationIcon(context)
    }

    /**
     * StateFlow used to notify the available extensions.
     */
    private val _availableExtensionsFlow = MutableStateFlow<List<Extension.Available>>(emptyList())
    val availableExtensionsFlow: Flow<List<Extension.Available>> = _availableExtensionsFlow.asStateFlow()

    /**
     * List of the currently available extensions.
     */
    var availableExtensions = emptyList<Extension.Available>()
        private set(value) {
            field = value
            _availableExtensionsFlow.value = value
            updatedInstalledExtensionsStatuses(value)
        }

    /**
     * StateFlow used to notify the untrusted extensions.
     */
    private val _untrustedExtensionsFlow = MutableStateFlow<List<Extension.Untrusted>>(emptyList())
    val untrustedExtensionsFlow: Flow<List<Extension.Untrusted>> = _untrustedExtensionsFlow.asStateFlow()

    /**
     * List of the currently untrusted extensions.
     */
    var untrustedExtensions = emptyList<Extension.Untrusted>()
        private set(value) {
            field = value
            _untrustedExtensionsFlow.value = value
        }

    /**
     * The source manager where the sources of the extensions are added.
     */
    private lateinit var sourceManager: SourceManager

    /**
     * Initializes this manager with the given source manager.
     */
    fun init(sourceManager: SourceManager) {
        this.sourceManager = sourceManager
        initExtensions()
        ExtensionInstallReceiver(InstallationListener()).register(context)
    }

    /**
     * Loads and registers the installed extensions.
     */
    private fun initExtensions() {
        val extensions = ExtensionLoader.loadExtensions(context)

        installedExtensions = extensions
            .filterIsInstance<LoadResult.Success>()
            .map { it.extension }
        installedExtensions
            .flatMap { it.sources }
            .forEach { sourceManager.registerSource(it) }

        untrustedExtensions = extensions
            .filterIsInstance<LoadResult.Untrusted>()
            .map { it.extension }
    }



    /**
     * Finds the available extensions in the [api] and updates [availableExtensions].
     */
    fun findAvailableExtensions() {
        launchNow {
            val extensions: List<Extension.Available> = try {
                api.findExtensions()
            } catch (e: Exception) {
                context.toast(e.message)
                emptyList()
            }

            availableExtensions = extensions
        }
    }

    /**
     * Sets the update field of the installed extensions with the given [availableExtensions].
     *
     * @param availableExtensions The list of extensions given by the [api].
     */
    private fun updatedInstalledExtensionsStatuses(availableExtensions: List<Extension.Available>) {
        if (availableExtensions.isEmpty()) {
            dataCenter.extensionUpdatesCount = 0
            return
        }

        val mutInstalledExtensions = installedExtensions.toMutableList()
        var changed = false

        for ((index, installedExt) in mutInstalledExtensions.withIndex()) {
            val pkgName = installedExt.pkgName
            val availableExt = availableExtensions.find { it.pkgName == pkgName }

            if (availableExt == null && !installedExt.isObsolete) {
                mutInstalledExtensions[index] = installedExt.copy(isObsolete = true)
                changed = true
            } else if (availableExt != null) {
                val hasUpdate = availableExt.versionCode > installedExt.versionCode
                if (installedExt.hasUpdate != hasUpdate) {
                    mutInstalledExtensions[index] = installedExt.copy(hasUpdate = hasUpdate)
                    changed = true
                }
            }
        }
        if (changed) {
            installedExtensions = mutInstalledExtensions
        }
        updatePendingUpdatesCount()
    }

    /**
     * Returns a flow of the installation process for the given extension. It will complete
     * once the extension is installed or throws an error. The process will be canceled if
     * the coroutine is cancelled before its completion.
     *
     * @param extension The extension to be installed.
     */
    fun installExtension(extension: Extension.Available): Flow<InstallStep> {
        return installer.downloadAndInstall(api.getApkUrl(extension), extension)
    }

    /**
     * Returns a flow of the installation process for the given extension. It will complete
     * once the extension is updated or throws an error. The process will be canceled if
     * the coroutine is cancelled before its completion.
     *
     * @param extension The extension to be updated.
     */
    suspend fun updateExtension(extension: Extension.Installed): Flow<InstallStep>? {
        val availableExt = availableExtensions.find { it.pkgName == extension.pkgName }
            ?: return null
        return installExtension(availableExt)
    }

    /**
     * Sets the result of the installation of an extension.
     *
     * @param downloadId The id of the download.
     * @param result Whether the extension was installed or not.
     */
    fun setInstallationResult(downloadId: Long, result: Boolean) {
        installer.setInstallationResult(downloadId, result)
    }

    /**
     * Uninstalls the extension that matches the given package name.
     *
     * @param pkgName The package name of the application to uninstall.
     */
    fun uninstallExtension(pkgName: String) {
        installer.uninstallApk(pkgName)
    }

    /**
     * Adds the given signature to the list of trusted signatures. It also loads in background the
     * extensions that match this signature.
     *
     * @param signature The signature to whitelist.
     */
    fun trustSignature(signature: String) {
        val untrustedSignatures = untrustedExtensions.map { it.signatureHash }.toSet()
        if (signature !in untrustedSignatures) return

        ExtensionLoader.trustedSignatures += signature
        dataCenter.trustedSignatures = (dataCenter.trustedSignatures + signature) as MutableSet<String>

        val nowTrustedExtensions = untrustedExtensions.filter { it.signatureHash == signature }
        untrustedExtensions -= nowTrustedExtensions

        val ctx = context
        launchNow {
            nowTrustedExtensions
                .map { extension ->
                    async { ExtensionLoader.loadExtensionFromPkgName(ctx, extension.pkgName) }
                }
                .map { it.await() }
                .forEach { result ->
                    if (result is LoadResult.Success) {
                        registerNewExtension(result.extension)
                    }
                }
        }
    }

    /**
     * Registers the given extension in this and the source managers.
     *
     * @param extension The extension to be registered.
     */
    private fun registerNewExtension(extension: Extension.Installed) {
        installedExtensions += extension
        extension.sources.forEach { sourceManager.registerSource(it) }
    }

    /**
     * Registers the given updated extension in this and the source managers previously removing
     * the outdated ones.
     *
     * @param extension The extension to be registered.
     */
    private fun registerUpdatedExtension(extension: Extension.Installed) {
        val mutInstalledExtensions = installedExtensions.toMutableList()
        val oldExtension = mutInstalledExtensions.find { it.pkgName == extension.pkgName }
        if (oldExtension != null) {
            mutInstalledExtensions -= oldExtension
            extension.sources.forEach { sourceManager.unregisterSource(it) }
        }
        mutInstalledExtensions += extension
        installedExtensions = mutInstalledExtensions
        extension.sources.forEach { sourceManager.registerSource(it) }
    }

    /**
     * Unregisters the extension in this and the source managers given its package name. Note this
     * method is called for every uninstalled application in the system.
     *
     * @param pkgName The package name of the uninstalled application.
     */
    private fun unregisterExtension(pkgName: String) {
        val installedExtension = installedExtensions.find { it.pkgName == pkgName }
        if (installedExtension != null) {
            installedExtensions -= installedExtension
            installedExtension.sources.forEach { sourceManager.unregisterSource(it) }
        }
        val untrustedExtension = untrustedExtensions.find { it.pkgName == pkgName }
        if (untrustedExtension != null) {
            untrustedExtensions -= untrustedExtension
        }
    }

    /**
     * Listener which receives events of the extensions being installed, updated or removed.
     */
    private inner class InstallationListener : ExtensionInstallReceiver.Listener {

        override fun onExtensionInstalled(extension: Extension.Installed) {
            registerNewExtension(extension.withUpdateCheck())
            updatePendingUpdatesCount()
        }

        override fun onExtensionUpdated(extension: Extension.Installed) {
            registerUpdatedExtension(extension.withUpdateCheck())
            updatePendingUpdatesCount()
        }

        override fun onExtensionUntrusted(extension: Extension.Untrusted) {
            untrustedExtensions += extension
        }

        override fun onPackageUninstalled(pkgName: String) {
            unregisterExtension(pkgName)
            updatePendingUpdatesCount()
        }
    }

    /**
     * Extension method to set the update field of an installed extension.
     */
    private fun Extension.Installed.withUpdateCheck(): Extension.Installed {
        val availableExt = availableExtensions.find { it.pkgName == pkgName }
        if (availableExt != null && availableExt.versionCode > versionCode) {
            return copy(hasUpdate = true)
        }
        return this
    }

    private fun updatePendingUpdatesCount() {
        dataCenter.extensionUpdatesCount = installedExtensions.count { it.hasUpdate }
    }
}

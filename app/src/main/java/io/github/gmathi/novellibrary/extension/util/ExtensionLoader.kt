package io.github.gmathi.novellibrary.extension.util

import SourceFactory
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import dalvik.system.PathClassLoader
import io.github.gmathi.novellibrary.annotations.Nsfw
import io.github.gmathi.novellibrary.extension.model.Extension
import io.github.gmathi.novellibrary.extension.model.LoadResult
import io.github.gmathi.novellibrary.model.source.CatalogueSource
import io.github.gmathi.novellibrary.model.source.Source
import io.github.gmathi.novellibrary.util.DataCenter
import io.github.gmathi.novellibrary.util.lang.Hash
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import uy.kohesive.injekt.injectLazy

/**
 * Class that handles the loading of the extensions installed in the system.
 */
@SuppressLint("PackageManagerGetSignatures")
internal object ExtensionLoader {

    private val dataCenter: DataCenter by injectLazy()
    private val loadNsfwSource by lazy {
        dataCenter.showNSFWSource
    }

    private const val EXTENSION_FEATURE = "novellibrary.extension"
    private const val METADATA_SOURCE_CLASS = "novellibrary.extension.class"
    private const val METADATA_SOURCE_FACTORY = "novellibrary.extension.factory"
    private const val METADATA_NSFW = "novellibrary.extension.nsfw"
    const val LIB_VERSION_MIN = 1.0
    const val LIB_VERSION_MAX = 1.0

    private const val PACKAGE_FLAGS = PackageManager.GET_CONFIGURATIONS or PackageManager.GET_SIGNATURES

    // novellibrary's key
    private const val officialSignature = "e8db103a37baf2d3eb38cdd72403ef55d910305f8e0f11be2890945a83a9f837"

    /**
     * List of the trusted signatures.
     */
    var trustedSignatures = mutableSetOf<String>() + dataCenter.trustedSignatures + officialSignature

    /**
     * Return a list of all the installed extensions initialized concurrently.
     *
     * @param context The application context.
     */
    fun loadExtensions(context: Context): List<LoadResult> {
        val pkgManager = context.packageManager
        val installedPkgs = pkgManager.getInstalledPackages(PACKAGE_FLAGS)
        val extPkgs = installedPkgs.filter { isPackageAnExtension(it) }

        if (extPkgs.isEmpty()) return emptyList()

        // Load each extension concurrently and wait for completion
        return runBlocking {
            val deferred = extPkgs.map {
                async { loadExtension(context, it.packageName, it) }
            }
            deferred.map { it.await() }
        }
    }

    /**
     * Attempts to load an extension from the given package name. It checks if the extension
     * contains the required feature flag before trying to load it.
     */
    fun loadExtensionFromPkgName(context: Context, pkgName: String): LoadResult {
        val pkgInfo = try {
            context.packageManager.getPackageInfo(pkgName, PACKAGE_FLAGS)
        } catch (error: PackageManager.NameNotFoundException) {
            // Unlikely, but the package may have been uninstalled at this point
            return LoadResult.Error(error)
        }
        if (!isPackageAnExtension(pkgInfo)) {
            return LoadResult.Error("Tried to load a package that wasn't a extension")
        }
        return loadExtension(context, pkgName, pkgInfo)
    }

    /**
     * Loads an extension given its package name.
     *
     * @param context The application context.
     * @param pkgName The package name of the extension to load.
     * @param pkgInfo The package info of the extension.
     */
    private fun loadExtension(context: Context, pkgName: String, pkgInfo: PackageInfo): LoadResult {
        val pkgManager = context.packageManager

        val appInfo = try {
            pkgManager.getApplicationInfo(pkgName, PackageManager.GET_META_DATA)
        } catch (error: PackageManager.NameNotFoundException) {
            // Unlikely, but the package may have been uninstalled at this point
            return LoadResult.Error(error)
        }

        val extName = pkgManager.getApplicationLabel(appInfo).toString().substringAfter("NovelLibrary: ")
        val versionName = pkgInfo.versionName
        val versionCode = pkgInfo.versionCode

        if (versionName.isNullOrEmpty()) {
            val exception = Exception("Missing versionName for extension $extName")
            return LoadResult.Error(exception)
        }

        // Validate lib version
        val libVersion = versionName.substringBeforeLast('.').toDouble()
        if (libVersion < LIB_VERSION_MIN || libVersion > LIB_VERSION_MAX) {
            val exception = Exception(
                "Lib version is $libVersion, while only versions " +
                    "$LIB_VERSION_MIN to $LIB_VERSION_MAX are allowed"
            )
            return LoadResult.Error(exception)
        }

        val signatureHash = getSignatureHash(pkgInfo)

        if (signatureHash == null) {
            return LoadResult.Error("Package $pkgName isn't signed")
        } else if (signatureHash !in trustedSignatures) {
            val extension = Extension.Untrusted(extName, pkgName, versionName, versionCode, signatureHash)
            return LoadResult.Untrusted(extension)
        }

        val isNsfw = appInfo.metaData.getInt(METADATA_NSFW) == 1
        if (!loadNsfwSource && isNsfw) {
            return LoadResult.Error("NSFW extension $pkgName not allowed")
        }

        val classLoader = PathClassLoader(appInfo.sourceDir, null, context.classLoader)

        val sources = appInfo.metaData.getString(METADATA_SOURCE_CLASS)!!
            .split(";")
            .map {
                val sourceClass = it.trim()
                if (sourceClass.startsWith(".")) {
                    pkgInfo.packageName + sourceClass
                } else {
                    sourceClass
                }
            }
            .flatMap {
                try {
                    when (val obj = Class.forName(it, false, classLoader).newInstance()) {
                        is Source -> listOf(obj)
                        is SourceFactory -> {
                            if (isSourceNsfw(obj)) {
                                emptyList()
                            } else {
                                obj.createSources()
                            }
                        }
                        else -> throw Exception("Unknown source class type! ${obj.javaClass}")
                    }
                } catch (e: Throwable) {
                    return LoadResult.Error(e)
                }
            }
            .filter { !isSourceNsfw(it) }

        val langs = sources.filterIsInstance<CatalogueSource>()
            .map { it.lang }
            .toSet()
        val lang = when (langs.size) {
            0 -> ""
            1 -> langs.first()
            else -> "all"
        }

        val extension = Extension.Installed(
            extName,
            pkgName,
            versionName,
            versionCode,
            lang,
            isNsfw,
            sources = sources,
            pkgFactory = appInfo.metaData.getString(METADATA_SOURCE_FACTORY),
            isUnofficial = signatureHash != officialSignature
        )
        return LoadResult.Success(extension)
    }

    /**
     * Returns true if the given package is an extension.
     *
     * @param pkgInfo The package info of the application.
     */
    private fun isPackageAnExtension(pkgInfo: PackageInfo): Boolean {
        return pkgInfo.reqFeatures.orEmpty().any { it.name == EXTENSION_FEATURE }
    }

    /**
     * Returns the signature hash of the package or null if it's not signed.
     *
     * @param pkgInfo The package info of the application.
     */
    private fun getSignatureHash(pkgInfo: PackageInfo): String? {
        val signatures = pkgInfo.signatures
        return if (signatures != null && signatures.isNotEmpty()) {
            Hash.sha256(signatures.first().toByteArray())
        } else {
            null
        }
    }

    /**
     * Checks whether a Source or SourceFactory is annotated with @Nsfw.
     */
    private fun isSourceNsfw(clazz: Any): Boolean {
        if (loadNsfwSource) {
            return false
        }

        if (clazz !is Source && clazz !is SourceFactory) {
            return false
        }

        // Annotations are proxied, hence this janky way of checking for them
        return clazz.javaClass.annotations
            .flatMap { it.javaClass.interfaces.map { it.simpleName } }
            .firstOrNull { it == Nsfw::class.java.simpleName } != null
    }
}

package io.github.gmathi.novellibrary.extension.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.RECEIVER_EXPORTED
import io.github.gmathi.novellibrary.extension.model.Extension
import io.github.gmathi.novellibrary.extension.model.LoadResult
import io.github.gmathi.novellibrary.util.lang.launchNow
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

/**
 * Broadcast receiver that listens for the system's packages installed, updated or removed, and only
 * notifies the given [listener] when the package is an extension.
 *
 * @param listener The listener that should be notified of extension installation events.
 */
internal class ExtensionInstallReceiver(private val listener: Listener) :
    BroadcastReceiver() {

    /**
     * Registers this broadcast receiver
     */
    fun register(context: Context) {
        ContextCompat.registerReceiver(context, this, filter, RECEIVER_EXPORTED)
    }

    /**
     * Returns the intent filter this receiver should subscribe to.
     */
    private val filter
        get() = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }

    /**
     * Called when one of the events of the [filter] is received. When the package is an extension,
     * it's loaded in background and it notifies the [listener] when finished.
     */
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                if (!isReplacing(intent)) launchNow {
                    when (val result = getExtensionFromIntent(context, intent)) {
                        is LoadResult.Success -> listener.onExtensionInstalled(result.extension)
                        is LoadResult.Untrusted -> listener.onExtensionUntrusted(result.extension)
                        else -> {
                        }
                    }
                }
            }
            Intent.ACTION_PACKAGE_REPLACED -> {
                launchNow {
                    when (val result = getExtensionFromIntent(context, intent)) {
                        is LoadResult.Success -> listener.onExtensionUpdated(result.extension)
                        // Not needed as a package can't be upgraded if the signature is different
                        is LoadResult.Untrusted -> {
                        }
                        else -> {}
                    }
                }
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                if (!isReplacing(intent)) {
                    val pkgName = getPackageNameFromIntent(intent)
                    if (pkgName != null) {
                        listener.onPackageUninstalled(pkgName)
                    }
                }
            }
        }
    }

    /**
     * Returns true if this package is performing an update.
     *
     * @param intent The intent that triggered the event.
     */
    private fun isReplacing(intent: Intent): Boolean {
        return intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
    }

    /**
     * Returns the extension triggered by the given intent.
     *
     * @param context The application context.
     * @param intent The intent containing the package name of the extension.
     */
    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun getExtensionFromIntent(context: Context, intent: Intent?): LoadResult {
        val pkgName = getPackageNameFromIntent(intent)
            ?: return LoadResult.Error("Package name not found")
        return GlobalScope.async(Dispatchers.Default, CoroutineStart.DEFAULT) { ExtensionLoader.loadExtensionFromPkgName(context, pkgName) }.await()
    }

    /**
     * Returns the package name of the installed, updated or removed application.
     */
    private fun getPackageNameFromIntent(intent: Intent?): String? {
        return intent?.data?.encodedSchemeSpecificPart ?: return null
    }

    /**
     * Listener that receives extension installation events.
     */
    interface Listener {
        fun onExtensionInstalled(extension: Extension.Installed)
        fun onExtensionUpdated(extension: Extension.Installed)
        fun onExtensionUntrusted(extension: Extension.Untrusted)
        fun onPackageUninstalled(pkgName: String)
    }
}

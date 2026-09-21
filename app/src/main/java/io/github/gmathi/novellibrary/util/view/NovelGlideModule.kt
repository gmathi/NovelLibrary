package io.github.gmathi.novellibrary.util.view

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import io.github.gmathi.novellibrary.network.NetworkHelper
import uy.kohesive.injekt.injectLazy
import java.io.InputStream

/**
 * Routes Glide's network loads through the app's OkHttp client.
 *
 * Glide's default fetcher uses HttpURLConnection, which in this app is subject to the
 * process-wide TLS overrides installed by NovelLibraryApplication.enableSSLSocket(): an
 * accept-all trust manager and a hostname verifier that only passes hosts from a fixed list.
 * Cover images served from any other host silently fail to load in the Glide-based screens
 * (library, novel details, TTS) while the Coil-based Compose screens show them fine.
 *
 * Using the shared OkHttp client gives cover loads standard certificate validation plus the
 * same user agent, cookie jar and DNS settings as every other request the app makes.
 */
@GlideModule
class NovelGlideModule : AppGlideModule() {

    private val networkHelper: NetworkHelper by injectLazy()

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(networkHelper.client))
    }

    override fun isManifestParsingEnabled(): Boolean = false
}

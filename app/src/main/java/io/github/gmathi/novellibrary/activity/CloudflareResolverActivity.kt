package io.github.gmathi.novellibrary.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import io.github.gmathi.novellibrary.compose.cloudflare.CloudflareResolverScreen
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.cloudflare.CloudflareCookieManager
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class CloudflareResolverActivity : BaseActivity() {

    override val skipWindowInsets: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra(EXTRA_URL)
            ?: "https://${HostNames.NOVEL_UPDATES}"

        setContent {
            MaterialTheme {
                CloudflareResolverScreen(
                    url = url,
                    onComplete = { resolvedUrl -> saveCookiesAndFinish(url, resolvedUrl) },
                    onBack = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }

    private fun saveCookiesAndFinish(originalUrl: String, resolvedUrl: String?) {
        Log.d(TAG, "saveCookiesAndFinish: originalUrl=$originalUrl, resolvedUrl=$resolvedUrl")
        val cm = CookieManager.getInstance()

        val resolvedUri = Uri.parse(resolvedUrl ?: originalUrl)
        val originalUri = Uri.parse(originalUrl)
        val host = resolvedUri.host ?: originalUri.host
        val scheme = resolvedUri.scheme ?: "https"

        if (host == null) {
            Log.e(TAG, "saveCookiesAndFinish: could not determine host")
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra(RESULT_COOKIES_SAVED, false)
            })
            finish()
            return
        }

        val bare = host.removePrefix("www.")
        val urlVariants = listOf(
            "$scheme://$bare/",
            "$scheme://www.$bare/",
            originalUrl,
            resolvedUrl
        ).filterNotNull().distinct()

        Log.d(TAG, "URL variants to sync: $urlVariants")

        var cookiesSaved = false
        for (url in urlVariants) {
            try {
                val cookieString = cm.getCookie(url)
                Log.d(TAG, "WebView cookies for $url: ${cookieString?.take(200)}")
                if (cookieString == null) continue
                val httpUrl = url.toHttpUrlOrNull() ?: continue
                val cookies = cookieString.split(";").map { it.trim() }.filter { it.isNotEmpty() }

                networkHelper.cookieManager.saveFromResponse(url, cookies)

                val parsedCookies = cookies.mapNotNull { Cookie.parse(httpUrl, it) }
                val cfCookies = parsedCookies.filter { CloudflareCookieManager.isCloudflareCookie(it) }
                Log.d(TAG, "CF cookies for $url: ${cfCookies.map { "${it.name}=${it.value.take(20)}" }}")
                if (cfCookies.isNotEmpty()) {
                    networkHelper.cloudflareCookieManager.storeCookies(httpUrl, cfCookies)
                    cookiesSaved = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing cookies for $url", e)
            }
        }

        val verifyUrl = "$scheme://www.$bare/".toHttpUrlOrNull()
        if (verifyUrl != null) {
            val jarCookies = networkHelper.cookieManager.get(verifyUrl)
            Log.d(TAG, "Verification - OkHttp jar cookies for $verifyUrl: ${jarCookies.map { "${it.name}=${it.value.take(20)}" }}")
            val cfmCookies = networkHelper.cloudflareCookieManager.getCookies(verifyUrl)
            Log.d(TAG, "Verification - CFM cookies for $verifyUrl: ${cfmCookies.map { "${it.name}=${it.value.take(20)}" }}")
        }

        cm.flush()
        Log.d(TAG, "saveCookiesAndFinish: cookiesSaved=$cookiesSaved")

        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(RESULT_COOKIES_SAVED, cookiesSaved)
        })
        finish()
    }

    companion object {
        private const val TAG = "CloudflareResolver"
        const val EXTRA_URL = "extra_url"
        const val RESULT_COOKIES_SAVED = "result_cookies_saved"

        fun createIntent(context: Context, url: String): Intent {
            return Intent(context, CloudflareResolverActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
            }
        }
    }
}

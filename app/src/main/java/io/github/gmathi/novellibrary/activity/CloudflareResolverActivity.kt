package io.github.gmathi.novellibrary.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import io.github.gmathi.novellibrary.compose.cloudflare.CloudflareResolverScreen
import io.github.gmathi.novellibrary.network.HostNames

/**
 * Compose-based activity that presents a WebView for the user to manually solve
 * a Cloudflare challenge. Once the user completes the verification, cookies are
 * saved and the result is returned so the original API operation can be retried.
 */
class CloudflareResolverActivity : BaseActivity() {

    // Compose handles its own window insets
    override val skipWindowInsets: Boolean = true

    companion object {
        const val EXTRA_URL = "extra_url"
        const val RESULT_COOKIES_SAVED = "result_cookies_saved"

        fun createIntent(context: Context, url: String): Intent {
            return Intent(context, CloudflareResolverActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra(EXTRA_URL)
            ?: "https://${HostNames.NOVEL_UPDATES}"

        setContent {
            MaterialTheme {
                CloudflareResolverScreen(
                    url = url,
                    onComplete = { saveCookiesAndFinish(url) },
                    onBack = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }

    private fun saveCookiesAndFinish(originalUrl: String) {
        var cookiesSaved = false
        try {
            CookieManager.getInstance().flush()
            val cookieString = CookieManager.getInstance().getCookie(originalUrl)
            if (!cookieString.isNullOrEmpty()) {
                networkHelper.cookieManager.saveFromResponse(
                    originalUrl,
                    cookieString.split(";").map { it.trim() }
                )
                cookiesSaved = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(RESULT_COOKIES_SAVED, cookiesSaved)
        })
        finish()
    }
}

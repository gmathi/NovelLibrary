package io.github.gmathi.novellibrary.network.interceptor

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.model.source.online.HttpSource
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.lang.launchUI
import io.github.gmathi.novellibrary.util.system.toast
import io.github.gmathi.novellibrary.util.view.WebViewClientCompat
import io.github.gmathi.novellibrary.util.view.WebViewUtil
import io.github.gmathi.novellibrary.util.view.extensions.isOutdated
import io.github.gmathi.novellibrary.util.view.extensions.setDefaultSettings
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class CloudflareInterceptor(private val context: Context) : Interceptor {

    private val handler = Handler(Looper.getMainLooper())
    private val networkHelper: NetworkHelper by injectLazy()

    // Cache for tracking bypass attempts to avoid repeated failures
    private val bypassAttempts = mutableMapOf<String, Long>()
    private val retryDelay = 5000L // 5 seconds

    /**
     * When this is called, it initializes the WebView if it wasn't already. We use this to avoid
     * blocking the main thread too much. If used too often we could consider moving it to the
     * Application class.
     */
    private val initWebView by lazy {
        WebSettings.getDefaultUserAgent(context)
    }

    @Synchronized
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        if (!WebViewUtil.supportsWebView(context)) {
            launchUI {
                context.toast(R.string.information_webview_required, Toast.LENGTH_LONG)
            }
            return chain.proceed(originalRequest)
        }

        initWebView

        try {
            val response = chain.proceed(originalRequest)

            // Check if Cloudflare anti-bot is on
            if (!isCloudflareChallenge(response)) {
                return response
            }

            response.close()

            val host = originalRequest.url.host

            // If we already have a cf_clearance cookie from a previous bypass (e.g. a concurrent
            // request that already solved the challenge), just retry with that cookie.
            val existingClearance = networkHelper.cookieManager.get(originalRequest.url)
                .firstOrNull { it.name == "cf_clearance" }
            if (existingClearance != null) {
                return chain.proceed(originalRequest)
            }

            // Check if we should attempt bypass based on recent failures
            if (shouldSkipBypass(host)) {
                throw Exception(context.getString(R.string.information_cloudflare_bypass_failure))
            }

            networkHelper.cookieManager.remove(originalRequest.url, COOKIE_NAMES, 0)
            val oldCookie = networkHelper.cookieManager.get(originalRequest.url)
                .firstOrNull { it.name == "cf_clearance" }

            val bypassSuccess = resolveWithWebView(originalRequest, oldCookie)

            if (bypassSuccess) {
                recordBypassSuccess(host)
                return chain.proceed(originalRequest)
            } else {
                recordBypassFailure(host)
                throw Exception(context.getString(R.string.information_cloudflare_bypass_failure))
            }
        } catch (e: Exception) {
            // Because OkHttp's enqueue only handles IOExceptions, wrap the exception so that
            // we don't crash the entire app
            return chain.proceed(originalRequest)
        }
    }

    /**
     * Enhanced Cloudflare detection supporting multiple server headers and response patterns
     */
    private fun isCloudflareChallenge(response: Response): Boolean {
        if (response.code != 503) return false
        
        val server = response.header("Server")?.lowercase()
        if (server in SERVER_CHECK) return true
        
        // Additional checks for Cloudflare presence
        val cfRay = response.header("CF-RAY")
        val cfCacheStatus = response.header("CF-Cache-Status")
        
        return cfRay != null || cfCacheStatus != null
    }

    /**
     * Check if we should skip bypass attempt based on recent failures
     */
    private fun shouldSkipBypass(host: String): Boolean {
        val lastAttempt = bypassAttempts[host] ?: return false
        val timeSinceLastAttempt = System.currentTimeMillis() - lastAttempt
        return timeSinceLastAttempt < retryDelay
    }

    /**
     * Record successful bypass attempt
     */
    private fun recordBypassSuccess(host: String) {
        bypassAttempts.remove(host)
    }

    /**
     * Record failed bypass attempt
     */
    private fun recordBypassFailure(host: String) {
        bypassAttempts[host] = System.currentTimeMillis()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun resolveWithWebView(request: Request, oldCookie: Cookie?): Boolean {
        // We need to lock this thread until the WebView finds the challenge solution url, because
        // OkHttp doesn't support asynchronous interceptors.
        val latch = CountDownLatch(1)

        var webView: WebView? = null

        var challengeFound = false
        var cloudflareBypassed = false
        var isWebViewOutdated = false

        val origRequestUrl = request.url.toString()
        val headers = request.headers.toMultimap().mapValues { it.value.getOrNull(0) ?: "" }.toMutableMap()
        headers["X-Requested-With"] = WebViewUtil.REQUESTED_WITH

        handler.post {
            val webview = WebView(context)
            webView = webview
            webview.setDefaultSettings()

            // Avoid sending empty User-Agent, Chromium WebView will reset to default if empty
            webview.settings.userAgentString = request.header("User-Agent")
                ?: HttpSource.DEFAULT_USER_AGENT
            
            // Enhanced WebView settings for better Cloudflare bypass
            webview.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
            }

            webview.webViewClient = object : WebViewClientCompat() {
                override fun onPageFinished(view: WebView, url: String) {
                    fun isCloudFlareBypassed(): Boolean {
                        return networkHelper.cookieManager.get(origRequestUrl.toHttpUrl())
                            .firstOrNull { it.name == "cf_clearance" }
                            .let { it != null && it != oldCookie }
                    }

                    if (isCloudFlareBypassed()) {
                        cloudflareBypassed = true
                        latch.countDown()
                    }

                    if (url == origRequestUrl && !challengeFound) {
                        // The first request didn't return the challenge, abort.
                        latch.countDown()
                    }
                }

                override fun onReceivedErrorCompat(
                    view: WebView,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String,
                    isMainFrame: Boolean
                ) {
                    if (isMainFrame) {
                        if (errorCode in CLOUDFLARE_ERROR_CODES) {
                            // Found the Cloudflare challenge page.
                            challengeFound = true
                        } else {
                            // Unlock thread, the challenge wasn't found.
                            latch.countDown()
                        }
                    }
                }
            }

            webView?.loadUrl(origRequestUrl, headers)
        }

        // Wait a reasonable amount of time to retrieve the solution. The minimum should be
        // around 4 seconds but it can take more due to slow networks or server issues.
        // Increased timeout for more complex challenges
        latch.await(15, TimeUnit.SECONDS)

        handler.post {
            if (!cloudflareBypassed) {
                isWebViewOutdated = webView?.isOutdated() == true
            }

            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }

        // Prompt user to update WebView if it seems too outdated
        if (!cloudflareBypassed && isWebViewOutdated) {
            launchUI {
                context.toast(R.string.information_webview_outdated, Toast.LENGTH_LONG)
            }
        }

        return cloudflareBypassed
    }

    companion object {
        private val SERVER_CHECK = arrayOf("cloudflare-nginx", "cloudflare", "cf-ray")
        private val COOKIE_NAMES = listOf("cf_clearance", "__cf_bm", "cf_chl_2", "cf_chl_prog")
        private val CLOUDFLARE_ERROR_CODES = setOf(503, 403, 429)
    }
}

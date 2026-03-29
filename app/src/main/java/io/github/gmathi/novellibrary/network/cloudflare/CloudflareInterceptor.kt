package io.github.gmathi.novellibrary.network.cloudflare

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
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
        Log.d(TAG, "intercept: ${originalRequest.url}")

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

            Log.d(TAG, "Cloudflare challenge detected for ${originalRequest.url} (code=${response.code})")
            response.close()

            val host = originalRequest.url.host

            // If we already have a cf_clearance cookie from a previous bypass (e.g. a concurrent
            // request that already solved the challenge), just retry with that cookie.
            val jarClearance = networkHelper.cookieManager.get(originalRequest.url)
                .firstOrNull { it.name == "cf_clearance" }
            val cfmClearance = networkHelper.cloudflareCookieManager.getClearanceCookie(originalRequest.url)
            val existingClearance = jarClearance ?: cfmClearance
            Log.d(TAG, "Existing clearance for $host: jar=${jarClearance?.value?.take(20)}, cfm=${cfmClearance?.value?.take(20)}")

            if (existingClearance != null) {
                // Ensure the cookie is also in the AndroidCookieJar so OkHttp sends it
                syncCloudflareCookiesToJar(originalRequest.url)
                Log.d(TAG, "Retrying with existing clearance for $host")
                return chain.proceed(originalRequest)
            }

            // For non-HTML resource requests (images, fonts, etc.), don't attempt a WebView
            // bypass — it's expensive and unlikely to succeed. Just return the blocked response
            // so the caller (e.g. Coil/Glide) can handle the failure gracefully.
            // The bypass will happen on the next page/API request instead.
            val path = originalRequest.url.encodedPath.lowercase()
            val isResourceRequest = RESOURCE_EXTENSIONS.any { path.endsWith(it) }
            if (isResourceRequest) {
                Log.d(TAG, "Skipping bypass for resource request: $path")
                // Return the 403 directly — don't waste time on WebView bypass for images
                return chain.proceed(originalRequest)
            }

            // Check if we should attempt bypass based on recent failures
            if (shouldSkipBypass(host)) {
                Log.d(TAG, "Skipping bypass for $host (recent failure)")
                throw Exception(context.getString(R.string.information_cloudflare_bypass_failure))
            }

            // Use the base domain URL for WebView bypass instead of the original URL.
            // This ensures the WebView loads a proper HTML page where Cloudflare's JS
            // challenge can execute.
            val baseUrl = "${originalRequest.url.scheme}://${originalRequest.url.host}/"
            val bypassRequest = originalRequest.newBuilder().url(baseUrl).build()

            networkHelper.cookieManager.remove(baseUrl.toHttpUrl(), COOKIE_NAMES, 0)
            val oldCookie = networkHelper.cookieManager.get(baseUrl.toHttpUrl())
                .firstOrNull { it.name == "cf_clearance" }

            Log.d(TAG, "Attempting WebView bypass for $host (baseUrl=$baseUrl)")
            val bypassSuccess = resolveWithWebView(bypassRequest, oldCookie)
            Log.d(TAG, "WebView bypass result for $host: $bypassSuccess")

            if (bypassSuccess) {
                recordBypassSuccess(host)
                // Store the new cookies in the per-host CloudflareCookieManager
                storeCloudflareCookies(originalRequest.url)
                // Log all cookies after bypass
                val allCookies = networkHelper.cookieManager.get(originalRequest.url)
                Log.d(TAG, "Cookies after bypass for $host: ${allCookies.map { "${it.name}=${it.value.take(20)}" }}")
                return chain.proceed(originalRequest)
            } else {
                recordBypassFailure(host)
                Log.d(TAG, "WebView bypass failed for $host")
                throw Exception(context.getString(R.string.information_cloudflare_bypass_failure))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cloudflare intercept error for ${originalRequest.url}: ${e.message}")
            // Because OkHttp's enqueue only handles IOExceptions, wrap the exception so that
            // we don't crash the entire app
            return chain.proceed(originalRequest)
        }
    }

    /**
     * Enhanced Cloudflare detection supporting multiple server headers and response patterns.
     * Detects both classic 503 challenges and newer 403 challenges with cf-mitigated header.
     */
    private fun isCloudflareChallenge(response: Response): Boolean {
        // Newer Cloudflare challenges return 403 with cf-mitigated: challenge
        if (response.code == 403) {
            val cfMitigated = response.header("cf-mitigated")
            if (cfMitigated?.contains("challenge", ignoreCase = true) == true) return true
        }

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

                    // If the page finished loading and no challenge was found, abort.
                    // Don't compare URLs strictly — Cloudflare may redirect.
                    if (!challengeFound && !cloudflareBypassed) {
                        // Give it a moment for JS to execute before giving up
                        view.postDelayed({
                            if (!cloudflareBypassed && isCloudFlareBypassed()) {
                                cloudflareBypassed = true
                            }
                            if (!cloudflareBypassed && !challengeFound) {
                                latch.countDown()
                            }
                        }, 2000)
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

    /**
     * Store Cloudflare cookies from the AndroidCookieJar into the per-host
     * CloudflareCookieManager after a successful bypass.
     */
    private fun storeCloudflareCookies(url: okhttp3.HttpUrl) {
        val cookies = networkHelper.cookieManager.get(url)
        val cfCookies = cookies.filter { CloudflareCookieManager.isCloudflareCookie(it) }
        if (cfCookies.isNotEmpty()) {
            networkHelper.cloudflareCookieManager.storeCookies(url, cfCookies)
        }
    }

    /**
     * Sync Cloudflare cookies from the per-host CloudflareCookieManager back into
     * the AndroidCookieJar so OkHttp includes them in the retry request.
     */
    private fun syncCloudflareCookiesToJar(url: okhttp3.HttpUrl) {
        val cfCookies = networkHelper.cloudflareCookieManager.getCookies(url)
        if (cfCookies.isNotEmpty()) {
            networkHelper.cookieManager.saveFromResponse(url, cfCookies)
        }
    }

    companion object {
        private const val TAG = "CloudflareInterceptor"
        private val SERVER_CHECK = arrayOf("cloudflare-nginx", "cloudflare", "cf-ray")
        private val COOKIE_NAMES = listOf("cf_clearance", "__cf_bm", "cf_chl_2", "cf_chl_prog")
        private val CLOUDFLARE_ERROR_CODES = setOf(503, 403, 429)
        private val RESOURCE_EXTENSIONS = arrayOf(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg", ".ico",
            ".css", ".js", ".woff", ".woff2", ".ttf", ".eot", ".otf",
            ".mp3", ".mp4", ".webm", ".ogg", ".pdf"
        )
    }
}

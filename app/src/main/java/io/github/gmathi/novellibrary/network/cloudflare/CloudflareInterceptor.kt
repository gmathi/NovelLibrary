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
import io.github.gmathi.novellibrary.model.preference.DataCenter
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
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class CloudflareInterceptor(private val context: Context) : Interceptor {

    private val handler = Handler(Looper.getMainLooper())
    private val networkHelper: NetworkHelper by injectLazy()
    private val dataCenter: DataCenter by injectLazy()
    private val webViewFetcher = WebViewFetcher(context)

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
        Log.d(TAG, "intercept: cookies being sent: ${networkHelper.cookieManager.get(originalRequest.url).map { "${it.name}=${it.value.take(20)}" }}")

        if (!WebViewUtil.supportsWebView(context)) {
            launchUI {
                context.toast(R.string.information_webview_required, Toast.LENGTH_LONG)
            }
            return chain.proceed(originalRequest)
        }

        initWebView

        val host = originalRequest.url.host

        // Per-domain forced-WebView path. If this host has previously failed the API path
        // twice (and the flag hasn't self-healed after 15 days), skip OkHttp entirely and
        // serve via the WebView fetcher — unless this is a resource request (images/fonts),
        // which the WebView fetch can't help with.
        run {
            val forcedPath = originalRequest.url.encodedPath.lowercase()
            val forcedIsResource = RESOURCE_EXTENSIONS.any { forcedPath.endsWith(it) }
            if (!forcedIsResource && dataCenter.isWebViewForced(host)) {
                Log.d(TAG, "Host $host is forced to WebView fetcher; skipping API path")
                CloudflareProgress.post("Using in-app browser fetch for $host…")
                return runWebViewFetch(originalRequest, host)
            }
        }

        try {
            val response: Response
            try {
                response = chain.proceed(originalRequest)
            } catch (e: java.net.UnknownHostException) {
                Log.w(TAG, "DNS resolution failed for ${originalRequest.url.host}: ${e.message}")
                throw java.io.IOException("DNS resolution failed for ${originalRequest.url.host}", e)
            }


            // Check if Cloudflare anti-bot is on
            if (!isCloudflareChallenge(response)) {
                return response
            }

            Log.d(TAG, "Cloudflare challenge detected for ${originalRequest.url} (code=${response.code})")
            response.close()

            // For non-HTML resource requests (images, fonts, etc.) we won't attempt a bypass
            // (handled below), so don't surface progress for them either — keep the status
            // text about the actual page/chapter request the user is waiting on.
            val challengePath = originalRequest.url.encodedPath.lowercase()
            val isChallengeOnResource = RESOURCE_EXTENSIONS.any { challengePath.endsWith(it) }
            if (!isChallengeOnResource) {
                CloudflareProgress.post("Cloudflare challenge detected on $host…")
            }

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
                if (!isChallengeOnResource) CloudflareProgress.post("Retrying with saved verification…")
                val retryResponse = chain.proceed(originalRequest)
                Log.d(TAG, "Retry with existing clearance result: code=${retryResponse.code}, url=${originalRequest.url}")
                if (isCloudflareChallenge(retryResponse)) {
                    Log.w(TAG, "Retry with existing clearance STILL got Cloudflare challenge (TLS fingerprint mismatch likely). Forcing WebView fetch for $host")
                    retryResponse.close()
                    if (!isChallengeOnResource) CloudflareProgress.post("Falling back to in-app browser fetch…")
                    // Stale clearance didn't work via OkHttp — clear it, force this host onto
                    // the WebView fetcher, and serve via the unified path (method-aware; raises
                    // the manual-verify handoff if the WebView itself is still challenged).
                    networkHelper.cloudflareCookieManager.clearCookiesAllVariants(originalRequest.url)
                    networkHelper.cookieManager.remove(originalRequest.url, COOKIE_NAMES, 0)
                    dataCenter.flagWebViewForced(host)
                    return runWebViewFetch(originalRequest, host)
                } else {
                    return retryResponse
                }
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
                Log.d(TAG, "Skipping headless bypass for $host (recent failure); forcing WebView fetcher")
                // A fresh headless solve just failed moments ago — don't hammer it again.
                // Force this host onto the WebView fetcher and serve this request via it.
                dataCenter.flagWebViewForced(host)
                return runWebViewFetch(originalRequest, host)
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
            CloudflareProgress.post("Verifying with Cloudflare…")
            val bypassSuccess = resolveWithWebView(bypassRequest, oldCookie)
            Log.d(TAG, "WebView bypass result for $host: $bypassSuccess")

            if (bypassSuccess) {
                recordBypassSuccess(host)
                CloudflareProgress.post("Verified — loading…")
                // Store the new cookies in the per-host CloudflareCookieManager
                storeCloudflareCookies(originalRequest.url)
                // Log all cookies after bypass
                val allCookies = networkHelper.cookieManager.get(originalRequest.url)
                Log.d(TAG, "Cookies after bypass for $host: ${allCookies.map { "${it.name}=${it.value.take(20)}" }}")
                // Second API trial: retry OkHttp now that validation stored fresh clearance.
                val secondTrial = chain.proceed(originalRequest)
                if (!isCloudflareChallenge(secondTrial)) {
                    return secondTrial
                }
                // Second API trial still blocked (TLS fingerprint mismatch): force this host
                // onto the WebView fetcher for subsequent downloads, and serve this one via it.
                Log.w(TAG, "Second API trial still challenged for $host; forcing WebView fetcher")
                secondTrial.close()
                dataCenter.flagWebViewForced(host)
                return runWebViewFetch(originalRequest, host)
            } else {
                recordBypassFailure(host)
                Log.d(TAG, "WebView bypass failed for $host; forcing WebView fetcher and falling back")
                // The API path has failed validation: force this host onto the WebView fetcher
                // for subsequent downloads (self-heals after 15 days), and serve this request
                // via it now. This matches the "second trial" terminal state of the chain.
                dataCenter.flagWebViewForced(host)
                return runWebViewFetch(originalRequest, host)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cloudflare intercept error for ${originalRequest.url}: ${e.message}")
            // Because OkHttp's enqueue only handles IOExceptions, wrap the exception so that
            // we don't crash the entire app.
            // Re-throw as IOException so callers (like DownloadWebPageThread) can detect the
            // failure instead of silently receiving a Cloudflare challenge page.
            throw java.io.IOException("$BYPASS_FAILED_PREFIX${originalRequest.url}: ${e.message}", e)
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
            Log.d(TAG, "isCloudflareChallenge: 403 response, cf-mitigated=$cfMitigated, server=${response.header("Server")}, url=${response.request.url}")
            if (cfMitigated?.contains("challenge", ignoreCase = true) == true) return true
        }

        if (response.code != 503 && response.code != 403) {
            Log.d(TAG, "isCloudflareChallenge: response code=${response.code}, NOT a challenge")
            return false
        }

        if (response.code == 503) {
            val server = response.header("Server")?.lowercase()
            Log.d(TAG, "isCloudflareChallenge: 503 response, server=$server, url=${response.request.url}")
            if (server in SERVER_CHECK) return true
        }
        
        // Additional checks for Cloudflare presence
        val cfRay = response.header("CF-RAY")
        val cfCacheStatus = response.header("CF-Cache-Status")
        Log.d(TAG, "isCloudflareChallenge: CF-RAY=$cfRay, CF-Cache-Status=$cfCacheStatus")
        
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

        fun isCloudFlareBypassed(): Boolean {
            return networkHelper.cookieManager.get(origRequestUrl.toHttpUrl())
                .firstOrNull { it.name == "cf_clearance" }
                .let { it != null && it != oldCookie }
        }

        // Cloudflare "managed" challenges (the "Just a moment..." interstitial) don't
        // navigate away or re-trigger onPageFinished once loaded. Instead, they run an
        // orchestration script (loaded from /cdn-cgi/challenge-platform/.../orchestrate/chl_page)
        // that asynchronously talks to challenges.cloudflare.com, scores the client, and
        // only *then* sets cf_clearance via a background request — all without any further
        // page navigation. A single one-shot check a couple seconds after onPageFinished
        // (the previous approach) frequently fires before that orchestration completes.
        // Poll periodically for the cookie instead, for the full duration of the timeout.
        val pollIntervalMs = 1000L
        val pollRunnable = object : Runnable {
            override fun run() {
                if (cloudflareBypassed) return
                if (isCloudFlareBypassed()) {
                    cloudflareBypassed = true
                    latch.countDown()
                    return
                }
                handler.postDelayed(this, pollIntervalMs)
            }
        }

        handler.post {
            val webview = WebView(context)
            webView = webview
            webview.setDefaultSettings()

            webview.settings.userAgentString = HttpSource.userAgent()
            
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
                    if (isCloudFlareBypassed()) {
                        cloudflareBypassed = true
                        latch.countDown()
                        return
                    }
                    // Start (or keep) polling for the clearance cookie while the challenge
                    // orchestration script runs in the background.
                    handler.removeCallbacks(pollRunnable)
                    handler.postDelayed(pollRunnable, pollIntervalMs)
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
                            // Found the Cloudflare challenge page. Keep waiting/polling rather
                            // than giving up immediately — the challenge may still resolve.
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

        // Wait a short amount of time to retrieve the solution before giving up and falling
        // back to the manual "Resolve Cloudflare" flow.
        latch.await(5, TimeUnit.SECONDS)

        handler.post {
            // Stop the cookie-polling loop before tearing down the WebView.
            handler.removeCallbacks(pollRunnable)

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

    /**
     * Serve [request] via the WebView fetcher (POST uses the same-origin JS fetch; GET loads
     * the page). If the WebView itself can't clear a Cloudflare challenge, surface it as a
     * [BYPASS_FAILED_PREFIX] IOException carrying the gated URL so the UI layer routes the
     * user to the manual resolver and resumes afterward.
     */
    private fun runWebViewFetch(request: Request, host: String): Response {
        return try {
            val response =
                if (request.method.equals("POST", ignoreCase = true)) webViewFetcher.fetchPost(request)
                else webViewFetcher.fetch(request)
            // A WebView POST can still come back as a challenge body (fetchPost returns it with
            // real headers). Treat that as a challenge needing manual verification.
            if (isCloudflareChallenge(response)) {
                response.close()
                CloudflareProgress.post("Manual Cloudflare verification needed for $host…")
                throw java.io.IOException(
                    "$BYPASS_FAILED_PREFIX${request.url}: WebView fetch returned a Cloudflare challenge"
                )
            }
            recordBypassSuccess(host)
            response
        } catch (e: CloudflareWebViewChallengeException) {
            Log.w(TAG, "WebView fetch hit a Cloudflare challenge for $host: ${e.gatedUrl}")
            CloudflareProgress.post("Manual Cloudflare verification needed for $host…")
            // Reuse the established recovery contract: callers detect BYPASS_FAILED_PREFIX and
            // open CloudflareResolverActivity, then retry.
            throw java.io.IOException("$BYPASS_FAILED_PREFIX${e.gatedUrl}: ${e.message}", e)
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

        /**
         * Prefix used when wrapping a Cloudflare bypass failure into an IOException, followed
         * immediately by the exact request URL that was gated. Kept as a shared constant so
         * callers can reliably recover the real gated URL (e.g. a NovelUpdates search-finder
         * URL) instead of falling back to a source's bare base URL, which is a different
         * request that may never have needed a challenge at all.
         */
        const val BYPASS_FAILED_PREFIX = "Cloudflare bypass failed for "

        /**
         * Extracts the gated request URL from a [Throwable] thrown by this interceptor, if
         * present. Returns null if [error]'s message doesn't match the expected format (e.g.
         * it originated elsewhere).
         */
        fun extractGatedUrl(error: Throwable?): String? {
            val message = error?.message ?: return null
            if (!message.startsWith(BYPASS_FAILED_PREFIX)) return null
            val remainder = message.removePrefix(BYPASS_FAILED_PREFIX)
            val urlEnd = remainder.indexOf(": ")
            val url = if (urlEnd >= 0) remainder.substring(0, urlEnd) else remainder
            return url.trim().takeIf { it.isNotEmpty() }
        }

        /** The host of [url], or null if it can't be parsed. */
        fun hostOf(url: String?): String? =
            url?.toHttpUrlOrNull()?.host

        /**
         * Call after the user SUCCESSFULLY completes manual Cloudflare verification for [url].
         *
         * Step 1 of the post-verify sequence: clear the domain's forced-WebView flag so the
         * API path is retried. Steps 2–3 (retry the API fetch; on failure re-flag + fall back
         * to the WebView fetch) happen automatically when the caller re-issues the request —
         * it re-enters [intercept], which with the flag cleared takes the API path first and,
         * if it fails validation twice, re-flags the host and serves via the WebView fetcher
         * (the same terminal path as the original second trial).
         *
         * No-op when [cookiesSaved] is false (the user backed out without solving), so a
         * cancelled resolver never wrongly resets a domain.
         */
        fun onManualVerificationComplete(url: String?, cookiesSaved: Boolean) {
            if (!cookiesSaved) return
            val host = hostOf(url) ?: return
            val dc: DataCenter by injectLazy()
            dc.clearWebViewForced(host)
            Log.d(TAG, "Manual verification complete for $host; cleared forced-WebView flag, API path will be retried")
        }
    }
}

package io.github.gmathi.novellibrary.network.cloudflare

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import io.github.gmathi.novellibrary.model.source.online.HttpSource
import io.github.gmathi.novellibrary.util.view.extensions.setDefaultSettings
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Fetches a URL via WebView to bypass Cloudflare TLS fingerprinting.
 *
 * Cloudflare's cf_clearance cookie is bound to the TLS fingerprint of the client.
 * OkHttp (Java SSLSocket) and WebView (Chromium BoringSSL) have different fingerprints,
 * so a cookie obtained in the WebView won't work in OkHttp and vice versa.
 *
 * This class loads the request URL in a WebView (which shares the same TLS fingerprint
 * as the manual Cloudflare resolution WebView), waits for the page to load, then extracts
 * the HTML content via JavaScript.
 */
class WebViewFetcher(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "WebViewFetcher"
        private const val TIMEOUT_SECONDS = 5L
        private const val CHALLENGE_POLL_INTERVAL_MS = 1000L
        private const val CHALLENGE_MAX_WAIT_MS = 4_000L
        private const val JS_BRIDGE = "AndroidFetch"
    }

    /**
     * Fetch the given OkHttp [Request] using a WebView, returning a synthetic [Response]
     * containing the page HTML. The WebView will automatically use any cf_clearance cookies
     * stored in Android's CookieManager.
     *
     * Must NOT be called from the main thread.
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun fetch(request: Request): Response {
        val url = request.url.toString()
        return load(request, "fetch") { wv, headers ->
            wv.loadUrl(url, headers)
        }
    }

    /**
     * Fetch the given OkHttp POST [Request] using a WebView, returning a synthetic [Response]
     * with the response body. Runs a same-origin `fetch()` inside the WebView's JS context
     * (after navigating to the target's own origin) so the request rides the WebView's TLS
     * fingerprint and cf_clearance cookie while honoring the real method, headers and body —
     * unlike [WebView.postUrl], which hardcodes application/x-www-form-urlencoded and drops
     * all per-request headers. Same-origin means no CORS preflight.
     *
     * Must NOT be called from the main thread.
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun fetchPost(request: Request): Response {
        val bodyString = try {
            val buffer = Buffer()
            request.body?.writeTo(buffer)
            buffer.readUtf8()
        } catch (e: Exception) {
            throw java.io.IOException("Failed to read POST body for ${request.url}: ${e.message}", e)
        }
        val contentType = request.body?.contentType()?.toString()
        return loadWithJsFetch(request, bodyString, contentType)
    }

    /**
     * Navigate to the request's origin, then run a same-origin `fetch()` for the actual
     * request (method/headers/body) and return the response text. Forbidden request headers
     * (Cookie, Origin, Host, Content-Length, Connection, TE) are dropped — the browser sets
     * those itself; attempting to set them from fetch is a no-op or an error.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun loadWithJsFetch(
        request: Request,
        body: String?,
        contentType: String?
    ): Response {
        val url = request.url.toString()
        val origin = "${request.url.scheme}://${request.url.host}/"
        Log.d(TAG, "fetchPost: navigating to origin $origin then fetching $url")

        val headersJson = buildFetchHeadersJson(request, contentType)
        val fetchScript = buildFetchScript(url, request.method, headersJson, body)

        val latch = CountDownLatch(1)
        var responseBody: String? = null
        var responseStatus = 0
        var loadError: String? = null
        var webView: WebView? = null
        var fetchStarted = false

        val bridge = object {
            @android.webkit.JavascriptInterface
            fun onResult(status: Int, body: String) {
                responseStatus = status
                responseBody = body
                Log.d(TAG, "fetchPost: status=$status, length=${body.length}")
                latch.countDown()
            }

            @android.webkit.JavascriptInterface
            fun onError(message: String) {
                loadError = message
                latch.countDown()
            }
        }

        fun runFetch(view: WebView) {
            if (fetchStarted) return
            fetchStarted = true
            view.evaluateJavascript(fetchScript, null)
        }

        handler.post {
            val wv = WebView(context)
            webView = wv
            wv.setDefaultSettings()
            wv.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                @Suppress("DEPRECATION")
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                userAgentString = HttpSource.userAgent()
            }
            wv.addJavascriptInterface(bridge, JS_BRIDGE)
            wv.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, finishedUrl: String) {
                    Log.d(TAG, "fetchPost onPageFinished: $finishedUrl")
                    runFetch(view)
                }

                @Deprecated("Deprecated in Java")
                override fun onReceivedError(
                    view: WebView,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    // Only abort on the main-frame origin document failing to load; ignore
                    // benign subresource errors (ads, trackers) on the same origin.
                    if (failingUrl == origin) {
                        Log.e(TAG, "fetchPost onReceivedError: code=$errorCode, desc=$description, url=$failingUrl")
                        loadError = "WebView error $errorCode: $description"
                        latch.countDown()
                    }
                }
            }
            wv.loadUrl(origin)
        }

        val completed = latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        handler.post {
            webView?.removeJavascriptInterface(JS_BRIDGE)
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }

        if (!completed) {
            Log.e(TAG, "fetchPost: timeout after ${TIMEOUT_SECONDS}s for $url")
            throw java.io.IOException("WebView POST timed out for $url")
        }
        if (loadError != null) {
            throw java.io.IOException("WebView POST failed for $url: $loadError")
        }
        val respBody = responseBody
            ?: throw java.io.IOException("WebView POST returned no content for $url")

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(if (responseStatus in 100..599) responseStatus else 200)
            .message("OK")
            .headers(Headers.Builder().add("Content-Type", contentType ?: "text/html; charset=utf-8").build())
            .body(respBody.toResponseBody((contentType ?: "text/html; charset=utf-8").toMediaType()))
            .build()
    }

    /**
     * Build a JSON object literal of allowed request headers for the JS fetch init. Forbidden
     * header names (set by the browser itself) are skipped.
     */
    private fun buildFetchHeadersJson(request: Request, contentType: String?): String {
        val forbidden = setOf(
            "cookie", "origin", "host", "content-length", "connection",
            "te", "referer", "user-agent", "accept-encoding"
        )
        val entries = mutableListOf<String>()
        request.headers.forEach { (name, value) ->
            if (name.lowercase() !in forbidden) {
                entries += "${jsString(name)}:${jsString(value)}"
            }
        }
        if (contentType != null && request.header("Content-Type") == null) {
            entries += "${jsString("Content-Type")}:${jsString(contentType)}"
        }
        return entries.joinToString(",", "{", "}")
    }

    /**
     * Build the async fetch script. The result is pushed to the Kotlin side through the
     * [JS_BRIDGE] JavascriptInterface as typed args (status + body), so there is no string
     * sentinel to encode and no large HTML body to unescape — [WebView.evaluateJavascript]
     * cannot await a Promise, and a stringified result corrupts on big/quoted bodies.
     */
    private fun buildFetchScript(url: String, method: String, headersJson: String, body: String?): String {
        val bodyLiteral = if (body == null) "null" else jsString(body)
        return """
            (function() {
                (async function() {
                    try {
                        var resp = await fetch(${jsString(url)}, {
                            method: ${jsString(method)},
                            headers: $headersJson,
                            body: $bodyLiteral,
                            credentials: 'include'
                        });
                        var text = await resp.text();
                        $JS_BRIDGE.onResult(resp.status, text);
                    } catch (e) {
                        $JS_BRIDGE.onError(e && e.message ? e.message : String(e));
                    }
                })();
            })();
        """.trimIndent()
    }

    /** JSON-encode a string for safe inlining into a JS literal. */
    private fun jsString(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\u0001' -> sb.append("\\u0001")
                else -> if (c < ' ') sb.append("\\u%04x".format(c.code)) else sb.append(c)
            }
        }
        return sb.append("\"").toString()
    }

    /**
     * Shared WebView load/poll/extract flow used by both [fetch] and [fetchPost]. [startLoad]
     * is called on the main thread with the configured [WebView] and the request's headers
     * (as a map, for callers that can use them) and is responsible for kicking off the
     * navigation (via `loadUrl` or `postUrl`).
     */
    private fun load(
        request: Request,
        logLabel: String,
        startLoad: (WebView, Map<String, String>) -> Unit
    ): Response {
        val url = request.url.toString()
        Log.d(TAG, "$logLabel: loading $url via WebView")

        val latch = CountDownLatch(1)
        var htmlContent: String? = null
        var webView: WebView? = null
        var loadError: String? = null

        // Extracts HTML from the page once we're confident it's no longer a Cloudflare
        // challenge interstitial.
        fun extractHtml(view: WebView) {
            view.evaluateJavascript(
                "(function() { return document.documentElement.outerHTML; })()"
            ) { result ->
                if (result != null && result != "null") {
                    htmlContent = unescapeJsString(result)
                    Log.d(TAG, "$logLabel: got HTML content, length=${htmlContent!!.length}")
                } else {
                    Log.w(TAG, "$logLabel: evaluateJavascript returned null")
                    loadError = "Failed to extract page content"
                }
                latch.countDown()
            }
        }

        // Cloudflare's "managed" challenge page (title "Just a moment...") never triggers
        // a further onPageFinished once loaded — it resolves asynchronously in the
        // background (an orchestration script talks to challenges.cloudflare.com and only
        // then sets cf_clearance). A single check right after onPageFinished can catch the
        // interstitial before that finishes. Poll the page title/cookie state instead of
        // giving up (or extracting the interstitial HTML) immediately.
        val pollRunnable = object : Runnable {
            var elapsedMs = 0L
            override fun run() {
                val view = webView ?: return
                view.evaluateJavascript(
                    "(function() { return document.title; })()"
                ) { title ->
                    val pageTitle = title?.trim('"') ?: ""
                    val isChallengePage = pageTitle.contains("Just a moment", ignoreCase = true) ||
                            pageTitle.contains("Attention Required", ignoreCase = true) ||
                            pageTitle.contains("Checking your browser", ignoreCase = true)
                    Log.d(TAG, "pollRunnable: title='$pageTitle', isChallengePage=$isChallengePage, elapsedMs=$elapsedMs")

                    if (!isChallengePage) {
                        extractHtml(view)
                        return@evaluateJavascript
                    }

                    elapsedMs += CHALLENGE_POLL_INTERVAL_MS
                    if (elapsedMs >= CHALLENGE_MAX_WAIT_MS) {
                        Log.w(TAG, "$logLabel: still on challenge page after ${elapsedMs}ms, giving up")
                        loadError = "Still on Cloudflare challenge page after waiting"
                        latch.countDown()
                        return@evaluateJavascript
                    }
                    handler.postDelayed(this, CHALLENGE_POLL_INTERVAL_MS)
                }
            }
        }

        handler.post {
            val wv = WebView(context)
            webView = wv
            wv.setDefaultSettings()
            wv.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                @Suppress("DEPRECATION")
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                userAgentString = HttpSource.userAgent()
            }

            wv.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, finishedUrl: String) {
                    Log.d(TAG, "onPageFinished: $finishedUrl")

                    // Check if this is still a Cloudflare challenge page
                    val cookies = CookieManager.getInstance().getCookie(finishedUrl)
                    val hasClearance = cookies?.contains("cf_clearance") == true
                    Log.d(TAG, "onPageFinished: hasClearance=$hasClearance")

                    handler.removeCallbacks(pollRunnable)
                    pollRunnable.elapsedMs = 0L
                    pollRunnable.run()
                }

                @Deprecated("Deprecated in Java")
                override fun onReceivedError(
                    view: WebView,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    Log.e(TAG, "onReceivedError: code=$errorCode, desc=$description, url=$failingUrl")
                    loadError = "WebView error $errorCode: $description"
                    handler.removeCallbacks(pollRunnable)
                    latch.countDown()
                }
            }

            val headers = mutableMapOf<String, String>()
            request.headers.forEach { (name, value) -> headers[name] = value }
            startLoad(wv, headers)
        }

        // Wait for the page to load
        val completed = latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        // Clean up WebView
        handler.post {
            handler.removeCallbacks(pollRunnable)
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }

        if (!completed) {
            Log.e(TAG, "$logLabel: timeout after ${TIMEOUT_SECONDS}s for $url")
            throw java.io.IOException("WebView fetch timed out for $url")
        }

        if (loadError != null) {
            throw java.io.IOException("WebView fetch failed for $url: $loadError")
        }

        val body = htmlContent ?: throw java.io.IOException("WebView fetch returned no content for $url")

        // Build a synthetic OkHttp Response
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .headers(Headers.Builder().add("Content-Type", "text/html; charset=utf-8").build())
            .body(body.toResponseBody("text/html; charset=utf-8".toMediaType()))
            .build()
    }

    /**
     * Unescape a JavaScript string result from evaluateJavascript.
     * The result comes wrapped in quotes with escape sequences.
     */
    private fun unescapeJsString(jsString: String): String {
        // Remove surrounding quotes
        var s = jsString
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length - 1)
        }
        // Unescape common sequences
        return s
            .replace("\\\\", "\u0000") // temp placeholder for literal backslash
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\'", "'")
            .replace("\\u003C", "<")
            .replace("\\u003c", "<")
            .replace("\\u003E", ">")
            .replace("\\u003e", ">")
            .replace("\\u0026", "&")
            .replace("\\u003D", "=")
            .replace("\\u003d", "=")
            .replace("\\u0027", "'")
            .replace("\\u0022", "\"")
            .replace("\\/", "/")
            .replace("\u0000", "\\") // restore literal backslash
    }
}

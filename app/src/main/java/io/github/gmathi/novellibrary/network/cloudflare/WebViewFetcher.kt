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
        private const val TIMEOUT_SECONDS = 30L
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
        Log.d(TAG, "fetch: loading $url via WebView")

        val latch = CountDownLatch(1)
        var htmlContent: String? = null
        var pageUrl: String? = null
        var webView: WebView? = null
        var loadError: String? = null

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
                userAgentString = HttpSource.DEFAULT_USER_AGENT
            }

            wv.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, finishedUrl: String) {
                    Log.d(TAG, "onPageFinished: $finishedUrl")
                    pageUrl = finishedUrl

                    // Check if this is still a Cloudflare challenge page
                    val cookies = CookieManager.getInstance().getCookie(finishedUrl)
                    val hasClearance = cookies?.contains("cf_clearance") == true
                    Log.d(TAG, "onPageFinished: hasClearance=$hasClearance")

                    // Check page title to detect if we landed on a challenge page
                    view.evaluateJavascript(
                        "(function() { return document.title; })()"
                    ) { title ->
                        val pageTitle = title?.trim('"') ?: ""
                        val isChallengePage = pageTitle.contains("Just a moment", ignoreCase = true) ||
                                pageTitle.contains("Attention Required", ignoreCase = true) ||
                                pageTitle.contains("Checking your browser", ignoreCase = true)
                        Log.d(TAG, "onPageFinished: title='$pageTitle', isChallengePage=$isChallengePage")

                        if (isChallengePage) {
                            // Still on a challenge page, wait for it to resolve
                            Log.d(TAG, "Still on challenge page, waiting for resolution...")
                            return@evaluateJavascript
                        }

                        // Extract HTML content via JavaScript
                        view.evaluateJavascript(
                            "(function() { return document.documentElement.outerHTML; })()"
                        ) { result ->
                            if (result != null && result != "null") {
                                htmlContent = unescapeJsString(result)
                                Log.d(TAG, "fetch: got HTML content, length=${htmlContent!!.length}")
                            } else {
                                Log.w(TAG, "fetch: evaluateJavascript returned null")
                                loadError = "Failed to extract page content"
                            }
                            latch.countDown()
                        }
                    }
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
                    latch.countDown()
                }
            }

            // Set request headers if provided
            val headers = mutableMapOf<String, String>()
            request.headers.forEach { (name, value) ->
                headers[name] = value
            }
            wv.loadUrl(url, headers)
        }

        // Wait for the page to load
        val completed = latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        // Clean up WebView
        handler.post {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }

        if (!completed) {
            Log.e(TAG, "fetch: timeout after ${TIMEOUT_SECONDS}s for $url")
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

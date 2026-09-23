package io.github.gmathi.novellibrary.network.cloudflare

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import okhttp3.FormBody
import okhttp3.Request
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test verifying that [WebViewFetcher.fetchPost] actually sends the POST body
 * through the WebView (via [android.webkit.WebView.postUrl]), rather than silently dropping it.
 *
 * Uses httpbin.org/post, a public echo endpoint, so the test doesn't depend on or affect any
 * real Cloudflare-protected site. httpbin echoes submitted form fields back in the response
 * body, which lets us assert the exact field/value we sent was received.
 */
@RunWith(AndroidJUnit4::class)
class WebViewFetcherPostInstrumentedTest {

    @Test
    fun fetchPost_sendsFormBodyToServer() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fetcher = WebViewFetcher(context)

        val marker = "novellibrary_test_marker_${System.currentTimeMillis()}"
        val formBody = FormBody.Builder()
            .add("probe_field", marker)
            .build()
        val request = Request.Builder()
            .url("https://httpbin.org/post")
            .post(formBody)
            .build()

        val response = fetcher.fetchPost(request)
        val html = response.body?.string() ?: ""

        assertTrue(
            "Expected the echoed response to contain the posted marker value, but it didn't. " +
                "Response snippet: ${html.take(500)}",
            html.contains(marker)
        )
    }
}

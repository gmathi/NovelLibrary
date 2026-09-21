package io.github.gmathi.novellibrary.fragment

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gmathi.novellibrary.util.Constants.FILE_PROTOCOL
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Instrumented test validating the WebView-based reader's offline rendering behavior
 * as implemented by [WebPageDBFragment.loadCreatedDocument] / [WebPageDBFragment.loadFromFile]:
 *
 *  - The base URL passed to [WebView.loadDataWithBaseURL] is `"$FILE_PROTOCOL$filePath"`
 *    i.e. `file://<filePath>`, matching Requirement 4.3 / Stored_File_Path semantics.
 *  - A chapter HTML file with relative CSS and image resource references renders
 *    without triggering a WebView error callback, and those relative resources resolve
 *    against the file:// base URL (loaded from disk, no network error).
 *
 * This test drives a bare [WebView] configured the same way the reader fragment configures
 * its `readerWebView` (same `loadDataWithBaseURL` call shape), rather than instantiating the
 * full [WebPageDBFragment] + `ReaderDBPagerActivity`, since only the WebView offline-rendering
 * contract is in scope for this task. No Robolectric is used; this runs against a real WebView
 * on-device/emulator per project convention (see [ExampleInstrumentedTest]).
 */
@RunWith(AndroidJUnit4::class)
class WebViewOfflineRenderingInstrumentedTest {

    private lateinit var chapterDir: File
    private lateinit var chapterFile: File
    private lateinit var webView: WebView

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Simulate a downloaded Chapter_File directory with relative CSS/image resources,
        // mirroring what DownloadWebPageThread/HtmlCleaner write to a Novel_Dir.
        chapterDir = File(context.cacheDir, "offline_render_test_${System.currentTimeMillis()}")
        chapterDir.mkdirs()

        File(chapterDir, "style.css").writeText("body { color: #000; }")
        // 1x1 transparent GIF, smallest valid image payload, referenced via a relative path.
        val gifBytes = byteArrayOf(
            0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00, 0x01, 0x00.toByte(),
            0x80.toByte(), 0x00, 0x00, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
            0x00, 0x00, 0x00, 0x21, 0xF9.toByte(), 0x04, 0x01, 0x00, 0x00, 0x00,
            0x00, 0x2C, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00,
            0x02, 0x02, 0x44, 0x01, 0x00, 0x3B
        )
        File(chapterDir, "cover.gif").writeBytes(gifBytes)

        chapterFile = File(chapterDir, "chapter.html")
        chapterFile.writeText(
            """
            <html>
              <head><link rel="stylesheet" type="text/css" href="style.css"></head>
              <body>
                <img src="cover.gif" />
                <p>Offline chapter content</p>
              </body>
            </html>
            """.trimIndent()
        )
    }

    @After
    fun tearDown() {
        chapterDir.deleteRecursively()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            if (::webView.isInitialized) {
                webView.stopLoading()
                webView.destroy()
            }
        }
    }

    @Test
    fun webViewReader_setsFileBaseUrl_andRendersOfflinePageWithoutError() {
        // This is exactly the base URL WebPageDBFragment.loadCreatedDocument() passes to
        // WebView.loadDataWithBaseURL when webPageSettings.filePath is non-null:
        //   "$FILE_PROTOCOL${it.filePath}" i.e. "file://<filePath>"
        val expectedBaseUrl = "$FILE_PROTOCOL${chapterFile.absolutePath}"
        assertTrue(expectedBaseUrl.startsWith("file://"))

        val hadError = booleanArrayOf(false)
        val resolvedResourceUrls = java.util.Collections.synchronizedList(mutableListOf<String>())
        val pageFinishedLatch = CountDownLatch(1)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            webView = WebView(context)
            webView.settings.javaScriptEnabled = false
            webView.settings.allowFileAccess = true
            webView.settings.loadWithOverviewMode = true

            webView.webViewClient = object : WebViewClient() {
                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: android.webkit.WebResourceError?
                ) {
                    hadError[0] = true
                }

                // The reader's relative CSS/image references (href="style.css", src="cover.gif")
                // only resolve to real files if the WebView actually resolved them against the
                // file:// base URL passed to loadDataWithBaseURL. Intercepting these requests lets
                // us observe that resolution directly, since getUrl()/onPageFinished's url param
                // reflect the historyUrl (passed as null -> "about:blank"), not the base URL.
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                    val url = request?.url?.toString()
                    if (url != null) resolvedResourceUrls.add(url)
                    return super.shouldInterceptRequest(view, request)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    // The WebView's initial "about:blank" state fires onPageFinished before
                    // loadDataWithBaseURL's content actually commits; wait for the resource
                    // requests below instead of relying on this callback's url.
                    if (url == "about:blank" && resolvedResourceUrls.isEmpty()) return
                    pageFinishedLatch.countDown()
                }
            }

            // Mirrors WebPageDBFragment.loadCreatedDocument():
            // readerWebView.loadDataWithBaseURL(FILE_PROTOCOL + filePath, html, "text/html", "UTF-8", null)
            webView.loadDataWithBaseURL(
                expectedBaseUrl,
                chapterFile.readText(),
                "text/html",
                "UTF-8",
                null
            )
        }

        val finished = pageFinishedLatch.await(10, TimeUnit.SECONDS)
        assertTrue("WebView did not finish loading the offline page in time", finished)

        val expectedCssUrl = "$FILE_PROTOCOL${File(chapterDir, "style.css").absolutePath}"
        val expectedImageUrl = "$FILE_PROTOCOL${File(chapterDir, "cover.gif").absolutePath}"

        assertTrue(
            "Relative CSS reference must resolve against the file://<filePath> base URL, got: $resolvedResourceUrls",
            resolvedResourceUrls.contains(expectedCssUrl)
        )
        assertTrue(
            "Relative image reference must resolve against the file://<filePath> base URL, got: $resolvedResourceUrls",
            resolvedResourceUrls.contains(expectedImageUrl)
        )
        assertFalse("Offline page with relative CSS/image resources must render without a WebViewClient error", hadError[0])
    }
}

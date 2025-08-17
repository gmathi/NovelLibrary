package io.github.gmathi.novellibrary.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import org.junit.Assert.*

class WebPageDocumentFetcherTest {

    @Test
    fun `request should create GET request`() {
        // Given
        val url = "https://example.com/test"

        // When
        val request = WebPageDocumentFetcher.request(url)

        // Then
        assertEquals("GET", request.method)
        assertEquals(url, request.url.toString())
    }

    @Test
    fun `document parsing should work correctly`() {
        // Given
        val htmlContent = "<html><head><title>Test</title></head><body>Content</body></html>"
        val mediaType = "text/html; charset=utf-8".toMediaType()
        val responseBody = htmlContent.toResponseBody(mediaType)
        val response = Response.Builder()
            .request(Request.Builder().url("https://example.com/test").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(responseBody)
            .build()

        // When
        val document = WebPageDocumentFetcher.document(response)

        // Then
        assertNotNull(document)
        assertEquals("Test", document.title())
        assertEquals("Content", document.body().text())
    }

    @Test
    fun `string extraction should work correctly`() {
        // Given
        val content = "test response content"
        val mediaType = "text/plain".toMediaType()
        val responseBody = content.toResponseBody(mediaType)
        val response = Response.Builder()
            .request(Request.Builder().url("https://example.com/test").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(responseBody)
            .build()

        // When
        val result = WebPageDocumentFetcher.string(response)

        // Then
        assertEquals(content, result)
    }
}
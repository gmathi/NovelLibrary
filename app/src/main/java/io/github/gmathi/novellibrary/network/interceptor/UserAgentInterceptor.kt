package io.github.gmathi.novellibrary.network.interceptor

import io.github.gmathi.novellibrary.model.source.online.HttpSource
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Forces the app's canonical User-Agent on every outgoing request so the UA that solves a
 * Cloudflare challenge (in the WebView) is byte-identical to the UA that replays the request
 * (through OkHttp). A cf_clearance cookie is bound to the client fingerprint, so a divergent
 * per-source UA would get the cookie rejected on replay.
 */
class UserAgentInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val canonical = HttpSource.userAgent()
        if (request.header("User-Agent") == canonical) {
            return chain.proceed(request)
        }
        return chain.proceed(
            request.newBuilder()
                .removeHeader("User-Agent")
                .addHeader("User-Agent", canonical)
                .build()
        )
    }
}

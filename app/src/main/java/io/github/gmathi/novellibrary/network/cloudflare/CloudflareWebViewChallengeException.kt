package io.github.gmathi.novellibrary.network.cloudflare

import java.io.IOException

/**
 * Thrown by [WebViewFetcher] when the in-app WebView itself cannot get past a Cloudflare
 * challenge (the interstitial never clears within the poll window, or the POST fetch sees a
 * challenge response). Carries the exact gated URL so the caller can route the user to the
 * manual [io.github.gmathi.novellibrary.activity.CloudflareResolverActivity] and resume the
 * fetch afterwards.
 *
 * Kept distinct from a generic WebView load failure so callers can tell "needs manual
 * Cloudflare verification" apart from "the fetch simply failed".
 */
class CloudflareWebViewChallengeException(
    val gatedUrl: String,
    message: String = "Cloudflare challenge in WebView for $gatedUrl"
) : IOException(message)

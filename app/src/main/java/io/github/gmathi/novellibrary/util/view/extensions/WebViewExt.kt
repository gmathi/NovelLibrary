package io.github.gmathi.novellibrary.util.view.extensions

import android.webkit.WebSettings
import android.webkit.WebView
import io.github.gmathi.novellibrary.util.view.WebViewUtil

fun WebView.isOutdated(): Boolean {
    return getWebViewMajorVersion(this) < WebViewUtil.MINIMUM_WEBVIEW_VERSION
}

fun WebView.setDefaultSettings() {
    with(settings) {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        setAppCacheEnabled(true)
        useWideViewPort = false
        loadWithOverviewMode = false
        cacheMode = WebSettings.LOAD_DEFAULT
    }
}

// Based on https://stackoverflow.com/a/29218966
private fun getWebViewMajorVersion(webview: WebView): Int {
    val originalUA: String = webview.settings.userAgentString

    // Next call to getUserAgentString() will get us the default
    webview.settings.userAgentString = null

    val uaRegexMatch = WebViewUtil.WEBVIEW_UA_VERSION_REGEX.matchEntire(webview.settings.userAgentString)
    val webViewVersion: Int = if (uaRegexMatch != null && uaRegexMatch.groupValues.size > 1) {
        uaRegexMatch.groupValues[1].toInt()
    } else {
        0
    }

    // Revert to original UA string
    webview.settings.userAgentString = originalUA

    return webViewVersion
}

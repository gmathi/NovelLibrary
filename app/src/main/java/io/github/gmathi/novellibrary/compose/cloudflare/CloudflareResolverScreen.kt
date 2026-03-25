package io.github.gmathi.novellibrary.compose.cloudflare

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.gmathi.novellibrary.model.source.online.HttpSource
import io.github.gmathi.novellibrary.util.view.extensions.setDefaultSettings

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CloudflareResolverScreen(
    url: String,
    onComplete: (resolvedUrl: String?) -> Unit,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var challengeSolved by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var currentWebViewUrl by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.stopLoading()
            webViewRef?.destroy()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cloudflare Verification") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = { onComplete(currentWebViewUrl) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Text(
                        if (challengeSolved) "Verification complete — tap to continue"
                        else "I have completed the verification"
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        setDefaultSettings()
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            // Must match the UA used by OkHttp requests so Cloudflare
                            // accepts the cf_clearance cookie on the retry.
                            userAgentString = HttpSource.DEFAULT_USER_AGENT
                            useWideViewPort = true
                            loadWithOverviewMode = true
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, pageUrl: String?) {
                                super.onPageFinished(view, pageUrl)
                                isLoading = false
                                if (pageUrl != null) {
                                    currentWebViewUrl = pageUrl
                                    val cookies = CookieManager.getInstance().getCookie(pageUrl)
                                    if (cookies?.contains("cf_clearance") == true) {
                                        challengeSolved = true
                                    }
                                }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                isLoading = newProgress < 100
                            }
                        }

                        loadUrl(url)
                        webViewRef = this
                    }
                }
            )
        }
    }
}

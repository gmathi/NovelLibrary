package io.github.gmathi.novellibrary.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebViewClient
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.network.HostNames
import kotlinx.android.synthetic.main.activity_web_view.*

class WebViewActivity : BaseActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        previewWebView.apply {
            settings.apply {
                javaScriptEnabled = true
                userAgentString = HostNames.USER_AGENT
            }
            webViewClient = WebViewClient()
            loadUrl(intent.getStringExtra("url"))
        }
    }
}

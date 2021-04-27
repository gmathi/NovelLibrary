package io.github.gmathi.novellibrary.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebViewClient
import io.github.gmathi.novellibrary.databinding.ActivityWebViewBinding
import io.github.gmathi.novellibrary.network.HostNames

class WebViewActivity : BaseActivity() {

    private lateinit var binding: ActivityWebViewBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.previewWebView.apply {
            settings.apply {
                javaScriptEnabled = true
                userAgentString = HostNames.USER_AGENT
            }
            webViewClient = WebViewClient()
            val url = intent.getStringExtra("url") ?: return
            loadUrl(url)
        }
    }
}

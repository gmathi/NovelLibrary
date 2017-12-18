package io.github.gmathi.novellibrary.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.webkit.WebViewClient
import io.github.gmathi.novellibrary.R
import kotlinx.android.synthetic.main.activity_web_view.*

class WebViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        previewWebView.settings.javaScriptEnabled = true
        previewWebView.webViewClient = WebViewClient()
        previewWebView.loadUrl(intent.getStringExtra("url"))
    }

}

package com.mgn.bingenovelreader.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.webkit.WebView
import android.webkit.WebViewClient
import com.mgn.bingenovelreader.R
import kotlinx.android.synthetic.main.activity_reader.*

class ReaderActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)
        setWebView()

        val filePath = intent.getStringExtra("filePath")
        val internalFilePath = "file://$filePath"

        readerWebView.loadUrl(internalFilePath)
    }

    fun setWebView() {
        readerWebView.setWebViewClient(object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {

                return false
            }
        })
    }
}

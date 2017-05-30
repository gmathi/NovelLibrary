package com.mgn.bingenovelreader.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.webkit.WebView
import android.webkit.WebViewClient
import com.mgn.bingenovelreader.R
import com.mgn.bingenovelreader.dataCenter
import com.mgn.bingenovelreader.models.WebPage
import kotlinx.android.synthetic.main.activity_reader.*

class ReaderActivity : AppCompatActivity() {

    var position: Int = 0
    var title: String? = null
    var urls: ArrayList<String> = ArrayList()
    var webPages: ArrayList<WebPage>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)
        setWebView()

        position = intent.getIntExtra("position", 0)
        title = intent.getStringExtra("title")
        if (title == null) finish()
        webPages = dataCenter.cacheMap[title!!]
        webPages?.mapTo(urls) { it.url }

        readerWebView.loadData(webPages!![position].pageData, "text/html; charset=utf-8", "UTF-8")
    }

    fun setWebView() {
        readerWebView.setWebViewClient(object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (urls.contains(url))
                    view?.loadData(webPages!![urls.indexOf(url)].pageData, "text/html; charset=utf-8", "UTF-8")
                else
                    view?.loadUrl(url)
                return true
            }
        })
    }
}

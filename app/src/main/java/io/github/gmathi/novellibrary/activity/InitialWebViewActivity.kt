package io.github.gmathi.novellibrary.activity

import android.annotation.TargetApi
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.network.HostNames.USER_AGENT
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.util.Utils
import kotlinx.android.synthetic.main.activity_initial_web_view.*


class InitialWebViewActivity : AppCompatActivity() {

    companion object {
        val TAG = "InitialWebViewActivity"
    }

    var mainUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initial_web_view)

        if (!Utils.checkNetwork(this))
            startNavDrawerActivity()

        previewWebView.settings.javaScriptEnabled = true
        previewWebView.settings.userAgentString = USER_AGENT
        previewWebView.webViewClient = object : WebViewClient() {

            @Suppress("OverridingDeprecatedMember")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                val uri = Uri.parse(url)
                return handleUri(uri)
            }

            @TargetApi(Build.VERSION_CODES.N)
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url
                return handleUri(uri)
            }

            fun handleUri(uri: Uri?): Boolean {
                Log.e(TAG, "url:$uri")
                //if (uri?.toString() == mainUrl) startNavDrawerActivity()
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                val cookies = CookieManager.getInstance().getCookie(url)
                Log.e(TAG, "All the cookiesMap in a string:" + cookies)

                if (cookies.contains("cfduid") && cookies.contains("cf_clearance")) {
                    val map: HashMap<String, String> = HashMap()
                    val cookiesArray = cookies.split("; ")
                    cookiesArray.forEach { cookie ->
                        val cookieSplit = cookie.split("=")
                        map.put(cookieSplit[0], cookieSplit[1])
                    }
                    NovelApi.cookies = cookies
                    NovelApi.cookiesMap = map
                    startNavDrawerActivity()
                    finish()
                }

            }


        }

        mainUrl = "https://www.novelupdates.com/"
        previewWebView.loadUrl(mainUrl)
    }

}

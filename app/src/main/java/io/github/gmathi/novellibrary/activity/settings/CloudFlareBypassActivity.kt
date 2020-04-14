package io.github.gmathi.novellibrary.activity.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.util.Logs
import kotlinx.android.synthetic.main.activity_cloudflare_bypass.*

class CloudFlareBypassActivity : BaseActivity() {

    private var currentHost: String = ""

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloudflare_bypass)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        view.settings.javaScriptEnabled = true
        view.settings.userAgentString = HostNames.USER_AGENT
        view.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val cookies = CookieManager.getInstance().getCookie(url)
                if (cookies != null && cookies.contains("cf_clearance")) {
                    CloudFlareByPasser.saveCookies(currentHost)
                    finish()
                }
            }
        }
        bypassHost("novelupdates.com")
    }

    fun bypassHost(url: String) {
        currentHost = url
        view.loadUrl("https://www.$url")
    }

    /*override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }*/

}
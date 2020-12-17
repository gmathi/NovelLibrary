package io.github.gmathi.novellibrary.activity.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.network.CloudFlareByPasser
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.util.system.setDefaultSettings
import kotlinx.android.synthetic.main.activity_sync_login.*
import java.net.URL

class SyncLoginActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sync_login)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        confirm.setOnClickListener { finish() }

        val url = intent.getStringExtra("url") ?: return
        val host = URL(url).host
        CloudFlareByPasser.clearCookies(host)
        view.setDefaultSettings()
        view.apply {
            settings.apply {
                @SuppressLint("SetJavaScriptEnabled")
                javaScriptEnabled = true
                userAgentString = HostNames.USER_AGENT
            }
            val lookup = intent.getStringExtra("lookup")?.toRegex()
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (CloudFlareByPasser.saveLoginCookies(host, lookup!!)) {
                        //finish()
                        cookieStatus.text = getString(R.string.sync_cookies_found)
                        cookieStatus.isChecked = true
                    } else {
                        cookieStatus.text = getString(R.string.sync_cookies_not_found)
                        cookieStatus.isChecked = false
                    }
                }
            }
            loadUrl(url)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
}
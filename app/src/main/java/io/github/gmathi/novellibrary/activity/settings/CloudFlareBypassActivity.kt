package io.github.gmathi.novellibrary.activity.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.databinding.ActivityCloudflareBypassBinding
import io.github.gmathi.novellibrary.network.CloudFlareByPasser
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.util.system.setDefaultSettings

class CloudFlareBypassActivity : BaseActivity() {

    private var currentHost: String = ""
    private var closeTimer: CountDownTimer? = null
    
    private lateinit var binding: ActivityCloudflareBypassBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityCloudflareBypassBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.checkDone.visibility = View.GONE
        binding.view.setDefaultSettings()
        binding.view.settings.javaScriptEnabled = true
        binding.view.settings.userAgentString = HostNames.USER_AGENT
        binding.view.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val cookies = CookieManager.getInstance().getCookie(url)
                if (cookies != null && cookies.contains("cf_clearance")) {
                    CloudFlareByPasser.saveCookies(currentHost)
                    //finish()
                    binding.checkState.visibility = View.GONE
                    binding.checkDone.visibility = View.VISIBLE
                    closeTimer = object: CountDownTimer(3000, 4000) {
                        override fun onTick(millisUntilFinished: Long) { }

                        override fun onFinish() {
                            finish()
                        }
                    }.start()
                }
            }
        }
        val host = intent.getStringExtra("host")
        if (host != null) bypassHost(host)
    }

    override fun onDestroy() {
        closeTimer?.cancel()
        closeTimer = null
        super.onDestroy()
    }

    private fun bypassHost(url: String) {
        currentHost = url
        binding.view.loadUrl("https://www.$url")
    }

}
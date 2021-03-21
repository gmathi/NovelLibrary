//package io.github.gmathi.novellibrary.activity.settings
//
//import android.annotation.SuppressLint
//import android.os.Bundle
//import android.view.MenuItem
//import android.webkit.WebView
//import android.webkit.WebViewClient
//import io.github.gmathi.novellibrary.R
//import io.github.gmathi.novellibrary.activity.BaseActivity
//import io.github.gmathi.novellibrary.databinding.ActivitySyncLoginBinding
//import io.github.gmathi.novellibrary.network.CloudFlareByPasser
//import io.github.gmathi.novellibrary.network.HostNames
//import io.github.gmathi.novellibrary.util.system.setDefaultSettings
//import java.net.URL
//
//class SyncLoginActivity : BaseActivity() {
//
//    private lateinit var binding: ActivitySyncLoginBinding
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        binding = ActivitySyncLoginBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//        setSupportActionBar(binding.toolbar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//
//        binding.confirm.setOnClickListener { finish() }
//
//        val url = intent.getStringExtra("url") ?: return
//        val host = URL(url).host
//        CloudFlareByPasser.clearCookies(host)
//        binding.view.setDefaultSettings()
//        binding.view.apply {
//            settings.apply {
//                @SuppressLint("SetJavaScriptEnabled")
//                javaScriptEnabled = true
//                userAgentString = HostNames.USER_AGENT
//            }
//            val lookup = intent.getStringExtra("lookup")?.toRegex()
//            webViewClient = object : WebViewClient() {
//                override fun onPageFinished(view: WebView?, url: String?) {
//                    if (CloudFlareByPasser.saveLoginCookies(host, lookup!!)) {
//                        //finish()
//                        binding.cookieStatus.text = getString(R.string.sync_cookies_found)
//                        binding.cookieStatus.isChecked = true
//                    } else {
//                        binding.cookieStatus.text = getString(R.string.sync_cookies_not_found)
//                        binding.cookieStatus.isChecked = false
//                    }
//                }
//            }
//            loadUrl(url)
//        }
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        if (item.itemId == android.R.id.home) finish()
//        return super.onOptionsItemSelected(item)
//    }
//}
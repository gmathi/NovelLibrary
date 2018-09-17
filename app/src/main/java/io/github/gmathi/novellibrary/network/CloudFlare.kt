package io.github.gmathi.novellibrary.network

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.net.Uri
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.DataCenter
import com.loopj.android.http.PersistentCookieStore
import com.loopj.android.http.TextHttpResponseHandler
import cz.msebera.android.httpclient.Header
import cz.msebera.android.httpclient.impl.cookie.BasicClientCookie
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import com.loopj.android.http.SyncHttpClient
import io.github.gmathi.novellibrary.util.Logs

class CloudFlare(val context: Context, val listener: Listener) {

    companion object {
        private const val TAG = "CloudFlare"
        private const val NovelUpdatesHost = "novelupdates.com"

        interface Listener {
            fun onSuccess()
            fun onFailure()
        }

        private fun getCookieMap(): Map<String, String> {
            val map = HashMap<String, String>()
            map["device"] = "computer"
            if (isActive()) {
                map[DataCenter.CF_COOKIES_DUID] = dataCenter.cfDuid
                map[DataCenter.CF_COOKIES_CLEARANCE] = dataCenter.cfClearance
            }
            return map
        }

        private fun isActive(): Boolean {
            return dataCenter.cfClearance.isNotBlank() && dataCenter.cfDuid.isNotBlank()
        }

        @Suppress("unused")
        fun clearSavedCookies() {
            //dataCenter.userAgent = HostNames.USER_AGENT
            dataCenter.cfDuid = ""
            dataCenter.cfClearance = ""
            dataCenter.cfCookiesString = ""
        }

        private fun saveCookie(cookies: String) {
            val parts = cookies.split(";")
            for (cookie in parts) {
                if (cookie.contains(DataCenter.CF_COOKIES_DUID)) {
                    dataCenter.cfDuid = getCookieValue(cookie)
                    Logs.error(TAG, dataCenter.cfDuid)
                }
                if (cookie.contains(DataCenter.CF_COOKIES_CLEARANCE)) {
                    dataCenter.cfClearance = getCookieValue(cookie)
                    Logs.error(TAG, dataCenter.cfClearance)
                }
            }
            dataCenter.cfCookiesString = cookies
            NovelApi.cookies = cookies
            NovelApi.cookiesMap = getCookieMap()
        }

        private fun clearCookies(domain: String) {
            val cookieManager = CookieManager.getInstance()
            val cookieString = cookieManager.getCookie(domain)
            cookieString?.split(";")?.forEach {
                cookieManager.setCookie(domain, it.split("=")[0].trim() + "=; Expires=Wed, 31 Dec 2025 23:59:59 GMT")
            }
        }

        private fun getCookieValue(cookie: String): String {
            return cookie.trim().substring(cookie.trim().indexOf("=") + 1)
        }
    }


    fun check() {
        Logs.warning(TAG, "Checking for CloudFlare")
        val client = SyncHttpClient()

        if (!dataCenter.cfClearance.isBlank() && !dataCenter.cfDuid.isBlank()) {

            val cookieStore = PersistentCookieStore(context)

            val dUidCookie = BasicClientCookie(DataCenter.CF_COOKIES_DUID, dataCenter.cfDuid)
            val clearanceCookie = BasicClientCookie(DataCenter.CF_COOKIES_CLEARANCE, dataCenter.cfClearance)

            dUidCookie.domain = ".$NovelUpdatesHost"
            clearanceCookie.domain = ".$NovelUpdatesHost"

            dUidCookie.path = "/"
            clearanceCookie.path = "/"

            cookieStore.addCookie(dUidCookie)
            cookieStore.addCookie(clearanceCookie)
            client.setCookieStore(cookieStore)
        }

        client.setUserAgent(dataCenter.userAgent)
        client.get("https://$NovelUpdatesHost", null, object : TextHttpResponseHandler() {

            override fun onSuccess(statusCode: Int, headers: Array<out Header>?, responseString: String?) {
                listener.onSuccess()
                NovelApi.cookies = dataCenter.cfCookiesString
                NovelApi.cookiesMap = getCookieMap()
            }

            @SuppressLint("SetJavaScriptEnabled")
            override fun onFailure(statusCode: Int, headers: Array<out Header>?, responseString: String?, throwable: Throwable?) {
                if (statusCode != 0) {
                    Logs.error(TAG, "Failed with code: " + statusCode)

                    //(context as AppCompatActivity).runOnUiThread {
                    Handler(Looper.getMainLooper()).post {
                        clearCookies(".$NovelUpdatesHost")
                        loadCookiesUsingWebView()
                    }
                }
            }

        })
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadCookiesUsingWebView() {
        val webView = WebView(context)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {

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
                val url = uri.toString()
                Logs.info(TAG, "Redirect to url:$url")

                if (url.contains(NovelUpdatesHost) && !url.contains("cdn-cgi")) {
                    Logs.info(TAG, "Extracting Cookies")
                    val cookies = CookieManager.getInstance().getCookie(url)
                    Logs.info(TAG, "Cookies: " + cookies?.toString())
                    if (cookies != null && (cookies.contains(DataCenter.CF_COOKIES_DUID) || cookies.contains(DataCenter.CF_COOKIES_CLEARANCE))) {
                        //dataCenter.userAgent = webView.settings.userAgentString
                        saveCookie(cookies)
                        listener.onSuccess()
                    } else {
                        Logs.error(TAG, "CloudFlare ByPass Failed!!")
                        listener.onFailure()
                    }
                    return true
                }

                //listener.onFailure()
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val cookies = CookieManager.getInstance().getCookie(url)
                Logs.error(TAG, "OnPageFinished: Cookies: $cookies")
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    Logs.error(TAG, "Inside: if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR2) ")
                }

                val uri = Uri.parse(url)
                handleUri(uri)
            }
        }

        webView.loadUrl("https://novelupdates.com")
    }


}
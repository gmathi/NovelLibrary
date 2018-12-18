import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import co.metalab.asyncawait.async
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.util.DataCenter
import io.github.gmathi.novellibrary.util.Logs
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import java.io.IOException

object CloudFlareByPasser {

    const val TAG = "CloudFlareByPasser"

    @SuppressLint("SetJavaScriptEnabled")
    fun check(context: Context, hostName: String = "novelupdates.com", callback: (state: State) -> Unit) {
        async {
            if (await { isNeeded(hostName) }) {
                callback.invoke(State.CREATING)
                Logs.error(TAG, "is needed")
                clearCookies(hostName)

                val webView = WebView(context).apply {
                    settings?.javaScriptEnabled = true
                    webViewClient = object : WebViewClient() {

                        @Suppress("OverridingDeprecatedMember")
                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                            Log.e(TAG, "Override $url")
                            if (url.toString() == "https://www.$hostName/") {
                                Log.e(TAG, "Cookies: " + CookieManager.getInstance().getCookie("https://www.$hostName/"))
                                if (saveCookies(hostName)) {
                                    callback.invoke(State.CREATED)
                                }
                            }
                            return false
                        }

                        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            Log.e(TAG, "Override ${request?.url}")
                            if (request?.url.toString() == "https://www.$hostName/") {
                                Log.e(TAG, "Cookies: " + CookieManager.getInstance().getCookie("https://www.$hostName/"))
                                if (saveCookies(hostName)) {
                                    callback.invoke(State.CREATED)
                                }
                            }
                            return false
                        }
                    }
                    settings?.userAgentString = HostNames.USER_AGENT
                }
                webView.loadUrl("https://www.$hostName")
            } else {
                callback.invoke(State.UNNEEDED)
                Log.e(TAG, "Not needed")
            }
        }
    }

    private fun isNeeded(hostName: String): Boolean {
        return try {
            val response = Jsoup.connect("https://www.$hostName").cookies(getCookieMap(hostName)).userAgent(HostNames.USER_AGENT).execute()
            response.statusCode() == 503 && response.hasHeader("cf-ray")
        } catch (e: HttpStatusException) {
            e.statusCode == 503
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun saveCookies(hostName: String): Boolean {
        val cookies = CookieManager.getInstance().getCookie("https://www.$hostName/").trim()
        if (cookies.contains(DataCenter.CF_COOKIES_CLEARANCE)) {
            val parts = cookies.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (cookie in parts) {
                if (cookie.contains(DataCenter.CF_COOKIES_DUID))
                    dataCenter.setCFDuid(hostName, cookie.trim().substring(cookie.trim().indexOf("=") + 1))
                if (cookie.contains(DataCenter.CF_COOKIES_CLEARANCE))
                    dataCenter.setCFClearance(hostName, cookie.trim().substring(cookie.trim().indexOf("=") + 1))
            }
            dataCenter.setCFCookiesString(hostName, cookies)
            return true
        }
        return false
    }

    private fun clearCookies(hostName: String) {
        val cookieManager = CookieManager.getInstance()
        val cookieString = cookieManager.getCookie(".$hostName")
        if (cookieString != null) {
            val cookies = cookieString.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (cookie in cookies) {
                val cookieParts = cookie.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                cookieManager.setCookie(".$hostName", cookieParts[0].trim { it <= ' ' } + "=; Expires=Wed, 31 Dec 2025 23:59:59 GMT")
            }
        }
    }

    fun getCookieMap(hostName: String): Map<String, String> {
        val map = HashMap<String, String>()
        map[DataCenter.CF_COOKIES_DUID] = dataCenter.getCFDuid(hostName)
        map[DataCenter.CF_COOKIES_CLEARANCE] = dataCenter.getCFClearance(hostName)
        return map
    }

    enum class State {
        CREATING, CREATED, UNNEEDED
    }

}
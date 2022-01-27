package io.github.gmathi.novellibrary.network

import android.webkit.CookieManager
import io.github.gmathi.novellibrary.model.preference.DataCenter
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import uy.kohesive.injekt.injectLazy
import java.net.URL

class AndroidCookieJar : CookieJar {

    private val manager = CookieManager.getInstance()
    private val dataCenter: DataCenter by injectLazy()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val urlString = url.toString()
        cookies.forEach { manager.setCookie(urlString, it.toString()) }
    }

    fun saveFromResponse(urlString: String, cookiesStrings: List<String>) {
        cookiesStrings.forEach { manager.setCookie(urlString, it) }
    }


    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return get(url)
    }

    fun get(url: HttpUrl): List<Cookie> {
        val cookies = manager.getCookie(url.toString())

        return if (cookies != null && cookies.isNotEmpty()) {
            cookies.split(";").mapNotNull { Cookie.parse(url, it) }
        } else {
            emptyList()
        }
    }

    fun remove(url: HttpUrl, cookieNames: List<String>? = null, maxAge: Int = -1) {
        val urlString = url.toString()
        val cookies = manager.getCookie(urlString) ?: return

        fun List<String>.filterNames(): List<String> {
            return if (cookieNames != null) {
                this.filter { it in cookieNames }
            } else {
                this
            }
        }

        cookies.split(";")
            .map { it.substringBefore("=") }
            .filterNames()
            .onEach { manager.setCookie(urlString, "$it=;Max-Age=$maxAge") }
    }

    fun removeAll() {
        manager.removeAllCookies {}
    }

    // Old methods copied here
    fun saveLoginCookies(hostName: String, lookupRegex: Regex?): Boolean {
        val pureHost = hostName.replace("www.", "").replace("m.", "")
        val general = saveLoginCookiesInternal(pureHost, lookupRegex)
        val www = saveLoginCookiesInternal("www.$pureHost", lookupRegex)
        val mobile = saveLoginCookiesInternal("m.$pureHost", lookupRegex)
        return general || www || mobile;
    }

    private fun saveLoginCookiesInternal(hostName: String, lookup: Regex?): Boolean {
        val cookies = CookieManager.getInstance().getCookie("https://$hostName/")?.trim() ?: return false
        val parts = cookies.split(";".toRegex()).dropLastWhile { it.isEmpty() }.filter {
            lookup?.containsMatchIn(it) ?: false
        }
        return if (parts.count() != 0) {
            dataCenter.setLoginCookiesString(hostName, parts.joinToString(";"))
            true
        } else {
            false
        }
    }

    fun clearCookies(hostName: String) {
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

    private fun populateCookieMap(hostName: String, map: HashMap<String, String>) {

        dataCenter.getLoginCookiesString(hostName).split(";".toRegex()).dropLastWhile { it.isEmpty() }.forEach {
            val idx = it.indexOf('=')
            map[it.substring(0, idx)] = it.substring(idx + 1)
        }
    }

    fun getCookieMap(url: URL?): Map<String, String> {
        val map = HashMap<String, String>()
        if (url?.host == null) return map
        val hostName = url.host.replace("www.", "").replace("m.", "").trim()
        populateCookieMap(hostName, map)
        populateCookieMap("www.$hostName", map)
        populateCookieMap("m.$hostName", map)
        return map
    }


}

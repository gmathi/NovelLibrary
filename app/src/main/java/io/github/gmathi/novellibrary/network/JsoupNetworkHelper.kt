//package io.github.gmathi.novellibrary.network
//
//import android.content.Context
//import android.net.Uri
//import android.webkit.CookieManager
//import io.github.gmathi.novellibrary.util.DataCenter
//import io.github.gmathi.novellibrary.util.lang.fixMalformedWithHost
//import okhttp3.Request
//import okio.Buffer
//import org.jsoup.Connection
//import org.jsoup.Jsoup
//import uy.kohesive.injekt.injectLazy
//import java.io.IOException
//import java.util.regex.Pattern
//import javax.net.ssl.SSLPeerUnverifiedException
//
//class JsoupNetworkHelper(private val context: Context) {
//
//    private val dataCenter: DataCenter by injectLazy()
//    val cookieManager = AndroidCookieJar()
//
//    fun jsoupResponse(request: Request): Connection.Response {
//        val url = request.url.toString()
//        val isPost = request.method == "POST"
//        val requestBody = if (isPost) stringifyRequestBody(request) else null
//        return getDocumentWithParams(url, ignoreHttpErrors = true, ignoreContentType = true, isPost = isPost, requestBody = requestBody)
//    }
//
//    @Throws(Exception::class)
//    private fun getDocumentWithParams(
//        url: String,
//        ignoreHttpErrors: Boolean,
//        ignoreContentType: Boolean,
//        isPost: Boolean = false,
//        requestBody: String? = null
//    ): Connection.Response {
//        try {
//            // Since JSoup can't load different cookies after redirect, disable them and perform redirects manually.
//            // Redirect limit here just as a safeguard in case someone decides to do infinite redirect loop.
//            var response: Connection.Response
//            var redirectUrl = url
//            var redirectLimit = 5
//            do {
//                //Get any existing cookies
//                val cookies = CookieManager.getInstance().getCookie(url) ?: ""
//                val cookieMap: Map<String, String> = cookies.split(";").filter { it.split("=").count() == 2 }.map { cookie ->
//                    val kv = cookie.split("=")
//                    kv[0] to kv[1]
//                }.toMap()
//
//
//                // Create the connection
//                val connection = Jsoup
//                    .connect(redirectUrl)
//                    .referrer(redirectUrl)
//                    .cookies(cookieMap)
//                    .ignoreHttpErrors(ignoreHttpErrors)
//                    .ignoreContentType(ignoreContentType)
//                    .timeout(30000)
//                    .userAgent(HostNames.USER_AGENT)
//                    .followRedirects(false)
//
//                connection.method(if (isPost) Connection.Method.POST else Connection.Method.GET)
//                if (requestBody != null)
//                    connection.requestBody(requestBody)
//
//                response = connection.execute()
//
//                if (!response.hasHeader("location")) break
//                redirectUrl = response.header("location").fixMalformedWithHost(response.url().host, response.url().protocol)
//            } while (--redirectLimit >= 0)
//
//            return response
//
//        } catch (e: SSLPeerUnverifiedException) {
//            val p = Pattern.compile("Hostname\\s(.*?)\\snot", Pattern.DOTALL or Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE or Pattern.MULTILINE) // Regex for the value of the key
//            val m = p.matcher(e.localizedMessage ?: "")
//            if (m.find()) {
//                val hostName = m.group(1)
//                val hostNames = dataCenter.getVerifiedHosts()
//                if (!hostNames.contains(hostName ?: "")) {
//                    dataCenter.saveVerifiedHost(hostName ?: "")
//                    return getDocumentWithParams(url, ignoreHttpErrors, ignoreContentType)
//                }
//            }
//            throw e
//        } catch (e: IOException) {
//            val error = e.localizedMessage
//            if (error != null && error.contains("was not verified")) {
//                val hostName = Uri.parse(url)?.host!!
//                if (!HostNames.isVerifiedHost(hostName)) {
//                    dataCenter.saveVerifiedHost(hostName)
//                    return getDocumentWithParams(url, ignoreHttpErrors, ignoreContentType)
//                }
//            }
//            throw e
//        }
//    }
//
//    private fun stringifyRequestBody(request: Request): String? {
//        return try {
//            val copy = request.newBuilder().build()
//            val buffer = Buffer()
//            copy.body?.writeTo(buffer)
//            buffer.readUtf8()
//        } catch (e: IOException) {
//            "did not work"
//        }
//    }
//
//
//}
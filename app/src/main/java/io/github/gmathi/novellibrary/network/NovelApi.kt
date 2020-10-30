package io.github.gmathi.novellibrary.network

import android.net.Uri
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.extensions.fixMalformedWithHost
import io.github.gmathi.novellibrary.network.proxy.BaseProxyHelper
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.URL
import java.util.regex.Pattern
import javax.net.ssl.SSLPeerUnverifiedException


object NovelApi {

    @Throws(Exception::class)
    fun getDocument(url: String, ignoreHttpErrors: Boolean = true, useProxy: Boolean = true): Document {
        return getDocumentWithParams(url, ignoreHttpErrors = ignoreHttpErrors, ignoreContentType = false, useProxy = useProxy) as  Document
    }

    @Throws(Exception::class)
    fun getString(url: String, ignoreHttpErrors: Boolean = true, useProxy: Boolean = true): String {
        return getDocumentWithParams(url, ignoreHttpErrors = ignoreHttpErrors, ignoreContentType = true, useProxy = useProxy) as  String
    }

    @Throws(Exception::class)
    fun getDocumentWithFormData(url: String, formData:HashMap<String, String>, ignoreHttpErrors:Boolean = true): Document {
       return getDocumentWithParams(url, ignoreHttpErrors, ignoreContentType = false, isPost = true, formData = formData) as Document
    }

    @Throws(Exception::class)
    fun getStringWithFormData(url: String, formData:HashMap<String, String>, ignoreHttpErrors:Boolean = true): String {
        return getDocumentWithParams(url, ignoreHttpErrors, ignoreContentType = true, isPost = true, formData = formData) as String
    }

    @Throws(Exception::class)
    private fun getDocumentWithParams(
        url: String,
        ignoreHttpErrors: Boolean,
        ignoreContentType: Boolean,
        useProxy: Boolean = true,
        isPost: Boolean = false,
        formData: HashMap<String, String> = hashMapOf<String, String>()
    ): Any {
        try {
            // Since JSoup can't load different cookies after redirect, disable them and perform redirects manually.
            // Redirect limit here just as a safeguard in case someone decides to do infinite redirect loop.
            var proxy: BaseProxyHelper?
            var response: Connection.Response
            var redirectUrl = url
            var redirectLimit = 5
            do {
                proxy = BaseProxyHelper.getInstance(redirectUrl)
                if (!useProxy) { proxy = null }
                val connection = proxy?.connect(redirectUrl)
                        ?: Jsoup
                            .connect(redirectUrl)
                            .referrer(redirectUrl)
                            .cookies(CloudFlareByPasser.getCookieMap(URL(redirectUrl)))
                            .ignoreHttpErrors(ignoreHttpErrors)
                            .ignoreContentType(ignoreContentType)
                            .timeout(30000)
                            .userAgent(HostNames.USER_AGENT)
                            .followRedirects(false)

                connection.method(if (isPost) Connection.Method.POST else Connection.Method.GET)
                if (isPost && proxy == null) {
                    for (data in formData) {
                        connection.data(data.key, data.value)
                    }
                }

                response = connection.execute()

                if (!response.hasHeader("location")) break
                redirectUrl = response.header("location")
                    .fixMalformedWithHost(response.url().host, response.url().protocol)
            } while (--redirectLimit >= 0)

            val body = proxy?.body(response) ?: response.body()

            return if (ignoreContentType) body
            else proxy?.document(response) ?: Jsoup.parse(body, redirectUrl)

        } catch (e: SSLPeerUnverifiedException) {
            val p = Pattern.compile("Hostname\\s(.*?)\\snot", Pattern.DOTALL or Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE or Pattern.MULTILINE) // Regex for the value of the key
            val m = p.matcher(e.localizedMessage ?: "")
            if (m.find()) {
                val hostName = m.group(1)
                val hostNames = dataCenter.getVerifiedHosts()
                if (!hostNames.contains(hostName ?: "")) {
                    dataCenter.saveVerifiedHost(hostName ?: "")
                    return getDocumentWithParams(url, ignoreHttpErrors, ignoreContentType)
                }
            }
            throw e
        } catch (e: IOException) {
            val error = e.localizedMessage
            if (error != null && error.contains("was not verified")) {
                val hostName = Uri.parse(url)?.host!!
                if (!HostNames.isVerifiedHost(hostName)) {
                    dataCenter.saveVerifiedHost(hostName)
                    return getDocumentWithParams(url, ignoreHttpErrors, ignoreContentType)
                }
            }
            throw e
        }
    }

}
package io.github.gmathi.novellibrary.network

import CloudFlareByPasser
import android.net.Uri
import io.github.gmathi.novellibrary.dataCenter
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.URL
import java.util.regex.Pattern
import javax.net.ssl.SSLPeerUnverifiedException


object NovelApi {

    fun getDocument(url: String, ignoreHttpErrors: Boolean = true): Document {
        return getDocumentWithParams(url, ignoreHttpErrors = ignoreHttpErrors, ignoreContentType = false) as  Document
    }

    fun getString(url: String, ignoreHttpErrors: Boolean = true): String {
        return getDocumentWithParams(url, ignoreHttpErrors = ignoreHttpErrors, ignoreContentType = true) as  String
    }

    fun getDocumentWithFormData(url: String, formData:HashMap<String, String>, ignoreHttpErrors:Boolean = true): Document {
       return getDocumentWithParams(url, ignoreHttpErrors, ignoreContentType = false, isPost = true, formData = formData) as Document
    }

    fun getStringWithFormData(url: String, formData:HashMap<String, String>, ignoreHttpErrors:Boolean = true): String {
        return getDocumentWithParams(url, ignoreHttpErrors, ignoreContentType = true, isPost = true, formData = formData) as String
    }

    private fun getDocumentWithParams(
        url: String,
        ignoreHttpErrors: Boolean,
        ignoreContentType: Boolean,
        isPost: Boolean = false,
        formData: HashMap<String, String> = hashMapOf<String, String>()
    ): Any {

        try {
            // Since JSoup can't load different cookies after redirect, disable them and perform redirects manually.
            // Redirect limit here just as a safeguard in case someone decides to do infinite redirect loop.
            var doc: Connection.Response
            var redirectUrl = url
            var redirectLimit = 5
            do {
                val connection = Jsoup
                    .connect(redirectUrl)
                    .referrer(redirectUrl)
                    .cookies(CloudFlareByPasser.getCookieMap(URL(redirectUrl)))
                    .ignoreHttpErrors(ignoreHttpErrors)
                    .ignoreContentType(ignoreContentType)
                    .timeout(30000)
                    .userAgent(HostNames.USER_AGENT)
                    .followRedirects(false)

                connection.method(if (isPost) Connection.Method.POST else Connection.Method.GET)
                if (isPost) {
                    for (data in formData) {
                        connection.data(data.key, data.value)
                    }
                }

                doc = connection.execute()

                if (!doc.hasHeader("location")) break
                redirectUrl = doc.header("location")
            } while (redirectLimit-- > 0)

            return if (!ignoreContentType) doc.parse() else doc.body()
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
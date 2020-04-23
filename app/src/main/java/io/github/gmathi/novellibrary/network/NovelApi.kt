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

    fun getDocumentWithUserAgent(url: String): Document {
        return getDocumentWithUserAgentParams(url, ignoreHttpErrors = true, ignoreContentType = false)
    }

    fun getDocumentWithUserAgentIgnoreContentType(url: String): Document {
        return getDocumentWithUserAgentParams(url, ignoreHttpErrors = false, ignoreContentType = true)
    }

    private fun getDocumentWithUserAgentParams(url: String, ignoreHttpErrors: Boolean, ignoreContentType: Boolean): Document {
        try {
            // Since JSoup can't load different cookies after redirect, disable them and perform redirects manually.
            // Redirect limit here just as a safeguard in case someone decides to do infinite redirect loop.
            var doc : Connection.Response
            var redirectUrl = url
            var redirectLimit = 5
            do {
                doc = Jsoup
                    .connect(redirectUrl)
                    .referrer(redirectUrl)
                    .cookies(CloudFlareByPasser.getCookieMap(URL(redirectUrl)))
                    .ignoreHttpErrors(ignoreHttpErrors)
                    .ignoreContentType(ignoreContentType)
                    .timeout(30000)
                    .userAgent(HostNames.USER_AGENT)
                    .followRedirects(false)
                    .execute()
                if (!doc.hasHeader("location")) break
                redirectUrl = doc.header("location")
            } while(redirectLimit-- > 0)

            return doc.parse()
        } catch (e: SSLPeerUnverifiedException) {
            val p = Pattern.compile("Hostname\\s(.*?)\\snot", Pattern.DOTALL or Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE or Pattern.MULTILINE) // Regex for the value of the key
            val m = p.matcher(e.localizedMessage ?: "")
            if (m.find()) {
                val hostName = m.group(1)
                val hostNames = dataCenter.getVerifiedHosts()
                if (!hostNames.contains(hostName ?: "")) {
                    dataCenter.saveVerifiedHost(hostName ?: "")
                    return getDocumentWithUserAgentParams(url, ignoreHttpErrors, ignoreContentType)
                }
            }
            throw e
        } catch (e: IOException) {
            if (e.localizedMessage != null && e.localizedMessage.contains("was not verified")) {
                val hostName = Uri.parse(url)?.host!!
                if (!HostNames.isVerifiedHost(hostName)) {
                    dataCenter.saveVerifiedHost(hostName)
                    return getDocumentWithUserAgentParams(url, ignoreHttpErrors, ignoreContentType)
                }
            }
            throw e
        }
    }

}
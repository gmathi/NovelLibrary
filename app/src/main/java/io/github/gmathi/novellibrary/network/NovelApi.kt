package io.github.gmathi.novellibrary.network

import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.network.HostNames.USER_AGENT
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.regex.Pattern
import javax.net.ssl.SSLPeerUnverifiedException


class NovelApi {

    fun getDocument(url: String): Document {
        try {
            return Jsoup.connect(url).get()
        } catch (e: SSLPeerUnverifiedException) {
            val p = Pattern.compile("Hostname\\s(.*?)\\snot", Pattern.DOTALL or Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE or Pattern.MULTILINE) // Regex for the value of the key
            val m = p.matcher(e.localizedMessage)
            if (m.find()) {
                val hostName = m.group(1)
                if (!HostNames.isVerifiedHost(hostName)) {
                    dataCenter.saveVerifiedHost(m.group(1))
                    return getDocument(url)
                }
            }
            throw e
        }
    }

    fun getDocumentWithUserAgent(url: String): Document {
        try {
            return Jsoup.connect(url).userAgent(USER_AGENT).get()
        } catch (e: SSLPeerUnverifiedException) {
            val p = Pattern.compile("Hostname\\s(.*?)\\snot", Pattern.DOTALL or Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE or Pattern.MULTILINE) // Regex for the value of the key
            val m = p.matcher(e.localizedMessage)
            if (m.find()) {
                val hostName = m.group(1)
                if (!HostNames.isVerifiedHost(hostName)) {
                    dataCenter.saveVerifiedHost(m.group(1))
                    return getDocumentWithUserAgent(url)
                }
            }
            throw e
        }
    }

    fun getDocumentWithUserAgentIgnoreContentType(url: String): Document {
        try {
            return Jsoup.connect(url).userAgent(USER_AGENT).ignoreContentType(true).get()
        } catch (e: SSLPeerUnverifiedException) {
            val p = Pattern.compile("Hostname\\s(.*?)\\snot", Pattern.DOTALL or Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE or Pattern.MULTILINE) // Regex for the value of the key
            val m = p.matcher(e.localizedMessage)
            if (m.find()) {
                val hostName = m.group(1)
                if (!HostNames.isVerifiedHost(hostName)) {
                    dataCenter.saveVerifiedHost(m.group(1))
                    return getDocumentWithUserAgent(url)
                }
            }
            throw e
        }
    }

}
package io.github.gmathi.novellibrary.util.network

import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.model.preference.DataCenter
import okhttp3.Call
import okhttp3.Response

import java.io.IOException
import java.util.regex.Pattern
import javax.net.ssl.SSLPeerUnverifiedException

fun Call.safeExecute(dataCenter: DataCenter): Response {
    try {
        return execute()
    } catch (e: SSLPeerUnverifiedException) {
        val p = Pattern.compile("Hostname\\s(.*?)\\snot", Pattern.DOTALL or Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE or Pattern.MULTILINE) // Regex for the value of the key
        val m = p.matcher(e.localizedMessage ?: "")
        if (m.find()) {
            val hostName = m.group(1)
            val hostNames = dataCenter.getVerifiedHosts()
            if (!hostNames.contains(hostName ?: "")) {
                dataCenter.saveVerifiedHost(hostName ?: "")
            }
        }
        throw e
    } catch (e: IOException) {
        val url = this.request().url
        val error = e.localizedMessage
        if (error != null && error.contains("was not verified")) {
            val hostName = url.host
            if (!HostNames.isVerifiedHost(hostName)) {
                dataCenter.saveVerifiedHost(hostName)
            }
        }
        throw e
    } catch (e: Exception) {
        throw e
    }
}
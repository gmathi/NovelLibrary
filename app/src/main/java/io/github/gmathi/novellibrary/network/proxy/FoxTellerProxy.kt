package io.github.gmathi.novellibrary.network.proxy

import android.util.Base64
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.POST
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.network.asJsoup
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.jsoup.nodes.Document
import java.lang.Exception


class FoxTellerProxy : BaseProxyHelper() {

    companion object {
        val aux_dem = "https://www.${HostNames.FOXTELLER}/aux_dem"
        val dem_regex = """let +([de]) *= *'(.+?)';""".toRegex()
        val decode_regex = "%R([acbdfe])&".toRegex()

        // b/c and e/f are swapped, as per foxteller source
        val charmap: Map<String, Char> = mapOf(
            "a" to 'A',
            "c" to 'B', "b" to 'C',
            "d" to 'D',
            "f" to 'E', "e" to 'F'
        )
    }

    @ExperimentalStdlibApi
    override fun document(response: Response): Document {
        val doc = response.asJsoup()
        val cookiesList = response.headers.values("Set-Cookie")[0].split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val cookieMap = HashMap<String, String>()
        cookiesList.forEach {
            val keyValueSplit = it.split("=")
            if (keyValueSplit.size == 2)
                cookieMap[keyValueSplit[0]] = keyValueSplit[1]
        }

        val xsrf = cookieMap["XSRF-TOKEN"]
        val session = cookieMap["foxteller_session"] ?: xsrf
        val csrf = doc.selectFirst("meta[name=\"csrf-token\"]")?.attr("content")

        // remove chapter loading script as we load ourselves
        doc.selectFirst("script[src*=chapter.js]")?.remove()

        val dem = mutableMapOf<String, String>()
        doc.getElementsByTag("script").forEach { script ->
            dem_regex.findAll(script.data()).forEach { match ->
                dem[match.groups[1]?.value!!] = match.groups[2]?.value!!
            }
        }

        val data = JsonObject()
        data.addProperty("x1", dem["d"])
        data.addProperty("x2", dem["e"])


        val cookieString = "theme=light;font-size=16;gdb1=true;foxteller_session=$session;XSRF-TOKEN=$xsrf;"

        val headers = Headers.Builder()
            .add("Origin", "https://www.${HostNames.FOXTELLER}")
            .add("X-Requested-With", "XMLHttpRequest")
            .add("Content-Type", "application/json;charset=utf-8")
            .add("X-XSRF-TOKEN", xsrf ?: "")
            .add("X-CSRF-TOKEN", csrf ?: "")
            .add("TE", "trailers")
            .add("Cookie", cookieString)
            .build()
        val requestBody = data.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = POST(aux_dem, headers, requestBody)
        var chapterResponse: Response? = null
        try {
            chapterResponse = connect(request)
        } catch (e:Exception) {
            Logs.error("FoxTellerProxy", "request: $request", e)
            return doc
        }

        if (chapterResponse.code != 200) {
            val content = doc.getElementById("chapter-content")
            content?.children()?.remove()
            content?.append("<p><b>ERROR: Could not load chapter content (${chapterResponse.code}).</b></p>")
            return doc
        }

        var chapter = JsonParser.parseString(chapterResponse.body?.string())?.asJsonObject?.get("aux")?.asString ?: return doc

        chapter = chapter.replace(decode_regex) { match ->
            charmap.getValue(match.groups[1]?.value ?: "").toString()
        }
        chapter = Base64.decode(chapter, Base64.DEFAULT).decodeToString()

        val content = doc.getElementById("chapter-content")
        content?.textNodes()?.firstOrNull()?.remove()
        content?.children()?.remove()
        content?.append(chapter)

        return doc
    }
}

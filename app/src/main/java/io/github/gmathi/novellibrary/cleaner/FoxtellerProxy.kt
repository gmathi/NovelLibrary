
package io.github.gmathi.novellibrary.cleaner

import android.util.Base64
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.gmathi.novellibrary.network.HostNames
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class FoxtellerProxy : ProxyHelper() {
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
    override fun document(res: Connection.Response): Document {
        val doc = res.parse()
        val xsrf = res.cookie("XSRF-TOKEN")
        val session = res.cookie("foxteller_session") ?: xsrf
        val csrf = doc.selectFirst("meta[name=\"csrf-token\"]").attr("content")

        // remove chapter loading script as we load ourselves
        doc.selectFirst("script[src*=chapter.js]").remove()

        val dem = mutableMapOf<String, String>()
        doc.getElementsByTag("script").forEach { script ->
            dem_regex.findAll(script.data()).forEach { match ->
                dem[match.groups[1]?.value!!] = match.groups[2]?.value!!
            }
        }

        val data = JsonObject()
        data.addProperty("x1", dem["d"])
        data.addProperty("x2", dem["e"])

        val aux = connect(aux_dem).referrer(doc.location())
            .ignoreContentType(true)
            .method(Connection.Method.POST).requestBody(data.toString())
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:78.0) Gecko/20100101 Firefox/78.0")
            .header("Origin", "https://www.${HostNames.FOXTELLER}")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Content-Type", "application/json;charset=utf-8")

            .cookie("theme", "light")
            .cookie("font-size", "16")
            .cookie("gdb1", "true")

            .cookie("foxteller_session", session)
            .cookie("XSRF-TOKEN", xsrf)
            .header("X-XSRF-TOKEN", xsrf)
            .header("X-CSRF-TOKEN", csrf)
            .header("TE", "trailers")
            .execute()

        if (aux.statusCode() != 200) {
            val content = doc.getElementById("chapter-content")
            content.children().remove()
            content.append("<p><b>ERROR: Could not load chapter content (${aux.statusCode()}).</b></p>")
            return doc
        }

        var chapter = JsonParser.parseString(aux.body())!!.asJsonObject!!["aux"]!!.asString!!

        chapter = chapter.replace(decode_regex) { match ->
            charmap.getValue(match.groups[1]!!.value).toString()
        }
        chapter = Base64.decode(chapter, Base64.DEFAULT).decodeToString()

        val content = doc.getElementById("chapter-content")
        content.textNodes().firstOrNull()?.remove()
        content.children().remove()
        content.append(chapter)

        return doc
    }
}

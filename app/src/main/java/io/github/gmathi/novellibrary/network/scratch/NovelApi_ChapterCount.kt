package io.github.gmathi.novellibrary.network.scratch//package io.github.gmathi.novellibrary.network
//
//import com.google.gson.Gson
//import com.google.gson.internal.LinkedTreeMap
//import com.google.gson.reflect.TypeToken
//import io.github.gmathi.novellibrary.model.database.Novel
//import org.jsoup.Jsoup
//import java.net.URI
//
//
//fun NovelApi.getChapterCount(novel: Novel): Int {
//    val host = URI(novel.url).host
//    when {
//        host.contains(HostNames.NOVEL_UPDATES)
//                || host.contains(HostNames.ROYAL_ROAD_OLD) || host.contains(HostNames.ROYAL_ROAD)
//                || host.contains(HostNames.WLN_UPDATES)
//                || host.contains(HostNames.NOVEL_FULL)
//        -> return getChapterUrls(novel)?.size ?: 0
//
//        host.contains(HostNames.LNMTL) -> return getLNMTLChapterCount(novel.url)
//    }
//    return 0
//}
//
//fun getLNMTLChapterCount(url: String): Int {
//    try {
//        val doc = Jsoup.connect(url).get()
//
//        val scripts = doc.select("script")
//        val script = scripts.find { it.html().contains("lnmtl.firstResponse =") } ?: return 0
//        val text = script.html()
//
//        val json = text.substring(text.indexOf("lnmtl.firstResponse =") + 21)
//            .substringBefore(";lnmtl.volumes =")
//
//        val type = object : TypeToken<Map<String, Any>>() {}.type
//        val data: LinkedTreeMap<String, Any> = Gson().fromJson(json, type) ?: return 0
//
//        when (val total = data["total"]) {
//            is Int -> return total
//            is Double -> return total.toInt()
//            is String -> return total.toInt()
//        }
//    } catch (e: Exception) {
//        e.printStackTrace()
//    }
//    return 0
//}
//

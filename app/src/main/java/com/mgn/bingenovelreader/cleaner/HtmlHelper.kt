package com.mgn.bingenovelreader.cleaner

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.mgn.bingenovelreader.network.HostNames
import com.mgn.bingenovelreader.service.DownloadService
import com.mgn.bingenovelreader.util.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File
import java.io.FileOutputStream


open class HtmlHelper protected constructor() {

    private val TAG = "HtmlHelper"

    companion object {
        fun getInstance(host: String): HtmlHelper {
            when {
                host.contains(Constants.NovelSites.ROYAL_ROAD) -> return RoyalRoadHelper()
                host.contains(Constants.NovelSites.KOBATOCHAN) -> return KobatochanCleaner()
            }
            return HtmlHelper()
        }
    }


    open fun removeJS(doc: Document) {
        doc.getElementsByTag("script").remove()
    }

    open fun downloadCSS(doc: Document, downloadDir: File) {
        val elements = doc.head().getElementsByTag("link").filter { element -> element.hasAttr("rel") && element.attr("rel") == "stylesheet" }
        for (element in elements) {
            val cssFile = downloadFile(element, downloadDir)
            if (cssFile != null) {
                element.removeAttr("href")
                element.attr("href", "../" + cssFile.name)
                //doc.head().appendElement("link").attr("rel", "stylesheet").attr("type", "text/css").attr("href", "../" + cssFile.name)
            }
        }
    }

    open fun cleanDoc(doc: Document) {

    }

    open fun downloadFile(element: Element, dir: File): File? {
        val uri = Uri.parse(element.absUrl("href"))
        val file: File
        val doc: Document
        try {
            if (uri.scheme == null || uri.host == null) throw Exception("Invalid URI: " + uri.toString())
            val fileName = uri.getFileName()
            file = File(dir, fileName)
            doc = Jsoup.connect(uri.toString()).userAgent(HostNames.USER_AGENT).ignoreContentType(true).get()
        } catch (e: Exception) {
            Log.w(TAG, "Uri: $uri", e)
            return null
        }
        return convertDocToFile(doc, file)
    }

    open fun convertDocToFile(doc: Document, file: File): File? {
        try {
            if (file.exists()) return file
            val stream = FileOutputStream(file)
            val content = doc.toString()
            stream.use { it.write(content.toByteArray()) }
        } catch (e: Exception) {
            Log.w(TAG, "convertDocToFile: ${file.name}", e)
            return null
        }
        return file
    }

    open fun downloadImages(doc: Document, novelDir: File) {
        val elements = doc.getElementsByTag("img").filter { element -> element.hasAttr("src") }
        for (element in elements) {
            val imageFile = downloadImage(element, novelDir)
            if (imageFile != null) {
                element.removeAttr("src")
                element.attr("src", "./${imageFile.name}")
            }
        }
    }

    open fun downloadImage(element: Element, dir: File): File? {
        val uri = Uri.parse(element.attr("src"))
        val file: File
        try {
            if (uri.scheme == null || uri.host == null) throw Exception("Invalid URI: " + uri.toString())
            val fileName = uri.lastPathSegment.writableFileName()
            file = File(dir, fileName)
            val response = Jsoup.connect(uri.toString()).userAgent(HostNames.USER_AGENT).ignoreContentType(true).execute()
            val bytes = response.bodyAsBytes()
            val bitmap = Util.getImage(bytes)
            val os = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
        } catch (e: Exception) {
            Log.w(DownloadService.TAG, uri.toString(), e)
            return null
        }
        return file
    }

    fun getTitle(doc: Document): String? {
        return doc.head().getElementsByTag("title").text()
    }


}
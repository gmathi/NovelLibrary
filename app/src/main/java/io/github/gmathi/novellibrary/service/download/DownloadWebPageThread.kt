package io.github.gmathi.novellibrary.service.download

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import io.github.gmathi.novellibrary.cleaner.HtmlHelper
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.model.*
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.writableFileName
import org.jsoup.nodes.Document
import java.io.File


class DownloadWebPageThread(val context: Context, val download: Download, val dbHelper: DBHelper, private val downloadListener: DownloadListener) : Thread(), DownloadListener {

    companion object {
        private const val TAG = "DownloadWebPageThread"
    }

    private lateinit var hostDir: File
    private lateinit var novelDir: File

    override fun run() {
        try {
            if (isNetworkDown()) throw InterruptedException(Constants.NO_NETWORK)

            val webPageSettings = dbHelper.getWebPageSettings(download.webPageUrl)!!
            val webPage = dbHelper.getWebPage(download.webPageUrl)!!

            hostDir = Utils.getHostDir(context, webPageSettings.url)
            novelDir = Utils.getNovelDir(hostDir, download.novelName)

            dbHelper.updateDownloadStatusWebPageUrl(Download.STATUS_RUNNING, download.webPageUrl)
            downloadListener.handleEvent(DownloadWebPageEvent(EventType.RUNNING, webPageSettings.url, download))

            val downloadComplete = downloadChapter(webPageSettings, webPage)
            if (downloadComplete) {
                dbHelper.deleteDownload(download.webPageUrl)
                //downloadListener.handleEvent(DownloadWebPageEvent(EventType.COMPLETE, webPageSettings.url, download))
            } else {
                Logs.error(TAG, "Download did not complete!")
            }

        } catch (e: InterruptedException) {
            Logs.error(TAG, "Interrupting the Thread: ${download.novelName}-${download.webPageUrl}, ${e.localizedMessage}")
        } catch (e: Exception) {
            Logs.error(TAG, "This is really bad!!", e)
        }

    }

    private fun downloadChapter(webPageSettings: WebPageSettings, webPage: WebPage): Boolean {
        val doc: Document
        try {
            doc = NovelApi.getDocument(webPageSettings.url)
        } catch (e: Exception) {
            Logs.error(TAG, "Error getting WebPage: ${webPageSettings.url}")
            return false
        }

        val uri = Uri.parse(doc.location())
        if (!uri.host.isNullOrBlank()) {

            val htmlHelper = HtmlHelper.getInstance(doc, uri.host ?: doc.location())
            htmlHelper.downloadResources(doc, hostDir, novelDir)
            webPageSettings.title = htmlHelper.getTitle(doc)
            val file = htmlHelper.convertDocToFile(doc, File(novelDir, webPageSettings.title!!.writableFileName()))
                ?: return false
            webPageSettings.filePath = file.path
            webPageSettings.redirectedUrl = doc.location()

            // We need to clean up the document to get only valid linked URLS
            htmlHelper.removeJS(doc)
            htmlHelper.additionalProcessing(doc)
            htmlHelper.setProperHrefUrls(doc)

            // Now we extract other links from the cleaned doc
            val otherLinks = htmlHelper.getLinkedChapters(doc)
            if (otherLinks.isNotEmpty()) {
                val otherWebPages = ArrayList<WebPageSettings>()
                otherLinks.mapNotNullTo(otherWebPages) { downloadOtherChapterLinks(it, webPage.novelId, hostDir, novelDir) }
                webPageSettings.metaData[Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES] = Gson().toJson(otherLinks)
                otherWebPages.forEach {
                    dbHelper.createWebPageSettings(it)
                }
            }

            if (webPageSettings.metaData.containsKey(Constants.DOWNLOADING))
                webPageSettings.metaData.remove(Constants.DOWNLOADING)
            dbHelper.updateWebPageSettings(webPageSettings)
            return true
        }
        return false
    }

    private fun downloadOtherChapterLinks(otherChapterLink: String, novelId: Long, hostDir: File, novelDir: File): WebPageSettings? {

        val doc: Document
        try {
            doc = NovelApi.getDocument(otherChapterLink)
        } catch (e: Exception) {
            Logs.error(TAG, "Error getting internal links: $otherChapterLink")
            e.printStackTrace()
            return null
        }

        val uri = Uri.parse(doc.location())
        if (uri.host.isNullOrBlank()) return null

        val webPageSettings = WebPageSettings(otherChapterLink, novelId)
        val htmlHelper = HtmlHelper.getInstance(doc, uri.host ?: doc.location())
        htmlHelper.downloadResources(doc, hostDir, novelDir)

        webPageSettings.title = htmlHelper.getTitle(doc)
        if (webPageSettings.title != null && webPageSettings.title == "") webPageSettings.title = doc.location()

        val file = htmlHelper.convertDocToFile(doc, File(novelDir, webPageSettings.title!!.writableFileName()))
            ?: return null
        webPageSettings.filePath = file.path
        webPageSettings.redirectedUrl = doc.location()
        return webPageSettings
    }


    private fun onNoNetwork() {
        Logs.info(TAG, Constants.NO_NETWORK)
    }

    private fun isNetworkDown(): Boolean {
        if (!Utils.isConnectedToNetwork(context)) {
            onNoNetwork()
            return true
        }
        return false
    }

    override fun handleEvent(downloadWebPageEvent: DownloadWebPageEvent) {
        downloadListener.handleEvent(downloadWebPageEvent)
    }

    override fun handleEvent(downloadNovelEvent: DownloadNovelEvent) {
        downloadListener.handleEvent(downloadNovelEvent)
    }
}
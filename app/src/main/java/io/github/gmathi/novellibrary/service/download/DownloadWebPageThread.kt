package io.github.gmathi.novellibrary.service.download

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import io.github.gmathi.novellibrary.cleaner.HtmlCleaner
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.model.database.Download
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.model.other.DownloadNovelEvent
import io.github.gmathi.novellibrary.model.other.DownloadWebPageEvent
import io.github.gmathi.novellibrary.model.other.EventType
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.network.WebPageDocumentFetcher
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.network.getFileName
import org.jsoup.nodes.Document
import java.io.File


class DownloadWebPageThread(val context: Context, val download: Download, val dbHelper: DBHelper, private val downloadListener: DownloadListener, private val networkHelper: NetworkHelper, private val webPageDocumentFetcher: WebPageDocumentFetcher, private val dataCenter: io.github.gmathi.novellibrary.model.preference.DataCenter) : Thread(), DownloadListener {

    companion object {
        private const val TAG = "DownloadWebPageThread"
    }

    private lateinit var novelDir: File
    // NetworkHelper is now injected via constructor

    override fun run() {
        try {
            if (isNetworkDown()) throw InterruptedException(Constants.NO_NETWORK)

            val webPageSettings = dbHelper.getWebPageSettings(download.webPageUrl)!!
            val webPage = dbHelper.getWebPage(download.webPageUrl)!!

            novelDir = Utils.getNovelDir(context, download.novelName, download.novelId)

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
            doc = webPageDocumentFetcher.document(webPageSettings.url)
        } catch (e: Exception) {
            Logs.error(TAG, "Error getting WebPage: ${webPageSettings.url}")
            return false
        }

        val uri = Uri.parse(doc.location())
        if (uri.host.isNullOrBlank()) return false

        val htmlHelper = HtmlCleaner.getInstance(doc, uri.host ?: doc.location(), dataCenter)
        htmlHelper.downloadResources(doc, novelDir)

        val file = htmlHelper.convertDocToFile(doc, File(novelDir, uri.getFileName())) ?: return false

        webPageSettings.title = htmlHelper.getTitle(doc)
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
            otherLinks.mapNotNullTo(otherWebPages) { downloadOtherChapterLinks(it.href, webPage.novelId, novelDir) }
            webPageSettings.metadata[Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES] = Gson().toJson(otherLinks)
            otherWebPages.forEach {
                dbHelper.createWebPageSettings(it)
            }
        }

        if (webPageSettings.metadata.containsKey(Constants.DOWNLOADING))
            webPageSettings.metadata.remove(Constants.DOWNLOADING)
        dbHelper.updateWebPageSettings(webPageSettings)
        return true
    }

    private fun downloadOtherChapterLinks(otherChapterLink: String, novelId: Long, novelDir: File): WebPageSettings? {

        val doc: Document
        try {
            doc = webPageDocumentFetcher.document(otherChapterLink)
        } catch (e: Exception) {
            Logs.error(TAG, "Error getting internal links: $otherChapterLink")
            e.printStackTrace()
            return null
        }

        val uri = Uri.parse(doc.location())
        if (uri.host.isNullOrBlank()) return null

        val webPageSettings = WebPageSettings(otherChapterLink, novelId)
        val htmlHelper = HtmlCleaner.getInstance(doc, uri.host ?: doc.location(), dataCenter)
        htmlHelper.downloadResources(doc, novelDir)

        val file = htmlHelper.convertDocToFile(doc, File(novelDir, uri.getFileName())) ?: return null

        val title = htmlHelper.getTitle(doc)
        webPageSettings.title = if (title.isNullOrBlank()) doc.location() else title
        webPageSettings.filePath = file.path
        webPageSettings.redirectedUrl = doc.location()
        return webPageSettings
    }


    private fun onNoNetwork() {
        Logs.info(TAG, Constants.NO_NETWORK)
    }

    private fun isNetworkDown(): Boolean {
        if (!networkHelper.isConnectedToNetwork()) {
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
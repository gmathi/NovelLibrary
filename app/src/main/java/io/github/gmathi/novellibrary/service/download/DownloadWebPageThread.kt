package io.github.gmathi.novellibrary.service.download

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import io.github.gmathi.novellibrary.cleaner.HtmlHelper
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.model.Download
import io.github.gmathi.novellibrary.model.DownloadWebPageEvent
import io.github.gmathi.novellibrary.model.EventType
import io.github.gmathi.novellibrary.model.WebPage
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.writableFileName
import org.greenrobot.eventbus.EventBus
import org.jsoup.helper.StringUtil
import org.jsoup.nodes.Document
import java.io.File


class DownloadWebPageThread(val context: Context, val download: Download, val dbHelper: DBHelper) : Thread() {

    companion object {
        private const val TAG = "DownloadWebPageThread"
    }

    private lateinit var hostDir: File
    private lateinit var novelDir: File

    override fun run() {
        try {
            if (isNetworkDown()) throw InterruptedException(Constants.NO_NETWORK)

            val webPage = dbHelper.getWebPage(download.webPageId)!!

            hostDir = Utils.getHostDir(context, webPage.url)
            novelDir = Utils.getNovelDir(hostDir, download.novelName)

            dbHelper.updateDownloadStatus(Download.STATUS_RUNNING, download.webPageId)
            EventBus.getDefault().post(DownloadWebPageEvent(EventType.RUNNING, webPage.id, download))

            if (downloadChapter(webPage)) {
                dbHelper.deleteDownload(download.webPageId)
                EventBus.getDefault().post(DownloadWebPageEvent(EventType.COMPLETE, webPage.id, download))
            }

        } catch (e: InterruptedException) {
            Utils.error(TAG, "Interrupting the Thread: ${download.novelName}-${download.webPageId}, ${e.localizedMessage}")
        } catch (e: Exception) {
            Utils.error(TAG, "This is really bad!!", e)
        }

    }

    private fun downloadChapter(webPage: WebPage): Boolean {
        val doc: Document
        try {
            doc = NovelApi().getDocumentWithUserAgent(webPage.url)
        } catch (e: Exception) {
            Utils.error(TAG, "Error getting WebPage: ${webPage.url}")
            e.printStackTrace()
            return false
        }

        val uri = Uri.parse(doc.location())
        if (!StringUtil.isBlank(uri.host)) {

            val htmlHelper = HtmlHelper.getInstance(doc, uri.host)
            htmlHelper.clean(doc, hostDir, novelDir)
            webPage.title = htmlHelper.getTitle(doc)
            val file = htmlHelper.convertDocToFile(doc, File(novelDir, webPage.title!!.writableFileName())) ?: return false
            webPage.filePath = file.path
            webPage.redirectedUrl = doc.location()

            val otherLinks = htmlHelper.getLinkedChapters(doc)
            if (otherLinks.isNotEmpty()) {
                val otherWebPages = ArrayList<WebPage>()
                otherLinks.mapNotNullTo(otherWebPages) { it -> downloadOtherChapterLinks(it, hostDir, novelDir) }
                webPage.metaData[Constants.MD_OTHER_LINKED_WEB_PAGES] = Gson().toJson(otherWebPages)
            }

            if (webPage.metaData.containsKey(Constants.DOWNLOADING))
                webPage.metaData.remove(Constants.DOWNLOADING)
            val id = dbHelper.updateWebPage(webPage)
            return (id.toInt() != -1)
        }
        return false
    }

    private fun downloadOtherChapterLinks(otherChapterLink: String, hostDir: File, novelDir: File): WebPage? {

        val doc: Document
        try {
            doc = NovelApi().getDocumentWithUserAgent(otherChapterLink)
        } catch (e: Exception) {
            Utils.error(TAG, "Error getting WebPage: $otherChapterLink")
            e.printStackTrace()
            return null
        }

        val uri = Uri.parse(doc.location())
        if (StringUtil.isBlank(uri.host)) return null

        val otherWebPage = WebPage(otherChapterLink, doc.title())
        val htmlHelper = HtmlHelper.getInstance(doc, uri.host)
        htmlHelper.clean(doc, hostDir, novelDir)

        otherWebPage.title = htmlHelper.getTitle(doc)
        if (otherWebPage.title != null && otherWebPage.title == "") otherWebPage.title = doc.location()

        otherWebPage.id = -2L

        val file = htmlHelper.convertDocToFile(doc, File(novelDir, otherWebPage.title!!.writableFileName())) ?: return null
        otherWebPage.filePath = file.path
        otherWebPage.redirectedUrl = doc.location()
        return otherWebPage
    }


    private fun onNoNetwork() {
        Utils.error(TAG, Constants.NO_NETWORK)
    }

    private fun isNetworkDown(): Boolean {
        if (!Utils.checkNetwork(context)) {
            onNoNetwork()
            return true
        }
        return false
    }

}
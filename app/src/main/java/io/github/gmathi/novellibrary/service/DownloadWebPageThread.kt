package io.github.gmathi.novellibrary.service

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import io.github.gmathi.novellibrary.cleaner.HtmlHelper
import io.github.gmathi.novellibrary.database.getDownloadQueue
import io.github.gmathi.novellibrary.database.updateDownloadQueueStatus
import io.github.gmathi.novellibrary.database.updateWebPage
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.model.EventType
import io.github.gmathi.novellibrary.model.NovelEvent
import io.github.gmathi.novellibrary.model.WebPage
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.writableFileName
import org.greenrobot.eventbus.EventBus
import org.jsoup.helper.StringUtil
import org.jsoup.nodes.Document
import java.io.File


class DownloadWebPageThread(val context: Context, val webPage: WebPage, private val hostDir: File, private val novelDir: File, val isCompleteNovelDownload: Boolean) : Thread() {

    private val TAG = "DownloadWebPageThread"

    override fun run() {
        super.run()
        try {
            if (isNetworkDown()) throw InterruptedException(Constants.NO_NETWORK)
            if (webPage.filePath != null) return

            if (isCompleteNovelDownload) {
                val dq = dbHelper.getDownloadQueue(webPage.novelId)
                if (dq == null || dq.status == Constants.STATUS_STOPPED) return
            }

            if (downloadChapter(webPage)) {

                if (isCompleteNovelDownload) {
                    val dq = dbHelper.getDownloadQueue(webPage.novelId)
                    if (dq == null || dq.status == Constants.STATUS_STOPPED) return
                }
                EventBus.getDefault().post(NovelEvent(EventType.UPDATE, webPage.novelId, webPage))
            }

        } catch (e: InterruptedException) {
            Log.w(TAG, "Interrupting the Thread: ${webPage.novelId}, ${e.localizedMessage}")
        } catch (e: Exception) {
            Log.e(TAG, "This is really bad!!", e)
        }

    }

    private fun downloadChapter(webPage: WebPage): Boolean {
        val doc: Document
        try {
            doc = NovelApi().getDocumentWithUserAgent(webPage.url!!)
        } catch (e: Exception) {
            Log.w(DownloadNovelService.TAG, webPage.url!!)
            e.printStackTrace()
            return false
        }

        val uri = Uri.parse(doc.location())
        if (!StringUtil.isBlank(uri.host)) {

            val htmlHelper = HtmlHelper.getInstance(uri.host)
            htmlHelper.clean(doc, hostDir, novelDir)
            webPage.title = htmlHelper.getTitle(doc)
            val file = htmlHelper.convertDocToFile(doc, File(novelDir, webPage.title!!.writableFileName())) ?: return false
            webPage.filePath = file.path
            webPage.redirectedUrl = doc.location()

            val otherLinks = htmlHelper.getLinkedChapters(doc)
            if (otherLinks.isNotEmpty()) {
                val otherWebPages = ArrayList<WebPage>()
                otherLinks.mapNotNullTo(otherWebPages) { it -> downloadOtherChapterLinks(it, hostDir, novelDir) }
                webPage.metaData.put(Constants.MD_OTHER_LINKED_WEB_PAGES, Gson().toJson(otherWebPages))
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
            Log.w(DownloadNovelService.TAG, otherChapterLink)
            e.printStackTrace()
            return null
        }

        val uri = Uri.parse(doc.location())
        if (StringUtil.isBlank(uri.host)) return null

        val otherWebPage = WebPage(otherChapterLink, doc.title())
        val htmlHelper = HtmlHelper.getInstance(uri.host)
        htmlHelper.clean(doc, hostDir, novelDir)
        otherWebPage.title = htmlHelper.getTitle(doc)
        otherWebPage.id = -2L

        val file = htmlHelper.convertDocToFile(doc, File(novelDir, otherWebPage.title!!.writableFileName())) ?: return null
        otherWebPage.filePath = file.path
        otherWebPage.redirectedUrl = doc.location()
        return otherWebPage
    }


    private fun onNoNetwork() {
        Log.e(DownloadNovelService.TAG, Constants.NO_NETWORK)
        dbHelper.updateDownloadQueueStatus(Constants.STATUS_STOPPED.toLong(), webPage.novelId)
        EventBus.getDefault().post(NovelEvent(EventType.UPDATE, -1L))
    }

    private fun isNetworkDown(): Boolean {
        if (!Utils.checkNetwork(context)) {
            onNoNetwork()
            return true
        }
        return false
    }

}
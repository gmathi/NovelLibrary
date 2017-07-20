package com.mgn.bingenovelreader.service

import android.content.Context
import android.net.Uri
import android.util.Log
import com.mgn.bingenovelreader.cleaner.HtmlHelper
import com.mgn.bingenovelreader.database.DBHelper
import com.mgn.bingenovelreader.database.updateDownloadQueueStatus
import com.mgn.bingenovelreader.database.updateWebPage
import com.mgn.bingenovelreader.dbHelper
import com.mgn.bingenovelreader.event.EventType
import com.mgn.bingenovelreader.event.NovelEvent
import com.mgn.bingenovelreader.extension.writableFileName
import com.mgn.bingenovelreader.model.WebPage
import com.mgn.bingenovelreader.network.NovelApi
import com.mgn.bingenovelreader.util.Constants
import com.mgn.bingenovelreader.util.Utils
import org.greenrobot.eventbus.EventBus
import org.jsoup.helper.StringUtil
import org.jsoup.nodes.Document
import java.io.File


class DownloadWebPageThread(val context: Context, val webPage: WebPage, val hostDir: File, val novelDir: File) : Thread() {

    private val TAG = "DownloadWebPageThread"

    override fun run() {
        super.run()
        try {
            if (isNetworkDown()) throw InterruptedException(Constants.NO_NETWORK)
            if (downloadChapter(webPage, hostDir, novelDir)) {
                EventBus.getDefault().post(NovelEvent(EventType.UPDATE, webPage.novelId, webPage))
            }
        } catch (e: InterruptedException) {
            Log.w(TAG, "Interrupting the Thread: ${webPage.novelId}, ${e.localizedMessage}")
        } catch (e: Exception) {
            Log.e(TAG, "This is really bad!!", e)
        }

    }

    private fun downloadChapter(webPage: WebPage, hostDir: File, novelDir: File): Boolean {
        val doc: Document
        try {
            doc = NovelApi().getDocumentWithUserAgent(webPage.url!!)
        } catch (e: Exception) {
            Log.w(DownloadService.TAG, webPage.url!!)
            e.printStackTrace()
            return false
        }

        val uri = Uri.parse(doc.location())
        if (!StringUtil.isBlank(uri.host)) {

            val htmlHelper = HtmlHelper.getInstance(uri.host)
            htmlHelper.clean(doc, hostDir, novelDir)
            webPage.title = htmlHelper.getTitle(doc)

            val file = htmlHelper.convertDocToFile(doc, File(novelDir, webPage.title!!.writableFileName()))
            if (file != null) {
                webPage.filePath = file.path
                webPage.redirectedUrl = doc.location()
                val id = DBHelper(context).updateWebPage(webPage)
                return (id.toInt() != -1)
            }
        }
        return false
    }

    private fun onNoNetwork() {
        Log.e(DownloadService.TAG, Constants.NO_NETWORK)
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
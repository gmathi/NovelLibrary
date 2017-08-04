package io.github.gmathi.novellibrary.service

import android.content.Context
import android.net.Uri
import android.util.Log
import io.github.gmathi.novellibrary.cleaner.HtmlHelper
import io.github.gmathi.novellibrary.database.DBHelper
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


class DownloadWebPageThread(val context: Context, val webPage: WebPage, val hostDir: File, val novelDir: File) : Thread() {

    private val TAG = "DownloadWebPageThread"

    override fun run() {
        super.run()
        try {
            if (isNetworkDown()) throw InterruptedException(Constants.NO_NETWORK)
            if (webPage.filePath != null) return
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
            Log.w(DownloadNovelService.TAG, webPage.url!!)
            e.printStackTrace()
            return false
        }

        val uri = Uri.parse(doc.location())
        if (!StringUtil.isBlank(uri.host)) {

            val htmlHelper = HtmlHelper.getInstance(uri.host)
            htmlHelper.clean(doc, hostDir, novelDir)

//            val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
//            if (prefs.getBoolean("cleanPages", false)) {
//                htmlHelper.removeJS(doc)
//                htmlHelper.additionalProcessing(doc)
//            }

            webPage.title = htmlHelper.getTitle(doc)

            val file = htmlHelper.convertDocToFile(doc, File(novelDir, webPage.title!!.writableFileName()))
            if (file != null) {
                webPage.filePath = file.path
                webPage.redirectedUrl = doc.location()
                if (webPage.metaData.containsKey(Constants.DOWNLOADING))
                    webPage.metaData.remove(Constants.DOWNLOADING)
                val id = DBHelper(context).updateWebPage(webPage)
                return (id.toInt() != -1)
            }
        }
        return false
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
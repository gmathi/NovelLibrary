package com.mgn.bingenovelreader.services

import android.app.IntentService
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.mgn.bingenovelreader.cleaners.HtmlHelper
import com.mgn.bingenovelreader.database.*
import com.mgn.bingenovelreader.models.DownloadQueue
import com.mgn.bingenovelreader.models.Novel
import com.mgn.bingenovelreader.models.WebPage
import com.mgn.bingenovelreader.network.NovelApi
import com.mgn.bingenovelreader.utils.Constants
import com.mgn.bingenovelreader.utils.Util
import com.mgn.bingenovelreader.utils.writableFileName
import org.jsoup.helper.StringUtil
import org.jsoup.nodes.Document
import java.io.File
import java.util.*


class DownloadService : IntentService(TAG) {

    lateinit var dbHelper: DBHelper

    //static components
    companion object {
        val TAG = "DownloadService"
        var IS_DOWNLOADING = false
        var NOVEL_ID = -1L
    }

    override fun onHandleIntent(workIntent: Intent) {
        dbHelper = DBHelper(applicationContext)
        NOVEL_ID = workIntent.getLongExtra(Constants.NOVEL_ID, -1L)

        if (!Util.checkNetwork(this)) {
            onNoNetwork()
            return
        }

        android.os.Debug.waitForDebugger()
        if (!IS_DOWNLOADING) {
            IS_DOWNLOADING = true
            checkDownloadQueue()
            IS_DOWNLOADING = false
            NOVEL_ID = -1L
        }
    }

    private fun checkDownloadQueue() {
        var downloadQueue = dbHelper.getFirstDownloadableQueueItem()
        if (NOVEL_ID != -1L)
            downloadQueue = dbHelper.getDownloadQueue(NOVEL_ID)
        while (downloadQueue != null) {
            val novel = dbHelper.getNovel(downloadQueue.novelId)
            if (novel != null) {
                NOVEL_ID = novel.id
                startDownload(novel, downloadQueue)
            }
            downloadQueue = dbHelper.getFirstDownloadableQueueItem()
        }
    }


    fun startDownload(novel: Novel, downloadQueue: DownloadQueue) {

        val chapters: ArrayList<WebPage>

        //If chapter URLS were not downloaded
        if (downloadQueue.chapterUrlsCached == 0L) {

            if (!Util.checkNetwork(this)) {
                onNoNetwork()
                return
            }
            //download the chapter urls
            chapters = NovelApi().getChapterUrls(novel.url!!)

            //Insert the webPages for future download (in-case they pause the download and start it later)
            if (chapters.isNotEmpty()) {
                chapters.asReversed().forEach {
                    it.novelId = novel.id
                    val dbWebPage = dbHelper.getWebPage(it.novelId, it.url!!)
                    if (dbWebPage == null)
                        it.id = dbHelper.createWebPage(it)
                    else
                        it.id = dbWebPage.id
                }
            }

            //Update database with chapters urls cached
            dbHelper.updateChapterUrlsCached(1, novel.id)
        } else {
            chapters = ArrayList<WebPage>(dbHelper.getAllWebPages(novel.id))
        }

        val totalChapterCount = chapters.size
        if (dbHelper.getNovel(novel.id) == null) { //If the novel was deleted
            dbHelper.cleanupNovelData(novel.id)
            return
        }

        val hostDir = Util.getHostDir(applicationContext, novel.url!!)
        val novelDir = Util.getNovelDir(hostDir, novel.name!!)

        run runDownloads@ {
            if (chapters.isNotEmpty()) {
                //sendBroadcastUpdate(novel.id, totalChapterCount, totalChapterCount - chapters.size)
                chapters.asReversed().forEach {
                    if (!Util.checkNetwork(this)) {
                        onNoNetwork()
                        return
                    }
                    val dq = dbHelper.getDownloadQueue(it.novelId)
                    if (dq != null && dq.status.toInt() != Constants.STATUS_STOPPED) {
                        if (it.filePath == null) {
                            val downloadSuccess = downloadChapter(it, hostDir, novelDir)
                            if (downloadSuccess)
                                sendBroadcastUpdate(novel.id, totalChapterCount, dbHelper.getDownloadedChapterCount(novel.id))
                        }
                    } else
                        return@runDownloads //If downloads stopped or novel is deleted from database
                }
            }

            //If all downloads completed
            dbHelper.deleteDownloadQueue(downloadQueue.novelId)
            sendBroadcastDownloadComplete(downloadQueue.novelId)
        }
    }

    private fun downloadChapter(webPage: WebPage, hostDir: File, novelDir: File): Boolean {
        val doc: Document
        try {
            doc = NovelApi().getDocumentWithUserAgent(webPage.url!!)
        } catch (e: Exception) {
            Log.w(TAG, webPage.url!!)
            e.printStackTrace()
            return false
        }

        val uri = Uri.parse(doc.location())
        if (!StringUtil.isBlank(uri.host)) {

            val htmlHelper = HtmlHelper.getInstance(uri.host)
            htmlHelper.removeJS(doc)
            htmlHelper.downloadCSS(doc, hostDir)
            htmlHelper.downloadImages(doc, novelDir)
            htmlHelper.cleanDoc(doc)
            webPage.title = htmlHelper.getTitle(doc)
            val file = htmlHelper.convertDocToFile(doc, File(novelDir, webPage.title!!.writableFileName()))
            if (file != null) {
                webPage.filePath = file.path
                webPage.redirectedUrl = doc.location()
                val id = dbHelper.updateWebPage(webPage)
                return (id.toInt() != -1)
            }
        }
        return false
    }

    private fun sendBroadcast(extras: Bundle, action: String) {
        val localIntent = Intent()
        localIntent.action = action
        localIntent.putExtras(extras)
        localIntent.addCategory(Intent.CATEGORY_DEFAULT)
        sendBroadcast(localIntent)
    }

    private fun sendBroadcastUpdate(novelId: Long, totalChaptersCount: Int, currentChaptersCount: Int) {
        val extras = Bundle()
        extras.putLong(Constants.NOVEL_ID, novelId)
        extras.putInt(Constants.TOTAL_CHAPTERS_COUNT, totalChaptersCount)
        extras.putInt(Constants.CURRENT_CHAPTER_COUNT, currentChaptersCount)
        sendBroadcast(extras, Constants.DOWNLOAD_QUEUE_NOVEL_UPDATE)
    }

    private fun sendBroadcastDownloadComplete(novelId: Long) {
        val extras = Bundle()
        extras.putLong(Constants.NOVEL_ID, novelId)
        sendBroadcast(extras, Constants.DOWNLOAD_QUEUE_NOVEL_DOWNLOAD_COMPLETE)
    }

    private fun onNoNetwork() {
        Log.e(TAG, "No Active Internet")
        dbHelper.updateAllDownloadQueueStatuses(Constants.STATUS_STOPPED)
        sendBroadcastUpdate(-1L, 0, 0)
    }
}

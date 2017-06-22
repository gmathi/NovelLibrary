package com.mgn.bingenovelreader.services

import android.app.IntentService
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.mgn.bingenovelreader.database.DBHelper
import com.mgn.bingenovelreader.models.DownloadQueue
import com.mgn.bingenovelreader.models.Novel
import com.mgn.bingenovelreader.models.WebPage
import com.mgn.bingenovelreader.network.NovelApi
import com.mgn.bingenovelreader.utils.Constants
import com.mgn.bingenovelreader.utils.HostNames.USER_AGENT
import com.mgn.bingenovelreader.utils.getFileName
import com.mgn.bingenovelreader.utils.writableFileName
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File
import java.io.FileOutputStream
import java.util.*


class DownloadService : IntentService(TAG) {

    private var isDownloading = false
    lateinit var dbHelper: DBHelper

    //static components
    companion object {
        val TAG = "DownloadService"
    }

    override fun onHandleIntent(workIntent: Intent) {
        dbHelper = DBHelper(applicationContext)
        //android.os.Debug.waitForDebugger()
        if (!isDownloading) {
            isDownloading = true
            checkDownloadQueue()
            isDownloading = false
        }
    }

    private fun checkDownloadQueue() {
        var downloadQueue = dbHelper.firstDownloadableQueueItem
        while (downloadQueue != null) {
            val novel = dbHelper.getNovel(downloadQueue.novelId)
            startDownload(novel, downloadQueue)
            downloadQueue = dbHelper.firstDownloadableQueueItem
        }
    }


    fun startDownload(novel: Novel, downloadQueue: DownloadQueue) {
        val hostDir = getHostDir(novel.url!!)
        val novelDir = getNovelDir(hostDir, novel.name!!)

        val chapters: ArrayList<WebPage>

        //If chapter URLS were not downloaded
        if (downloadQueue.chapterUrlsCached == 0L) {

            //download the chapter urls
            chapters = NovelApi().getChapterUrls(novel)

            //Insert the webPages for future download (in-case they pause the download and start it later)
            if (chapters.isNotEmpty()) {
                chapters.asReversed().forEach {
                    it.novelId = novel.id
                    val dbWebPage = dbHelper.getWebPage(it.novelId, it.url)
                    if (dbWebPage == null)
                        dbHelper.createWebPage(it)
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

        run runDownloads@ {
            if (chapters.isNotEmpty()) {
                //sendBroadcastUpdate(novel.id, totalChapterCount, totalChapterCount - chapters.size)
                chapters.forEach {
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
            e.printStackTrace()
            return false
        }
        updateCSS(doc, hostDir)
        webPage.title = doc.head().getElementsByTag("title").text()
        val file = convertDocToFile(doc, File(novelDir, webPage.title!!.writableFileName()))
        webPage.filePath = file.path
        val id = dbHelper.updateWebPage(webPage)
        return (id.toInt() != -1)
    }

    private fun getHostDir(url: String): File {
        val uri = Uri.parse(url)
        val path = filesDir

        val dirName = uri.host.writableFileName()
        val hostDir = File(path, dirName)
        if (!hostDir.exists()) hostDir.mkdir()

        return hostDir
    }

    private fun getNovelDir(hostDir: File, novelName: String): File {
        val novelDir = File(hostDir, novelName.writableFileName())
        if (!novelDir.exists()) novelDir.mkdir()
        return novelDir
    }

    private fun updateCSS(doc: Document, hostDir: File) {
        val elements = doc.head().getElementsByTag("link").filter { element -> element.hasAttr("rel") && element.attr("rel") == "stylesheet" }
        for (element in elements) {
            val cssFile = downloadFile(element, hostDir)
            element.remove()
            if (cssFile != null)
                doc.head().appendElement("link").attr("rel", "stylesheet").attr("type", "text/css").attr("href", "" + cssFile.parentFile.name + "/" + cssFile.name)
        }
    }

    private fun downloadFile(element: Element, dir: File): File? {
        val uri = Uri.parse(element.absUrl("href"))
        val fileName = uri.getFileName()
        val file = File(dir, fileName)
        val doc: Document?
        try {
            doc = Jsoup.connect(uri.toString()).userAgent(USER_AGENT).ignoreContentType(true).get()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        return convertDocToFile(doc, file)
    }

    private fun convertDocToFile(doc: Document, file: File): File {
        if (file.exists()) return file
        val stream = FileOutputStream(file)
        val content = doc.body().html()
        stream.use { stream ->
            stream.write(content.toByteArray())
        }
        return file
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

}

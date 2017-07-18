package com.mgn.bingenovelreader.service

import android.content.Context
import android.util.Log
import com.mgn.bingenovelreader.dataCenter
import com.mgn.bingenovelreader.database.*
import com.mgn.bingenovelreader.event.EventType
import com.mgn.bingenovelreader.event.NovelEvent
import com.mgn.bingenovelreader.model.DownloadQueue
import com.mgn.bingenovelreader.model.Novel
import com.mgn.bingenovelreader.model.WebPage
import com.mgn.bingenovelreader.network.NovelApi
import com.mgn.bingenovelreader.util.Constants
import com.mgn.bingenovelreader.util.Utils
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


class DownloadNovelThread(val context: Context, val novelId: Long, val dbHelper: DBHelper) : Thread() {

    private val TAG = "DownloaderNovelTask"
    private var threadPool: ThreadPoolExecutor? = null

    override fun run() {
        super.run()
        try {
            val downloadQueue = dbHelper.getDownloadQueue(novelId)
            val novel = dbHelper.getNovel(downloadQueue!!.novelId)
            if (novel != null) {
                startDownload(novel, downloadQueue)
            }

        } catch (e: InterruptedException) {
            Log.w(TAG, "Interrupting the Thread: $novelId, ${e.localizedMessage}")
            threadPool?.shutdownNow()
        } catch (e: Exception) {
            Log.e(TAG, "This is really bad!!", e)
            threadPool?.shutdownNow()
        } finally {
            DownloadService.novelThreadMap.remove(novelId)
        }
    }

    fun startDownload(novel: Novel, downloadQueue: DownloadQueue) {
        if (isNetworkDown()) throw InterruptedException(Constants.NO_NETWORK)

        val chapters = getChapters(downloadQueue, url = novel.url!!)

        //If the novel was deleted
        if (dbHelper.getNovel(novel.id) == null) {
            dbHelper.cleanupNovelData(novel.id)
            return
        }

        //Get Directories to download html
        val hostDir = Utils.getHostDir(context, novel.url!!)
        val novelDir = Utils.getNovelDir(hostDir, novel.name!!)

        val poolSize = Math.min(10, chapters.size)
        threadPool = Executors.newFixedThreadPool(poolSize) as ThreadPoolExecutor

        run downloadChapters@ {

            chapters.forEach {
                if (isNetworkDown()) throw InterruptedException(Constants.NO_NETWORK)

                val dq = dbHelper.getDownloadQueue(it.novelId)
                if (dq != null && dq.status == Constants.STATUS_DOWNLOAD) {

                    if (dataCenter.experimentalDownload) {
                        threadPool?.execute(DownloadWebPageThread(context, it, hostDir, novelDir))
                    } else {
                        threadPool?.submit(DownloadWebPageThread(context, it, hostDir, novelDir))?.get()
                    }
                } else
                    return@downloadChapters //If downloads stopped or novel is deleted from database
            }
            threadPool?.shutdown()
            try {
                threadPool?.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
            } catch (e: InterruptedException) {
                Log.w(TAG, "Thread pool executor interrupted~")
            }

            //If all downloads completed
            dbHelper.deleteDownloadQueue(downloadQueue.novelId)
            EventBus.getDefault().post(NovelEvent(EventType.COMPLETE, novel.id))
        }
    }

    private fun getChapters(downloadQueue: DownloadQueue, url: String): List<WebPage> {
        if (downloadQueue.chapterUrlsCached == 0L) {                                //If chapter URLS were not downloaded
            var chapters: List<WebPage> = NovelApi().getChapterUrls(url)     //download the chapter urls

            //Insert the webPages for future download (in-case they pause the download and start it later)
            if (chapters.isNotEmpty()) {

                /* DOWNLOAD PRIORITY */
                /*
                if (dataCenter.downloadLatestFirst)
                    chapters = chapters.asReversed()
                */

                chapters.forEach {
                    it.novelId = downloadQueue.novelId
                    val dbWebPage = dbHelper.getWebPage(it.novelId, it.url!!)
                    if (dbWebPage == null)
                        it.id = dbHelper.createWebPage(it)
                    else
                        it.copyFrom(dbWebPage)
                }

                chapters = chapters.filter { it.filePath == null }
            }

            //Update database with novel - chapters urls cached
            dbHelper.updateChapterUrlsCached(1, downloadQueue.novelId)
            return chapters
        } else {
            return dbHelper.getAllWebPagesToDownload(downloadQueue.novelId)
        }
    }

    private fun onNoNetwork() {
        Log.e(DownloadService.TAG, Constants.NO_NETWORK)
        dbHelper.updateDownloadQueueStatus(Constants.STATUS_STOPPED.toLong(), novelId)
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
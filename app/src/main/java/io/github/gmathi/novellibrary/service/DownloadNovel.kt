package io.github.gmathi.novellibrary.service

import android.content.Context
import android.util.Log
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.event.EventType
import io.github.gmathi.novellibrary.event.NovelEvent
import io.github.gmathi.novellibrary.model.DownloadQueue
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.WebPage
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Utils
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


class DownloadNovel(val context: Context, val novelId: Long, val dbHelper: DBHelper) {

    private val TAG = "DownloaderNovelTask"
    private var threadPool: ThreadPoolExecutor? = null

    fun start() {
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

        //Get Directories to start html
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
            dbHelper.updateDownloadQueueStatus(Constants.STATUS_COMPLETE, downloadQueue.novelId)
            EventBus.getDefault().post(NovelEvent(EventType.COMPLETE, novel.id))
        }
    }

    private fun getChapters(downloadQueue: DownloadQueue, @Suppress("UNUSED_PARAMETER") url: String): List<WebPage> {
//        if (downloadQueue.chapterUrlsCached == 0L) {                                //If chapter URLS were not downloaded
//            var chapters: List<WebPage> = NovelApi().getChapterUrls(url)     //start the chapter urls
//
//            //Insert the webPages for future start (in-case they pause the start and start it later)
//            if (chapters.isNotEmpty()) {
//
//                /* DOWNLOAD PRIORITY */
//                /*
//                if (dataCenter.downloadLatestFirst)
//                    chapters = chapters.asReversed()
//                */
//
//                chapters.forEach {
//                    it.novelId = downloadQueue.novelId
//                    val dbWebPage = dbHelper.getWebPage(it.novelId, it.url!!)
//                    if (dbWebPage == null)
//                        it.id = dbHelper.createWebPage(it)
//                    else
//                        it.copyFrom(dbWebPage)
//                }
//
//                chapters = chapters.filter { it.filePath == null }
//            }
//
//            //Update database with novel - chapters urls cached
//            dbHelper.updateChapterUrlsCached(1, downloadQueue.novelId)
//            return chapters
//        } else {
            return dbHelper.getAllWebPagesToDownload(downloadQueue.novelId)
//        }
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
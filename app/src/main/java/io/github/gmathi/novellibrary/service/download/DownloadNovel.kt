package io.github.gmathi.novellibrary.service.download

import android.content.Context
import android.util.Log
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.model.*
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Utils
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


class DownloadNovel(val context: Context, val novelId: Long) {

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

        if (isDqStoppedOrCompleted(novel.id)) return
        val chapters =  dbHelper.getAllWebPagesToDownload(novelId)
            //NovelApi().getChapterUrls(novel)?.asReversed() ?: return


        //If the novel was deleted
        if (isDqStoppedOrCompleted(novel.id)) return
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

            (0 until chapters.size).asSequence().forEach {

                val webPage = dbHelper.getWebPage(novel.id, chapters[it].url!!) ?: chapters[it]
                if (webPage.id == -1L) {
                    webPage.orderId = it.toLong()
                    webPage.novelId = novel.id
                    webPage.id = dbHelper.createWebPage(webPage)
                }

                if (webPage.filePath != null) return@forEach
                if (isNetworkDown()) throw InterruptedException(Constants.NO_NETWORK)

                val dq = dbHelper.getDownloadQueue(webPage.novelId)
                if (dq != null && dq.status == Constants.STATUS_DOWNLOAD) {

                    if (dataCenter.experimentalDownload) {
                        threadPool?.execute(DownloadWebPageThread(context, webPage, hostDir, novelDir, true))
                    } else {
                        threadPool?.submit(DownloadWebPageThread(context, webPage, hostDir, novelDir, true))?.get()
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
            if (dbHelper.getAllWebPagesToDownload(novelId).isEmpty()) {
                dbHelper.updateDownloadQueueStatus(Constants.STATUS_COMPLETE, downloadQueue.novelId)
                EventBus.getDefault().post(NovelEvent(EventType.COMPLETE, novel.id))
            }
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
        Log.e(DownloadNovelService.TAG, Constants.NO_NETWORK)
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

    private fun isDqStoppedOrCompleted(novelId: Long): Boolean {
        val dqStatus = dbHelper.getDownloadQueue(novelId)?.status ?: return true
        return dqStatus == Constants.STATUS_STOPPED || dqStatus == Constants.STATUS_COMPLETE
    }


}
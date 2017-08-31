package io.github.gmathi.novellibrary.service

import android.app.IntentService
import android.content.Intent
import android.util.Log
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.updateWebPage
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.model.EventType
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.NovelEvent
import io.github.gmathi.novellibrary.model.WebPage
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Utils
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


class DownloadChapterService : IntentService(TAG) {

    //static components
    companion object {
        val TAG = "DownloadChapterService"
        var chapters = ArrayList<WebPage>()
    }

    override fun onHandleIntent(workIntent: Intent) {
        //android.os.Debug.waitForDebugger()
        @Suppress("UNCHECKED_CAST")
        val chapters: ArrayList<WebPage> = ArrayList(workIntent.getSerializableExtra("webPages") as ArrayList<WebPage>)
        val novel = workIntent.getSerializableExtra("novel") as Novel

        if (isNetworkDown()) return

        try {
            if (chapters.isNotEmpty())
                downloadChapters(novel, chapters)
        } catch (e: Exception) {
            Utils.error(TAG, "Exception Caught", e)
        } finally {
            DownloadChapterService.chapters = ArrayList()
        }
    }

    private fun downloadChapters(novel: Novel, chapters: ArrayList<WebPage>) {
        //Get Directories to start html
        DownloadChapterService.chapters = chapters
        chapters.forEach {
            it.metaData.put(Constants.DOWNLOADING, Constants.STATUS_DOWNLOAD.toString())
            dbHelper.updateWebPage(it)
            EventBus.getDefault().post(NovelEvent(EventType.UPDATE, novel.id, it))
        }

        val hostDir = Utils.getHostDir(this@DownloadChapterService, novel.url!!)
        val novelDir = Utils.getNovelDir(hostDir, novel.name!!)

        val poolSize = Math.min(10, chapters.size)
        val threadPool = Executors.newFixedThreadPool(poolSize) as ThreadPoolExecutor

        run download@ {

            chapters.forEach {
                if (isNetworkDown()) throw InterruptedException(Constants.NO_NETWORK)

                if (dataCenter.experimentalDownload) {
                    threadPool.execute(DownloadWebPageThread(this@DownloadChapterService, it, hostDir, novelDir, false))
                } else {
                    threadPool.submit(DownloadWebPageThread(this@DownloadChapterService, it, hostDir, novelDir, false))?.get()
                }
            }

            try {
                threadPool.shutdown()
                threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
            } catch (e: Exception) {
                Log.w(TAG, "Thread pool executor interrupted~")
            }
        }
    }


    private fun isNetworkDown(): Boolean {
        if (!Utils.checkNetwork(this)) {
            return true
        }
        return false
    }

}

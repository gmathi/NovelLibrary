package io.github.gmathi.novellibrary.service.download

import android.app.IntentService
import android.content.Intent
import android.util.Log
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.getDownloadItemInQueue
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Utils
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


class DownloadService : IntentService(TAG) {

    lateinit var dbHelper: DBHelper

    //static components
    companion object {
        val TAG = "DownloadService"
        val QUALIFIED_NAME = "io.github.gmathi.novellibrary.service.download.DownloadService"
    }

    override fun onHandleIntent(workIntent: Intent) {
        //android.os.Debug.waitForDebugger()
        @Suppress("UNCHECKED_CAST")
        if (isNetworkDown()) return

        dbHelper = DBHelper.getInstance(this@DownloadService)
        downloadChapters()
    }

    private fun downloadChapters() {

        //Get Directories to start html


        val poolSize = 10
        val threadPool = Executors.newFixedThreadPool(poolSize) as ThreadPoolExecutor

        var download = dbHelper.getDownloadItemInQueue()
        while (download != null) {
            if (isNetworkDown()) throw InterruptedException(Constants.NO_NETWORK)

            if (dataCenter.experimentalDownload) {
                threadPool.execute(DownloadWebPageThread(this@DownloadService, download))
            } else {
                threadPool.submit(DownloadWebPageThread(this@DownloadService, download))?.get()
            }

            download = dbHelper.getDownloadItemInQueue()
        }

        try {
            threadPool.shutdown()
            threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
        } catch (e: Exception) {
            Log.w(TAG, "Thread pool executor interrupted~")
        }

    }


    private fun isNetworkDown(): Boolean {
        if (!Utils.checkNetwork(this)) {
            return true
        }
        return false
    }

}

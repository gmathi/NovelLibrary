//package com.mgn.bingenovelreader.service
//
//import android.app.IntentService
//import android.content.Intent
//import android.util.Log
//import com.mgn.bingenovelreader.database.DBHelper
//import com.mgn.bingenovelreader.database.getAllDownloadQueue
//import com.mgn.bingenovelreader.database.updateAllDownloadQueueStatuses
//import com.mgn.bingenovelreader.event.EventType
//import com.mgn.bingenovelreader.event.NovelEvent
//import com.mgn.bingenovelreader.util.Constants
//import com.mgn.bingenovelreader.util.Utils
//import org.greenrobot.eventbus.EventBus
//import java.util.concurrent.Executors
//
//
//class DownloadService : IntentService(TAG) {
//
//    lateinit var dbHelper: DBHelper
//    val threadPoolExeutor = Executors.newFixedThreadPool(5)
//
//    //static components
//    companion object {
//        val TAG = "DownloadService"
//        val novelThreadMap = HashMap<Long, DownloadNovelThread>()
//
//        fun isDownloading(): Boolean = novelThreadMap.isNotEmpty()
//        fun isDownloading(novelId: Long): Boolean = novelThreadMap.containsKey(novelId)
//
//        fun stopDownload(novelId: Long) {
//            novelThreadMap[novelId]?.interrupt()
//            novelThreadMap.remove(novelId)
//        }
//
//        fun stopAllDownloads() {
//            novelThreadMap.keys.forEach { novelThreadMap[it]?.interrupt() }
//            novelThreadMap.clear()
//        }
//
//    }
//
//    override fun onHandleIntent(workIntent: Intent) {
//        dbHelper = DBHelper(applicationContext)
//        val novelId = workIntent.getLongExtra(Constants.NOVEL_ID, -1L)
//
//        android.os.Debug.waitForDebugger()
//
//        if (isNetworkDown()) return
//
//        if (novelId == -1L) {
//            downloadAllNovels()
//        } else {
//            downloadNovel(novelId)
//        }
//    }
//
//    private fun downloadAllNovels() {
//        val downloadQueueItems = dbHelper.getAllDownloadQueue().filter { it.status == Constants.STATUS_DOWNLOAD && !novelThreadMap.containsKey(it.novelId) }
//        downloadQueueItems.forEach {
//            novelThreadMap.put(it.novelId, DownloadNovelThread(this, it.novelId, dbHelper))
//           // if (dataCenter.queueNovelDownloads) {
//                threadPoolExeutor.submit(novelThreadMap[it.novelId]).get()
////            } else {
////                threadPoolExeutor.submit(novelThreadMap[it.novelId])
////            }
//        }
//
//    }
//
//    private fun downloadNovel(novelId: Long) {
//        if (!novelThreadMap.containsKey(novelId)) {
//            novelThreadMap.put(novelId, DownloadNovelThread(this, novelId, dbHelper))
//            //if (dataCenter.queueNovelDownloads) {
//                threadPoolExeutor.submit(novelThreadMap[novelId]).get()
////            } else {
////                threadPoolExeutor.submit(novelThreadMap[novelId])
////            }
//        }
//    }
//
//    private fun onNoNetwork() {
//        Log.e(TAG, "No Active Internet")
//        dbHelper.updateAllDownloadQueueStatuses(Constants.STATUS_STOPPED)
//        EventBus.getDefault().post(NovelEvent(EventType.UPDATE, -1L))
//    }
//
//    private fun isNetworkDown(): Boolean {
//        if (!Utils.checkNetwork(this)) {
//            onNoNetwork()
//            return true
//        }
//        return false
//    }
//
//}

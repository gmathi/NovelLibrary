package io.github.gmathi.novellibrary.service

import android.app.IntentService
import android.content.Intent
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.getNovel
import io.github.gmathi.novellibrary.database.updateNovel
import io.github.gmathi.novellibrary.util.Utils


class SyncNovelsService : IntentService(TAG) {

    lateinit var dbHelper: DBHelper

    //static components
    companion object {
        val TAG = "DownloadChapterService"
    }

    override fun onHandleIntent(workIntent: Intent) {
        //android.os.Debug.waitForDebugger()
        dbHelper = DBHelper(applicationContext)
        if (isNetworkDown()) return

        //Test Code
        val novel = dbHelper.getNovel(novelId = 0)
        novel?.chapterCount = 0
        dbHelper.updateNovel(novel!!)

    }

    private fun isNetworkDown(): Boolean {
        if (!Utils.checkNetwork(this)) {
            return true
        }
        return false
    }

}

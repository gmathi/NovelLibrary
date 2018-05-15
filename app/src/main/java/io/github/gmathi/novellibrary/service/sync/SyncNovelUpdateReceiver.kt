package io.github.gmathi.novellibrary.service.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.crashlytics.android.Crashlytics
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.updateTotalChapterCount
import io.github.gmathi.novellibrary.model.Novel


class SyncNovelUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        //Do Nothing

//        val bundle = intent.extras ?: return
//        val dbHelper = DBHelper.getInstance(context)
//        if (bundle.containsKey("novelsChapMap")) {
//            @Suppress("UNCHECKED_CAST")
//            try {
//                val novelsMap = bundle.getSerializable("novelsChapMap") as? HashMap<Novel, Int>
//                novelsMap?.keys?.forEach {
//                    dbHelper.updateTotalChapterCount(it.id, novelsMap[it]!!.toLong())
//                }
//            } catch (e: Exception) {
//                Crashlytics.log(intent.extras?.toString() ?: "No Extras")
//                Crashlytics.logException(e)
//            }
//        }
    }

}
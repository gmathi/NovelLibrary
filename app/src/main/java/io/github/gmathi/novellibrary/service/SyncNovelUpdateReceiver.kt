package io.github.gmathi.novellibrary.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.getNovel
import io.github.gmathi.novellibrary.database.updateNewChapterCount

class SyncNovelUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val bundle = intent.extras ?: return
        if (bundle.containsKey("novelsChapMap")) {
            @Suppress("UNCHECKED_CAST")
            val novelsMap = bundle.getSerializable("novelsChapMap") as HashMap<String, Long>
            novelsMap.keys.forEach {
                val dbHelper = DBHelper(context)
                val novel = dbHelper.getNovel(it)
                if (novel != null) {
                    dbHelper.updateNewChapterCount(novel.id, novelsMap[it]!!)
                }
            }
        }
    }

}
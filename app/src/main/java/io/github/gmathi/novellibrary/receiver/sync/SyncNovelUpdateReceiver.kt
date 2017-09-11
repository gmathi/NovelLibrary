package io.github.gmathi.novellibrary.receiver.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.gmathi.novellibrary.database.getNovel
import io.github.gmathi.novellibrary.database.updateNewChapterCount
import io.github.gmathi.novellibrary.dbHelper


class SyncNovelUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val bundle = intent.extras ?: return
        if (bundle.containsKey("novelsChapMap")) {
            @Suppress("UNCHECKED_CAST")
            val novelsMap = bundle.getSerializable("novelsChapMap") as HashMap<String, Long>
            try {
                novelsMap.keys.forEach {
                    val novel = dbHelper.getNovel(it)
                    if (novel != null) {
                        dbHelper.updateNewChapterCount(novel.id, novelsMap[it]!!)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}
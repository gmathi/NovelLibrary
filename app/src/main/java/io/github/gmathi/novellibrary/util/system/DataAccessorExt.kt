package io.github.gmathi.novellibrary.util.system

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.NovelSection
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.FAC
import io.github.gmathi.novellibrary.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Logs a simple Firebase event for a given novel.
 */
fun FirebaseAnalytics.logNovelEvent(eventName: String, novel: Novel) {
    logEvent(eventName) {
        param(FAC.Param.NOVEL_NAME, novel.name)
        param(FAC.Param.NOVEL_URL, novel.url)
    }
}

/**
 * Adds a new novel instance to the DB.
 * Adds novel to the NovelSync
 * Logs novel addition to Firebase.
 */
fun DataAccessor.addNewNovel(novel: Novel) {
    if (novel.id == -1L) {
        novel.id = dbHelper.insertNovel(novel)
        //NovelSync.getInstance(novel)?.applyAsync(lifecycleScope) { if (dataCenter.getSyncAddNovels(it.host)) it.addNovel(novel, null) }
        firebaseAnalytics.logNovelEvent(FAC.Event.ADD_NOVEL, novel)
    }
}

/**
 * Deletes the novel from the DB
 * Deletes all downloaded chapters of the novel
 * Logs novel deletion to Firebase.
 * Removes novel in NovelSync
 */
fun DataAccessor.deleteNovel(novel: Novel, context: Context) {
    Utils.deleteDownloadedChapters(context, novel)
    dbHelper.cleanupNovelData(novel)
    //NovelSync.getInstance(novel)?.applyAsync(lifecycleScope) { if (dataCenter.getSyncAddNovels(it.host)) it.removeNovel(novel, null) }
    firebaseAnalytics.logNovelEvent(FAC.Event.REMOVE_NOVEL, novel)
    Utils.broadcastNovelDelete(context, novel)
    novel.id = -1L
}
fun DataAccessor.deleteNovel(novel: Novel) = getContext()?.let { deleteNovel(novel, it) }

/**
 * Perform a hard reset on a novel.
 */
suspend fun DataAccessor.resetNovel(novel: Novel) {
    dbHelper.resetNovel(novel)
}

/**
 * Updates current novel bookmarked chapter.
 * Updates `Novel.currentChapterURL`
 * Marks the chapter as read
 * Sets the bookmark in the NovelSync
 */
fun DataAccessor.updateNovelBookmark(novel: Novel, webPage: WebPage) {
    if (novel.currentChapterUrl != webPage.url) {
        firebaseAnalytics.logNovelEvent(FAC.Event.READ_NOVEL, novel)
        novel.currentChapterUrl = webPage.url
        dbHelper.updateBookmarkCurrentWebPageUrl(novel.id, webPage.url)
        // NovelSync.getInstance(novel)?.applyAsync(lifecycleScope) { if (dataCenter.getSyncBookmarks(it.host)) it.setBookmark(novel, webPage) }
        markChapterRead(webPage, true)
    }
}

/**
 * Updates the WebPage settings instance with a read flag.
 */
fun DataAccessor.markChapterRead(webPage: WebPage, markRead: Boolean) {
    val webPageSettings = dbHelper.getWebPageSettings(webPage.url)
    if (webPageSettings != null) {
        dbHelper.updateWebPageSettingsReadStatus(webPageSettings, markRead)
    }
}

/**
 * Updates the novel last read timestamp.
 * Logs novel open event to Firebase.
 */
fun DataAccessor.updateNovelLastRead(novel: Novel) {
    novel.metadata[Constants.MetaDataKeys.LAST_READ_DATE] = Utils.getCurrentFormattedDate()
    dbHelper.updateNovelMetaData(novel)
    firebaseAnalytics.logNovelEvent(FAC.Event.OPEN_NOVEL, novel)
}

// TODO: Section actions
// TODO: Download actions
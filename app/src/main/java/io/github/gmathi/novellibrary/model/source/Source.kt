package io.github.gmathi.novellibrary.model.source

import android.graphics.drawable.Drawable
import io.github.gmathi.novellibrary.extension.ExtensionManager
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.source.NovelUpdatesSource
import io.github.gmathi.novellibrary.util.lang.awaitSingle
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * A basic interface for creating a source. It could be an online source, a local source, etc...
 */
interface Source {

    /**
     * Id for the source. Must be unique.
     */
    val id: Long

    /**
     * Name of the source.
     */
    val name: String

    /**
     * Returns an observable with the updated details for a novel.
     *
     * @param novel the novel to update.
     */
    fun fetchNovelDetails(novel: Novel): Observable<Novel>

    /**
     * [1.x API] Get all the available chapters for a novel.
     */
    suspend fun getNovelDetails(novel: Novel): Novel {
        val downloadedNovel = fetchNovelDetails(novel).awaitSingle()
        if (downloadedNovel.chaptersCount == 0L) {
            downloadedNovel.chaptersCount =
                if (this is NovelUpdatesSource)
                    getUnsortedChapterList(novel).count().toLong()
                else
                    getChapterList(novel).count().toLong()

        }
        return novel
    }

    /**
     * Returns an observable with all the available chapters for a novel.
     *
     * @param novel the novel to update.
     */
    fun fetchChapterList(novel: Novel): Observable<List<WebPage>>

    /**
     * [1.x API] Get all the available chapters for a novel.
     */
    suspend fun getChapterList(novel: Novel): List<WebPage> {
        return fetchChapterList(novel).awaitSingle()
    }

    fun Source.icon(): Drawable? = Injekt.get<ExtensionManager>().getAppIconForSource(this)
    fun Source.getPreferenceKey(): String = "source_$id"

}
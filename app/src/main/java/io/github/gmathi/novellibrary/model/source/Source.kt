package io.github.gmathi.novellibrary.model.source

import android.graphics.drawable.Drawable
import io.github.gmathi.novellibrary.extension.ExtensionManager
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.source.NovelUpdatesSource
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
     * Get the updated details for a novel.
     *
     * @param novel the novel to update.
     */
    suspend fun getNovelDetails(novel: Novel): Novel {
        val downloadedNovel = fetchNovelDetails(novel)
        if (downloadedNovel.chaptersCount == 0L) {
            downloadedNovel.chaptersCount =
                if (this is NovelUpdatesSource)
                    getUnsortedChapterList(novel).count().toLong()
                else
                    getChapterList(novel).count().toLong()

        }
        return downloadedNovel
    }

    /**
     * Internal method to fetch novel details - should be implemented by subclasses
     */
    suspend fun fetchNovelDetails(novel: Novel): Novel

    /**
     * Get all the available chapters for a novel.
     */
    suspend fun getChapterList(novel: Novel): List<WebPage>

}

fun Source.icon(): Drawable? = Injekt.get<ExtensionManager>().getAppIconForSource(this)

fun Source.getPreferenceKey(): String = "source_$id"
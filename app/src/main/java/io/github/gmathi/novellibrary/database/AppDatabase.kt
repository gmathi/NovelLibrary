package io.github.gmathi.novellibrary.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.gmathi.novellibrary.database.dao.*
import io.github.gmathi.novellibrary.model.database.*
import io.github.gmathi.novellibrary.util.lang.launchIO

@TypeConverters(value = [Converters::class])
@Database(entities = [Download::class, Genre::class, Novel::class, NovelGenre::class, NovelSection::class, TranslatorSource::class, WebPage::class, WebPageSettings::class],
    version = DBKeys.DATABASE_VERSION)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun genreDao(): GenreDao
    abstract fun novelDao(): NovelDao
    abstract fun novelGenreDao(): NovelGenreDao
    abstract fun novelSectionDao(): NovelSectionDao
    abstract fun translatorSourceDao(): TranslatorSourceDao
    abstract fun webPageDao(): WebPageDao
    abstract fun webPageSettingsDao(): WebPageSettingsDao

    /**
     * Helper for inserting novels. Sets novel.id to 0 so the ID can be automatically generated
     * @param novel Novel to insert into database
     */
    fun insertNovel(novel: Novel): Long {
        assert(novel.id == -1L || novel.id == 0L)
        novel.id = 0
        return novelDao().insert(novel)
    }

    /**
     * Populate database with default required entries
     */
    suspend fun insertDefaults() {
        translatorSourceDao().insertOrIgnore(TranslatorSource(-1L, "All"))
    }
}
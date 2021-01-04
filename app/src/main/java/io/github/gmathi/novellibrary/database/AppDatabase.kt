package io.github.gmathi.novellibrary.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.gmathi.novellibrary.database.dao.*
import io.github.gmathi.novellibrary.model.database.*

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
    
    fun insertNovel(novel: Novel): Long {
        novel.id = 0
        return novelDao().insert(novel)
    }
}
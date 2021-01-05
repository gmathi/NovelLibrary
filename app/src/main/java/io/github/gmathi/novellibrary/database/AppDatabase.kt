package io.github.gmathi.novellibrary.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.gmathi.novellibrary.NovelLibraryApplication
import io.github.gmathi.novellibrary.database.dao.*
import io.github.gmathi.novellibrary.model.database.*
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.network.getNovelDetails
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.lang.launchIO

@TypeConverters(value = [Converters::class])
@Database(entities = [Download::class, Genre::class, Novel::class, NovelGenre::class, NovelSection::class, TranslatorSource::class, WebPage::class, WebPageSettings::class, LargePreference::class],
    version = DBKeys.DATABASE_VERSION)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        fun createInstance(context: Context = NovelLibraryApplication.context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DBKeys.DATABASE_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
            .allowMainThreadQueries()
            .build()
        }
    }
    abstract fun downloadDao(): DownloadDao
    abstract fun genreDao(): GenreDao
    abstract fun novelDao(): NovelDao
    abstract fun novelGenreDao(): NovelGenreDao
    abstract fun novelSectionDao(): NovelSectionDao
    abstract fun translatorSourceDao(): TranslatorSourceDao
    abstract fun webPageDao(): WebPageDao
    abstract fun webPageSettingsDao(): WebPageSettingsDao
    abstract fun largePreferenceDao(): LargePreferenceDao

    /**
     * Helper for inserting novels. Sets novel.id to 0 so the ID can be automatically generated
     * @param novel Novel to insert into database
     */
    fun insertNovel(novel: Novel): Long {
        if(novel.id == -1L)
            novel.id = 0

        val id = novelDao().insert(novel)
        novel.genres?.forEach {
            val genreId = genreDao().findOneByName(it)?.id ?: genreDao().insert(Genre(0, it))
            novelGenreDao().insertOrIgnore(NovelGenre(id, genreId))
        }
        return id
    }
    
    fun cleanupNovel(novel: Novel) {
        novelDao().delete(novel) // Deletes web_page, web_page_settings, novel_genre
        // because cascading foreign key property
        downloadDao().deleteByNovelName(novel.name)
    }

    fun resetNovel(novel: Novel) {
        cleanupNovel(novel)
        
        val newNovel = NovelApi.getNovelDetails(novel.url)
        newNovel?.novelSectionId = novel.novelSectionId
        newNovel?.orderId = novel.orderId
        if (newNovel != null) insertNovel(novel)
    }

    /**
     * Populate database with default required entries
     */
    fun insertDefaults() {
        translatorSourceDao().insertOrIgnore(TranslatorSource(-1L, "All"))
        novelSectionDao().insertOrIgnore(NovelSection(-1L, "Currently Reading"))
    }
    
    fun updateWebPageSettingsReadStatus(webPageSettings: WebPageSettings) {
        if (webPageSettings.isRead == 0) {
            webPageSettings.metadata.remove(Constants.MetaDataKeys.SCROLL_POSITION)
        }

        webPageSettingsDao().update(webPageSettings)
    }
}
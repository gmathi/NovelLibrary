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
     * Populate database with default required entries
     */
    fun insertDefaults() {
        translatorSourceDao().insertOrIgnore(TranslatorSource(-1L, "All"))
        novelSectionDao().insertOrIgnore(NovelSection(-1L, "Currently Reading"))
    }
}
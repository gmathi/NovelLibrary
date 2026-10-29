package io.github.gmathi.novellibrary.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.gmathi.novellibrary.database.dao.GenreDao
import io.github.gmathi.novellibrary.database.dao.LargePreferenceDao
import io.github.gmathi.novellibrary.database.dao.NovelDao
import io.github.gmathi.novellibrary.database.dao.NovelGenreDao
import io.github.gmathi.novellibrary.database.dao.NovelSectionDao
import io.github.gmathi.novellibrary.database.dao.NovelTagDao
import io.github.gmathi.novellibrary.database.dao.WebPageDao
import io.github.gmathi.novellibrary.database.dao.WebPageSettingsDao
import io.github.gmathi.novellibrary.model.database.Download
import io.github.gmathi.novellibrary.model.database.Genre
import io.github.gmathi.novellibrary.model.database.LargePreference
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.NovelGenre
import io.github.gmathi.novellibrary.model.database.NovelSection
import io.github.gmathi.novellibrary.model.database.NovelTag
import io.github.gmathi.novellibrary.model.database.NovelTagCrossRef
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.database.WebPageSettings

@Database(
    entities = [
        Novel::class,
        WebPage::class,
        WebPageSettings::class,
        Genre::class,
        NovelGenre::class,
        Download::class,
        NovelSection::class,
        LargePreference::class,
        NovelTag::class,
        NovelTagCrossRef::class
    ],
    version = AppDatabase.DB_VERSION,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun novelDao(): NovelDao
    abstract fun webPageDao(): WebPageDao
    abstract fun webPageSettingsDao(): WebPageSettingsDao
    abstract fun genreDao(): GenreDao
    abstract fun novelGenreDao(): NovelGenreDao
    abstract fun novelSectionDao(): NovelSectionDao
    abstract fun novelTagDao(): NovelTagDao
    abstract fun largePreferenceDao(): LargePreferenceDao

    companion object {
        const val DB_VERSION = 12

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DBKeys.DATABASE_NAME)
                .addMigrations(MIGRATION_10_11, MIGRATION_11_12)
                // TODO: remove allowMainThreadQueries() once callers are migrated to
                //  suspend DAO methods (Phase 2 — Hilt migration).
                .allowMainThreadQueries()
                .build()
        }

        /**
         * Migration from version 10 to 11.
         *
         * Recreates tables that had SQLite FK constraints so that the Room-managed schema
         * (which has no FK annotations) validates correctly. Also:
         *   - Drops the legacy `current_web_page_id` column from `novel`
         *   - Drops the deprecated `source_id` column from `web_page`
         *   - Adds a composite PRIMARY KEY to `novel_genre`
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {

                // ── novel ──────────────────────────────────────────────────────────────
                // Recreate without the broken FK (current_web_page_id → web_page.id)
                // and drop the legacy current_web_page_id column.
                db.execSQL("""
                    CREATE TABLE novel_new (
                        id INTEGER PRIMARY KEY,
                        name TEXT,
                        url TEXT,
                        image_url TEXT,
                        rating TEXT,
                        short_description TEXT,
                        long_description TEXT,
                        external_novel_id TEXT,
                        image_file_path TEXT,
                        metadata TEXT,
                        current_web_page_url TEXT,
                        order_id INTEGER,
                        source_id INTEGER,
                        chapter_count INTEGER,
                        new_chapter_count INTEGER,
                        novel_section_id INTEGER
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO novel_new
                    SELECT id, name, url, image_url, rating, short_description, long_description,
                           external_novel_id, image_file_path, metadata, current_web_page_url,
                           order_id, source_id, chapter_count, new_chapter_count, novel_section_id
                    FROM novel
                """.trimIndent())
                db.execSQL("DROP TABLE novel")
                db.execSQL("ALTER TABLE novel_new RENAME TO novel")

                // ── web_page ───────────────────────────────────────────────────────────
                // Recreate without FKs; also drops the deprecated source_id column.
                db.execSQL("DROP INDEX IF EXISTS ${DBKeys.INDEX_WEB_PAGE}")
                db.execSQL("""
                    CREATE TABLE web_page_new (
                        url TEXT PRIMARY KEY,
                        chapter TEXT,
                        novel_id INTEGER,
                        translator_source_name TEXT,
                        order_id INTEGER
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO web_page_new
                    SELECT url, chapter, novel_id, translator_source_name, order_id
                    FROM web_page
                """.trimIndent())
                db.execSQL("DROP TABLE web_page")
                db.execSQL("ALTER TABLE web_page_new RENAME TO web_page")
                db.execSQL("CREATE INDEX ${DBKeys.INDEX_WEB_PAGE} ON web_page(url, novel_id)")

                // ── web_page_settings ──────────────────────────────────────────────────
                // Recreate without FK.
                db.execSQL("DROP INDEX IF EXISTS ${DBKeys.INDEX_WEB_PAGE_SETTINGS}")
                db.execSQL("""
                    CREATE TABLE web_page_settings_new (
                        url TEXT PRIMARY KEY,
                        novel_id INTEGER,
                        redirect_url TEXT,
                        title TEXT,
                        metadata TEXT,
                        file_path TEXT,
                        is_read INTEGER
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO web_page_settings_new
                    SELECT url, novel_id, redirect_url, title, metadata, file_path, is_read
                    FROM web_page_settings
                """.trimIndent())
                db.execSQL("DROP TABLE web_page_settings")
                db.execSQL("ALTER TABLE web_page_settings_new RENAME TO web_page_settings")
                db.execSQL("CREATE INDEX ${DBKeys.INDEX_WEB_PAGE_SETTINGS} ON web_page_settings(url, novel_id)")

                // ── novel_genre ────────────────────────────────────────────────────────
                // Recreate without FKs and with a composite PRIMARY KEY.
                db.execSQL("""
                    CREATE TABLE novel_genre_new (
                        novel_id INTEGER NOT NULL,
                        genre_id INTEGER NOT NULL,
                        PRIMARY KEY(novel_id, genre_id)
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT OR IGNORE INTO novel_genre_new SELECT novel_id, genre_id FROM novel_genre
                """.trimIndent())
                db.execSQL("DROP TABLE novel_genre")
                db.execSQL("ALTER TABLE novel_genre_new RENAME TO novel_genre")
            }
        }

        /**
         * Migration from version 11 to 12 — the tag model.
         *
         * Creates `novel_tag` and `novel_tag_cross_ref` with the exact schema Room expects for
         * [NovelTag] / [NovelTagCrossRef] (so Room's open-time validation passes), then seeds one
         * tag per existing novel-section name and links each novel to its section's tag.
         *
         * The seeding is idempotent and guarded on the tag table being empty: whichever layer opens
         * the DB first (the legacy [DBHelper] or Room) creates and seeds the tables with `IF NOT
         * EXISTS`, and the other sees them already populated and skips the seed.
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(DBKeys.CREATE_TABLE_NOVEL_TAG)
                db.execSQL(DBKeys.CREATE_TABLE_NOVEL_TAG_CROSS_REF)
                db.execSQL(DBKeys.CREATE_INDEX_NOVEL_TAG_NAME)
                db.execSQL(DBKeys.CREATE_INDEX_NOVEL_TAG_CROSS_REF_TAG)

                // Skip seeding if tags already exist (legacy DBHelper.onUpgrade got here first).
                val existing = db.query("SELECT COUNT(*) FROM novel_tag")
                var tagCount = 0
                if (existing.moveToFirst()) tagCount = existing.getInt(0)
                existing.close()
                if (tagCount > 0) return

                val defaultName = "Currently Reading"
                val sectionNames = HashMap<Long, String>()
                sectionNames[-1L] = defaultName
                val sc = db.query("SELECT id, name FROM novel_section")
                while (sc.moveToNext()) {
                    val name = sc.getString(1)
                    if (!name.isNullOrBlank()) sectionNames[sc.getLong(0)] = name
                }
                sc.close()

                val used = HashSet<Long>()
                val uc = db.query("SELECT DISTINCT novel_section_id FROM novel")
                while (uc.moveToNext()) used.add(uc.getLong(0))
                uc.close()

                val sectionToTag = HashMap<Long, Long>()
                var order = 0L
                for (sectionId in used) {
                    val name = sectionNames[sectionId] ?: continue
                    db.execSQL(
                        "INSERT OR IGNORE INTO novel_tag (name, color, order_id) VALUES (?, NULL, ?)",
                        arrayOf<Any>(name, order++)
                    )
                    val tc = db.query("SELECT id FROM novel_tag WHERE name = ?", arrayOf<Any>(name))
                    if (tc.moveToFirst()) sectionToTag[sectionId] = tc.getLong(0)
                    tc.close()
                }

                val nc = db.query("SELECT id, novel_section_id FROM novel")
                while (nc.moveToNext()) {
                    val novelId = nc.getLong(0)
                    val sectionId = nc.getLong(1)
                    val tagId = sectionToTag[sectionId] ?: sectionToTag[-1L] ?: continue
                    db.execSQL(
                        "INSERT OR IGNORE INTO novel_tag_cross_ref (novel_id, tag_id) VALUES (?, ?)",
                        arrayOf<Any>(novelId, tagId)
                    )
                }
                nc.close()
            }
        }
    }
}

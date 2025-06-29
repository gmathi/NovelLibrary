package io.github.gmathi.novellibrary.database


object DBKeys {

    internal const val INITIAL_VERSION = 1
    internal const val VER_NOVEL_ORDER_ID = 2
    internal const val VER_WEB_PAGE_ORDER_ID = 3
    internal const val VER_NOVEL_SYNC = 4
    internal const val VER_CHAPTER_SOURCE = 5
    internal const val VER_DOWNLOADS = 6
    internal const val VER_NEW_RELEASES = 7
    internal const val VER_LARGE_PREFERENCE = 8
    internal const val VER_WEB_PAGE_SETTINGS = 9
    internal const val VER_SOURCES_REFACTOR = 10

    internal const val DATABASE_VERSION = VER_SOURCES_REFACTOR

    internal const val DATABASE_NAME = "bnr_db"

    // Table names
    internal const val TABLE_NOVEL = "novel"
    internal const val TABLE_WEB_PAGE = "web_page"
    internal const val TABLE_WEB_PAGE_SETTINGS = "web_page_settings"
    internal const val TABLE_GENRE = "genre"
    internal const val TABLE_NOVEL_GENRE = "novel_genre"

    @Deprecated("Use `downloads` table instead")
    internal const val TABLE_DOWNLOAD_QUEUE = "download_queue"
    internal const val TABLE_DOWNLOAD = "download"

    @Deprecated("Not used staring from db version 10")
    internal const val TABLE_SOURCE = "source"

    internal const val TABLE_NOVEL_SECTION = "novel_section"
    internal const val TABLE_LARGE_PREFERENCE = "large_preference"

    //Index names
    internal const val INDEX_WEB_PAGE = "web_page_url_novel_id_index"
    internal const val INDEX_WEB_PAGE_SETTINGS = "web_page_settings_url_index"
    internal const val INDEX_NOVEL_SECTION_ORDER = "novel_section_order_index"
    internal const val INDEX_NOVEL_URL = "novel_url_index"
    internal const val INDEX_NOVEL_SECTION_ID = "novel_section_id_index"
    internal const val INDEX_WEB_PAGE_NOVEL_ORDER = "web_page_novel_order_index"
    internal const val INDEX_WEB_PAGE_TRANSLATOR = "web_page_translator_index"
    internal const val INDEX_DOWNLOAD_NOVEL = "download_novel_index"
    internal const val INDEX_DOWNLOAD_STATUS = "download_status_index"
    internal const val INDEX_NOVEL_GENRE = "novel_genre_index"

    // Common column names
    internal const val KEY_ID = "id"
    internal const val KEY_NAME = "name"
    internal const val KEY_URL = "url"
    internal const val KEY_METADATA = "metadata"
    internal const val KEY_NOVEL_ID = "novel_id"
    internal const val KEY_ORDER_ID = "order_id"

    // Table novel columns
    internal const val KEY_IMAGE_URL = "image_url"
    internal const val KEY_RATING = "rating"
    internal const val KEY_SHORT_DESCRIPTION = "short_description"
    internal const val KEY_LONG_DESCRIPTION = "long_description"
    internal const val KEY_EXTERNAL_NOVEL_ID = "external_novel_id"
    internal const val KEY_IMAGE_FILE_PATH = "image_file_path"
    internal const val KEY_CURRENT_WEB_PAGE_ID = "current_web_page_id"
    internal const val KEY_CURRENT_WEB_PAGE_URL = "current_web_page_url"
    internal const val KEY_NEW_RELEASES_COUNT = "chapter_count"
    internal const val KEY_CHAPTERS_COUNT = "new_chapter_count"
    internal const val KEY_NOVEL_SECTION_ID = "novel_section_id"
    internal const val KEY_VALUE = "value"


    // Table web_page columns
    internal const val KEY_TITLE = "title"
    internal const val KEY_CHAPTER = "chapter"
    internal const val KEY_FILE_PATH = "file_path"
    internal const val KEY_REDIRECT_URL = "redirect_url"
    internal const val KEY_IS_READ = "is_read"
    internal const val KEY_SOURCE_ID = "source_id"
    internal const val KEY_TRANSLATOR_SOURCE_NAME = "translator_source_name"

    // Table novel_genre columns
    internal const val KEY_GENRE_ID = "genre_id"

    //Table download_queue columns
    internal const val KEY_STATUS = "status"
    internal const val KEY_TOTAL_CHAPTERS = "total_chapters"
    internal const val KEY_CURRENT_CHAPTER = "current_chapter"
    internal const val KEY_CHAPTER_URLS_CACHED = "chapter_urls_cached"

    //Table download_queue_columns
    internal const val KEY_WEB_PAGE_ID = "web_page_id"
    internal const val KEY_WEB_PAGE_URL = "web_page_url"

    //Table source


    // novel table create statement
    internal const val CREATE_TABLE_NOVEL = (
            "CREATE TABLE " + TABLE_NOVEL + " ("
                    + KEY_ID + " INTEGER PRIMARY KEY, "
                    + KEY_NAME + " TEXT, "
                    + KEY_URL + " TEXT, "
                    + KEY_IMAGE_URL + " TEXT, "
                    + KEY_RATING + " TEXT, "
                    + KEY_SHORT_DESCRIPTION + " TEXT, "
                    + KEY_LONG_DESCRIPTION + " TEXT, "
                    + KEY_EXTERNAL_NOVEL_ID + " TEXT, "
                    + KEY_IMAGE_FILE_PATH + " TEXT, "
                    + KEY_METADATA + " TEXT, "
                    + KEY_CURRENT_WEB_PAGE_ID + " INTEGER, "
                    + KEY_CURRENT_WEB_PAGE_URL + " TEXT, "
                    + KEY_ORDER_ID + " INTEGER, "
                    + KEY_SOURCE_ID + " INTEGER, "
                    + KEY_NEW_RELEASES_COUNT + " INTEGER, "
                    + KEY_CHAPTERS_COUNT + " INTEGER, "
                    + KEY_NOVEL_SECTION_ID + " INTEGER, "
                    + "FOREIGN KEY (" + KEY_CURRENT_WEB_PAGE_ID + ") REFERENCES " + TABLE_WEB_PAGE + "(" + KEY_ID + ")"
                    + ")")

    // web_page table create statement
    internal const val CREATE_TABLE_WEB_PAGE = (
            "CREATE TABLE " + TABLE_WEB_PAGE + " ("
                    + KEY_URL + " TEXT PRIMARY KEY, "
                    + KEY_CHAPTER + " TEXT, "
                    + KEY_NOVEL_ID + " INTEGER, "
                    + KEY_SOURCE_ID + " INTEGER, "
                    + KEY_TRANSLATOR_SOURCE_NAME + " TEXT, "
                    + KEY_ORDER_ID + " INTEGER, "
                    + "FOREIGN KEY (" + KEY_NOVEL_ID + ") REFERENCES " + TABLE_NOVEL + "(" + KEY_ID + ")"
                    + "FOREIGN KEY (" + KEY_SOURCE_ID + ") REFERENCES " + TABLE_SOURCE + "(" + KEY_ID + ")"
                    + ")")

    // web_page_settings table create statement
    internal const val CREATE_TABLE_WEB_PAGE_SETTINGS = (
            "CREATE TABLE " + TABLE_WEB_PAGE_SETTINGS + " ("
                    + KEY_URL + " TEXT PRIMARY KEY, "
                    + KEY_NOVEL_ID + " INTEGER, "
                    + KEY_REDIRECT_URL + " TEXT, "
                    + KEY_TITLE + " TEXT, "
                    + KEY_METADATA + " TEXT, "
                    + KEY_FILE_PATH + " TEXT, "
                    + KEY_IS_READ + " INTEGER, "
                    + "FOREIGN KEY (" + KEY_NOVEL_ID + ") REFERENCES " + TABLE_NOVEL + "(" + KEY_ID + ")"
                    + ")")

    // genre table create statement
    internal const val CREATE_TABLE_GENRE = (
            "CREATE TABLE " + TABLE_GENRE + " ("
                    + KEY_ID + " INTEGER PRIMARY KEY, "
                    + KEY_NAME + " TEXT"
                    + ")")

    // novel_genre table create statement
    internal const val CREATE_TABLE_NOVEL_GENRE = (
            "CREATE TABLE " + TABLE_NOVEL_GENRE + " ("
                    + KEY_NOVEL_ID + " INTEGER, "
                    + KEY_GENRE_ID + " INTEGER, "
                    + "FOREIGN KEY (" + KEY_NOVEL_ID + ") REFERENCES " + TABLE_NOVEL + "(" + KEY_ID + "), "
                    + "FOREIGN KEY (" + KEY_GENRE_ID + ") REFERENCES " + TABLE_GENRE + "(" + KEY_ID + ")"
                    + ")")

    // source table create statement
    internal const val CREATE_TABLE_SOURCE = (
            "CREATE TABLE " + TABLE_SOURCE + " ("
                    + KEY_ID + " INTEGER PRIMARY KEY, "
                    + KEY_NAME + " TEXT"
                    + ")")


    // downloads table create statement
    internal const val CREATE_TABLE_DOWNLOAD = (
            "CREATE TABLE " + TABLE_DOWNLOAD + " ("
                    + KEY_NAME + " TEXT, "
                    + KEY_NOVEL_ID + " INTEGER, "
                    + KEY_WEB_PAGE_URL + " TEXT PRIMARY KEY, "
                    + KEY_CHAPTER + " TEXT, "
                    + KEY_STATUS + " INTEGER, "
                    + KEY_ORDER_ID + " INTEGER, "
                    + KEY_METADATA + " TEXT"
                    + ")")

    // novel_section table create statement
    internal const val CREATE_TABLE_NOVEL_SECTION = (
            "CREATE TABLE " + TABLE_NOVEL_SECTION + " ("
                    + KEY_ID + " INTEGER PRIMARY KEY, "
                    + KEY_NAME + " TEXT, "
                    + KEY_ORDER_ID + " INTEGER"
                    + ")")

    // novel_section table create statement
    internal const val CREATE_TABLE_LARGE_PREFERENCE = (
            "CREATE TABLE " + TABLE_LARGE_PREFERENCE + " ("
                    + KEY_NAME + " TEXT PRIMARY KEY, "
                    + KEY_VALUE + " TEXT"
                    + ")")


    // web_page index statement
    internal const val CREATE_INDEX_WEB_PAGE = (
            "CREATE INDEX $INDEX_WEB_PAGE ON $TABLE_WEB_PAGE($KEY_URL, $KEY_NOVEL_ID)"
            )

    // web_page_settings index statement
    internal const val CREATE_INDEX_WEB_PAGE_SETTINGS = (
            "CREATE INDEX $INDEX_WEB_PAGE_SETTINGS ON $TABLE_WEB_PAGE_SETTINGS($KEY_URL, $KEY_NOVEL_ID)"
            )

    // Additional indexes for better performance
    internal const val CREATE_INDEX_NOVEL_SECTION_ORDER = (
            "CREATE INDEX $INDEX_NOVEL_SECTION_ORDER ON $TABLE_NOVEL($KEY_NOVEL_SECTION_ID, $KEY_ORDER_ID)"
            )

    internal const val CREATE_INDEX_NOVEL_URL = (
            "CREATE INDEX $INDEX_NOVEL_URL ON $TABLE_NOVEL($KEY_URL)"
            )

    internal const val CREATE_INDEX_NOVEL_SECTION_ID = (
            "CREATE INDEX $INDEX_NOVEL_SECTION_ID ON $TABLE_NOVEL($KEY_NOVEL_SECTION_ID)"
            )

    internal const val CREATE_INDEX_WEB_PAGE_NOVEL_ORDER = (
            "CREATE INDEX $INDEX_WEB_PAGE_NOVEL_ORDER ON $TABLE_WEB_PAGE($KEY_NOVEL_ID, $KEY_ORDER_ID)"
            )

    internal const val CREATE_INDEX_WEB_PAGE_TRANSLATOR = (
            "CREATE INDEX $INDEX_WEB_PAGE_TRANSLATOR ON $TABLE_WEB_PAGE($KEY_NOVEL_ID, $KEY_TRANSLATOR_SOURCE_NAME)"
            )

    internal const val CREATE_INDEX_DOWNLOAD_NOVEL = (
            "CREATE INDEX $INDEX_DOWNLOAD_NOVEL ON $TABLE_DOWNLOAD($KEY_NOVEL_ID)"
            )

    internal const val CREATE_INDEX_DOWNLOAD_STATUS = (
            "CREATE INDEX $INDEX_DOWNLOAD_STATUS ON $TABLE_DOWNLOAD($KEY_STATUS)"
            )

    internal const val CREATE_INDEX_NOVEL_GENRE = (
            "CREATE INDEX $INDEX_NOVEL_GENRE ON $TABLE_NOVEL_GENRE($KEY_NOVEL_ID, $KEY_GENRE_ID)"
            )

}

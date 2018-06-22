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


    internal const val DATABASE_VERSION = VER_WEB_PAGE_SETTINGS


    internal const val DATABASE_NAME = "bnr_db"

    // Table names
    internal const val TABLE_NOVEL = "novel"
    internal const val TABLE_WEB_PAGE = "web_page"
    internal const val TABLE_WEB_PAGE_SETTINGS = "web_page_settings"
    internal const val TABLE_GENRE = "genre"
    internal const val TABLE_NOVEL_GENRE = "novel_genre"
    internal const val TABLE_DOWNLOAD_QUEUE = "download_queue"
    internal const val TABLE_DOWNLOAD = "download"
    internal const val TABLE_SOURCE = "source"
    internal const val TABLE_NOVEL_SECTION = "novel_section"
    internal const val TABLE_LARGE_PREFERENCE = "large_preference"

    //Index names
    internal const val INDEX_WEB_PAGE = "web_page_url_novel_id_index"
    internal const val INDEX_WEB_PAGE_SETTINGS = "web_page_settings_url_index"

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
            + KEY_IMAGE_FILE_PATH + " TEXT, "
            + KEY_METADATA + " TEXT, "
            + KEY_CURRENT_WEB_PAGE_ID + " INTEGER, "
            + KEY_ORDER_ID + " INTEGER, "
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

    // download queue table create statement
//    internal val CREATE_TABLE_DOWNLOAD_QUEUE = (
//        "CREATE TABLE " + TABLE_DOWNLOAD_QUEUE + " ("
//            + KEY_NOVEL_ID + " INTEGER PRIMARY KEY, "
//            + KEY_STATUS + " INTEGER, "
//            + KEY_METADATA + " TEXT"
//            + ")")

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

    //    // sync table create statement
    //    static final String CREATE_TABLE_SYNC =
    //            "CREATE TABLE " + TABLE_DOWNLOAD_QUEUE + " ("
    //                    + KEY_ID + " INTEGER PRIMARY KEY, "
    //                    + KEY_NAME + " TEXT UNIQUE ON CONFLICT IGNORE, "
    //                    + KEY_NEW_RELEASES_COUNT + " INTEGER,"
    //                    + KEY_METADATA + " TEXT"
    //                    + ")";

    // web_page index statement
    internal const val CREATE_INDEX_WEB_PAGE = (
            "CREATE INDEX $INDEX_WEB_PAGE ON $TABLE_WEB_PAGE($KEY_URL, $KEY_NOVEL_ID)"
            )

    // web_page_settings index statement
    internal const val CREATE_INDEX_WEB_PAGE_SETTINGS = (
            "CREATE INDEX $INDEX_WEB_PAGE_SETTINGS ON $TABLE_WEB_PAGE_SETTINGS($KEY_URL, $KEY_NOVEL_ID)"
            )


}

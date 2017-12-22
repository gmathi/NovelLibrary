package io.github.gmathi.novellibrary.database


object DBKeys {

    internal val INITIAL_VERSION = 1
    internal val VER_NOVEL_ORDER_ID = 2
    internal val VER_WEB_PAGE_ORDER_ID = 3
    internal val VER_NOVEL_SYNC = 4
    internal val VER_CHAPTER_SOURCE = 5
    internal val VER_DOWNLOADS = 6

    internal val DATABASE_VERSION = VER_DOWNLOADS


    internal val DATABASE_NAME = "bnr_db"

    // Table names
    internal val TABLE_NOVEL = "novel"
    internal val TABLE_WEB_PAGE = "web_page"
    internal val TABLE_GENRE = "genre"
    internal val TABLE_NOVEL_GENRE = "novel_genre"
    internal val TABLE_DOWNLOAD_QUEUE = "download_queue"
    internal val TABLE_DOWNLOAD = "download"
    internal val TABLE_SOURCE = "source"

    // Common column names
    internal val KEY_ID = "id"
    internal val KEY_NAME = "name"
    internal val KEY_URL = "url"
    internal val KEY_METADATA = "metadata"
    internal val KEY_NOVEL_ID = "novel_id"
    internal val KEY_ORDER_ID = "order_id"

    // Table novel columns
    internal val KEY_IMAGE_URL = "image_url"
    internal val KEY_RATING = "rating"
    internal val KEY_SHORT_DESCRIPTION = "short_description"
    internal val KEY_LONG_DESCRIPTION = "long_description"
    internal val KEY_IMAGE_FILE_PATH = "image_file_path"
    internal val KEY_CURRENT_WEB_PAGE_ID = "current_web_page_id"
    internal val KEY_CHAPTER_COUNT = "chapter_count"
    internal val KEY_NEW_CHAPTER_COUNT = "new_chapter_count"


    // Table web_page columns
    internal val KEY_TITLE = "title"
    internal val KEY_CHAPTER = "chapter"
    internal val KEY_FILE_PATH = "file_path"
    internal val KEY_REDIRECT_URL = "redirect_url"
    internal val KEY_IS_READ = "is_read"
    internal val KEY_SOURCE_ID = "source_id"

    // Table novel_genre columns
    internal val KEY_GENRE_ID = "genre_id"

    //Table download_queue columns
    internal val KEY_STATUS = "status"
    internal val KEY_TOTAL_CHAPTERS = "total_chapters"
    internal val KEY_CURRENT_CHAPTER = "current_chapter"
    internal val KEY_CHAPTER_URLS_CACHED = "chapter_urls_cached"

    //Table download_queue_columns
    internal val KEY_WEB_PAGE_ID = "web_page_id"

    //Table source


    // novel table create statement
    internal val CREATE_TABLE_NOVEL = (
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
            + KEY_CHAPTER_COUNT + " INTEGER, "
            + KEY_NEW_CHAPTER_COUNT + " INTEGER, "
            + "FOREIGN KEY (" + KEY_CURRENT_WEB_PAGE_ID + ") REFERENCES " + TABLE_WEB_PAGE + "(" + KEY_ID + ")"
            + ")")

    // web_page table create statement
    internal val CREATE_TABLE_WEB_PAGE = (
        "CREATE TABLE " + TABLE_WEB_PAGE + " ("
            + KEY_ID + " INTEGER PRIMARY KEY, "
            + KEY_URL + " TEXT, "
            + KEY_REDIRECT_URL + " TEXT, "
            + KEY_CHAPTER + " TEXT, "
            + KEY_TITLE + " TEXT, "
            + KEY_METADATA + " TEXT, "
            + KEY_FILE_PATH + " TEXT, "
            + KEY_IS_READ + " INTEGER, "
            + KEY_NOVEL_ID + " INTEGER, "
            + KEY_SOURCE_ID + " INTEGER, "
            + KEY_ORDER_ID + " INTEGER, "
            + "FOREIGN KEY (" + KEY_NOVEL_ID + ") REFERENCES " + TABLE_NOVEL + "(" + KEY_ID + ")"
            + "FOREIGN KEY (" + KEY_SOURCE_ID + ") REFERENCES " + TABLE_SOURCE + "(" + KEY_ID + ")"
            + ")")

    // genre table create statement
    internal val CREATE_TABLE_GENRE = (
        "CREATE TABLE " + TABLE_GENRE + " ("
            + KEY_ID + " INTEGER PRIMARY KEY, "
            + KEY_NAME + " TEXT"
            + ")")

    // novel_genre table create statement
    internal val CREATE_TABLE_NOVEL_GENRE = (
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
    internal val CREATE_TABLE_SOURCE = (
        "CREATE TABLE " + TABLE_SOURCE + " ("
            + KEY_ID + " INTEGER PRIMARY KEY, "
            + KEY_NAME + " TEXT"
            + ")")


    // downloads table create statement
    internal val CREATE_TABLE_DOWNLOAD = (
        "CREATE TABLE " + TABLE_DOWNLOAD + " ("
            + KEY_NAME + " TEXT, "
            + KEY_WEB_PAGE_ID + " INTEGER PRIMARY KEY, "
            + KEY_CHAPTER + " TEXT, "
            + KEY_STATUS + " INTEGER, "
            + KEY_ORDER_ID + " INTEGER, "
            + KEY_METADATA + " TEXT"
            + ")")


    //    // sync table create statement
    //    static final String CREATE_TABLE_SYNC =
    //            "CREATE TABLE " + TABLE_DOWNLOAD_QUEUE + " ("
    //                    + KEY_ID + " INTEGER PRIMARY KEY, "
    //                    + KEY_NAME + " TEXT UNIQUE ON CONFLICT IGNORE, "
    //                    + KEY_CHAPTER_COUNT + " INTEGER,"
    //                    + KEY_METADATA + " TEXT"
    //                    + ")";

}

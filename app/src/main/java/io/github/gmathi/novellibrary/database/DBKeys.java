package io.github.gmathi.novellibrary.database;


public class DBKeys {

    static final int VER_NOVEL_ORDER_ID = 2;
    static final int VER_WEB_PAGE_ORDER_ID = 3;
    static final int VER_NOVEL_SYNC = 4;
    //static final int VER_CHAPTER_DOWNLOADS = 5;

    static final int INITIAL_VERSION = 1;


    static final int DATABASE_VERSION = VER_NOVEL_SYNC;



    static final String DATABASE_NAME = "bnr_db";

    // Table names
    static final String TABLE_NOVEL = "novel";
    static final String TABLE_WEB_PAGE = "web_page";
    static final String TABLE_GENRE = "genre";
    static final String TABLE_NOVEL_GENRE = "novel_genre";
    static final String TABLE_DOWNLOAD_QUEUE = "download_queue";
    static final String TABLE_CHAPTERS_DOWNLOADS= "chapter_downloads";

    // Common column names
    static final String KEY_ID = "id";
    static final String KEY_NAME = "name";
    static final String KEY_URL = "url";
    static final String KEY_METADATA = "metadata";
    static final String KEY_NOVEL_ID = "novel_id";
    static final String KEY_ORDER_ID = "order_id";

    // Table novel columns
    static final String KEY_IMAGE_URL = "image_url";
    static final String KEY_RATING = "rating";
    static final String KEY_SHORT_DESCRIPTION = "short_description";
    static final String KEY_LONG_DESCRIPTION = "long_description";
    static final String KEY_IMAGE_FILE_PATH = "image_file_path";
    static final String KEY_CURRENT_WEB_PAGE_ID = "current_web_page_id";
    static final String KEY_CHAPTER_COUNT = "chapter_count";
    static final String KEY_NEW_CHAPTER_COUNT = "new_chapter_count";


    // Table web_page columns
    static final String KEY_TITLE = "title";
    static final String KEY_CHAPTER = "chapter";
    static final String KEY_FILE_PATH = "file_path";
    static final String KEY_REDIRECT_URL = "redirect_url";
    static final String KEY_IS_READ = "is_read";

    // Table novel_genre columns
    static final String KEY_GENRE_ID = "genre_id";

    //Table download_queue columns
    static final String KEY_STATUS = "status";
    static final String KEY_TOTAL_CHAPTERS = "total_chapters";
    static final String KEY_CURRENT_CHAPTER = "current_chapter";
    static final String KEY_CHAPTER_URLS_CACHED = "chapter_urls_cached";

    //Table download_queue_columns
    static final String KEY_WEB_PAGE_ID = "web_page_id";


    // novel table create statement
    static final String CREATE_TABLE_NOVEL =
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
                    + ")";

    // web_page table create statement
    static final String CREATE_TABLE_WEB_PAGE =
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
                    + KEY_ORDER_ID + " INTEGER, "
                    + "FOREIGN KEY (" + KEY_NOVEL_ID + ") REFERENCES " + TABLE_NOVEL + "(" + KEY_ID + ")"
                    + ")";

    // genre table create statement
    static final String CREATE_TABLE_GENRE =
            "CREATE TABLE " + TABLE_GENRE + " ("
                    + KEY_ID + " INTEGER PRIMARY KEY, "
                    + KEY_NAME + " TEXT"
                    + ")";

    // novel_genre table create statement
    static final String CREATE_TABLE_NOVEL_GENRE =
            "CREATE TABLE " + TABLE_NOVEL_GENRE + " ("
                    + KEY_NOVEL_ID + " INTEGER, "
                    + KEY_GENRE_ID + " INTEGER, "
                    + "FOREIGN KEY (" + KEY_NOVEL_ID + ") REFERENCES " + TABLE_NOVEL + "(" + KEY_ID + "), "
                    + "FOREIGN KEY (" + KEY_GENRE_ID + ") REFERENCES " + TABLE_GENRE + "(" + KEY_ID + ")"
                    + ")";

    // genre table create statement
    static final String CREATE_TABLE_DOWNLOAD_QUEUE =
            "CREATE TABLE " + TABLE_DOWNLOAD_QUEUE + " ("
                    + KEY_NOVEL_ID + " INTEGER PRIMARY KEY, "
                    + KEY_STATUS + " INTEGER, "
                    + KEY_METADATA + " TEXT"
                    + ")";

    // genre table create statement
//    static final String CREATE_TABLE_CHAPTER_DOWNLOADS =
//            "CREATE TABLE " + TABLE_CHAPTERS_DOWNLOADS + " ("
//                    + KEY_NOVEL_ID + " INTEGER, "
//                    + KEY_WEB_PAGE_ID + " INTEGER PRIMARY KEY, "
//                    + KEY_STATUS + " INTEGER, "
//                    + KEY_METADATA + " TEXT"
//                    + ")";


//    // sync table create statement
//    static final String CREATE_TABLE_SYNC =
//            "CREATE TABLE " + TABLE_DOWNLOAD_QUEUE + " ("
//                    + KEY_ID + " INTEGER PRIMARY KEY, "
//                    + KEY_NAME + " TEXT UNIQUE ON CONFLICT IGNORE, "
//                    + KEY_CHAPTER_COUNT + " INTEGER,"
//                    + KEY_METADATA + " TEXT"
//                    + ")";

}

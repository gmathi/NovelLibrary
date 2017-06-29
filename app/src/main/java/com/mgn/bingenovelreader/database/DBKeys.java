package com.mgn.bingenovelreader.database;


public class DBKeys {

    static final int DATABASE_VERSION = 1;

    static final String DATABASE_NAME = "bnr_db";

    // Table names
    static final String TABLE_NOVEL = "novel";
    static final String TABLE_WEB_PAGE = "web_page";
    static final String TABLE_GENRE = "genre";
    static final String TABLE_NOVEL_GENRE = "novel_genre";
    static final String TABLE_DOWNLOAD_QUEUE = "download_queue";

    // Common column names
    static final String KEY_ID = "id";
    static final String KEY_NAME = "name";
    static final String KEY_URL = "url";
    static final String KEY_NOVEL_ID = "novel_id";

    // Table novel columns
    static final String KEY_AUTHOR = "author";
    static final String KEY_IMAGE_URL = "image_url";
    static final String KEY_RATING = "rating";
    static final String KEY_SHORT_DESCRIPTION = "short_description";
    static final String KEY_LONG_DESCRIPTION = "long_description";
    static final String KEY_IMAGE_FILE_PATH = "image_file_path";
    static final String KEY_CURRENT_WEB_PAGE_ID = "current_web_page_id";

    // Table web_page columns
    static final String KEY_TITLE = "title";
    static final String KEY_CHAPTER = "chapter";
    static final String KEY_FILE_PATH = "file_path";
    static final String KEY_REDIRECT_URL = "redirect_url";

    // Table novel_genre columns
    static final String KEY_GENRE_ID = "genre_id";

    //Table download_queue columns
    static final String KEY_STATUS = "status";
    static final String KEY_TOTAL_CHAPTERS = "total_chapters";
    static final String KEY_CURRENT_CHAPTER = "current_chapter";
    static final String KEY_CHAPTER_URLS_CACHED = "chapter_urls_cached";


    // novel table create statement
    static final String CREATE_TABLE_NOVEL =
            "CREATE TABLE " + TABLE_NOVEL + " ("
                    + KEY_ID + " INTEGER PRIMARY KEY, "
                    + KEY_NAME + " TEXT, "
                    + KEY_URL + " TEXT, "
                    + KEY_AUTHOR + " TEXT, "
                    + KEY_IMAGE_URL + " TEXT, "
                    + KEY_RATING + " TEXT, "
                    + KEY_SHORT_DESCRIPTION + " TEXT, "
                    + KEY_LONG_DESCRIPTION + " TEXT, "
                    + KEY_IMAGE_FILE_PATH + " TEXT, "
                    + KEY_CURRENT_WEB_PAGE_ID + " INTEGER, "
                    + "FOREIGN KEY (" + KEY_CURRENT_WEB_PAGE_ID + ") REFERENCES " + TABLE_WEB_PAGE + "(" + KEY_ID + ")"
                    + ")";

    // web_page table create statement
    static final String CREATE_TABLE_WEB_PAGE =
            "CREATE TABLE " + TABLE_WEB_PAGE + " ("
                    + KEY_ID + " INTEGER PRIMARY KEY, "
                    + KEY_URL + " TEXT UNIQUE ON CONFLICT IGNORE, "
                    + KEY_REDIRECT_URL + " TEXT UNIQUE ON CONFLICT IGNORE, "
                    + KEY_CHAPTER + " TEXT, "
                    + KEY_TITLE + " TEXT, "
                    + KEY_FILE_PATH + " TEXT, "
                    + KEY_NOVEL_ID + " INTEGER, "
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
                    + KEY_TOTAL_CHAPTERS + " INTEGER, "
                    + KEY_CHAPTER_URLS_CACHED + " INTEGER, "
                    + KEY_CURRENT_CHAPTER + " INTEGER"
                    + ")";

}

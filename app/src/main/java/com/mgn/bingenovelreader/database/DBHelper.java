package com.mgn.bingenovelreader.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.mgn.bingenovelreader.models.DownloadQueue;
import com.mgn.bingenovelreader.models.Genre;
import com.mgn.bingenovelreader.models.Novel;
import com.mgn.bingenovelreader.models.NovelGenre;
import com.mgn.bingenovelreader.models.WebPage;
import com.mgn.bingenovelreader.utils.Constants;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class DBHelper extends SQLiteOpenHelper {
    private static final String LOG = "DBHelper";

    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_NAME = "bnr_db";

    // Table names
    private static final String TABLE_NOVEL = "novel";
    private static final String TABLE_WEB_PAGE = "web_page";
    private static final String TABLE_GENRE = "genre";
    private static final String TABLE_NOVEL_GENRE = "novel_genre";
    private static final String TABLE_DOWNLOAD_QUEUE = "download_queue";

    // Common column names
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_URL = "url";
    private static final String KEY_NOVEL_ID = "novel_id";

    // Table novel columns
    private static final String KEY_AUTHOR = "author";
    private static final String KEY_IMAGE_URL = "image_url";
    private static final String KEY_RATING = "rating";
    private static final String KEY_SHORT_DESCRIPTION = "short_description";
    private static final String KEY_LONG_DESCRIPTION = "long_description";
    private static final String KEY_IMAGE_FILE_PATH = "image_file_path";
    private static final String KEY_CURRENT_PAGE_URL = "current_page_url";

    // Table web_page columns
    private static final String KEY_TITLE = "title";
    private static final String KEY_CHAPTER = "chapter";
    private static final String KEY_FILE_PATH = "file_path";

    // Table novel_genre columns
    private static final String KEY_GENRE_ID = "genre_id";

    //Table download_queue columns
    private static final String KEY_STATUS = "status";
    private static final String KEY_TOTAL_CHAPTERS = "total_chapters";
    private static final String KEY_CURRENT_CHAPTER = "current_chapter";
    private static final String KEY_CHAPTER_URLS_CACHED = "chapter_urls_cached";


    // novel table create statement
    private static final String CREATE_TABLE_NOVEL =
            "CREATE TABLE " + TABLE_NOVEL + " ("
                    + KEY_ID + " INTEGER PRIMARY KEY, "
                    + KEY_NAME + " TEXT, "
                    + KEY_URL + " TEXT, "
                    + KEY_AUTHOR + " TEXT, "
                    + KEY_IMAGE_URL + " TEXT, "
                    + KEY_RATING + " REAL, "
                    + KEY_SHORT_DESCRIPTION + " TEXT, "
                    + KEY_LONG_DESCRIPTION + " TEXT, "
                    + KEY_IMAGE_FILE_PATH + " TEXT, "
                    + KEY_CURRENT_PAGE_URL + " TEXT"
                    + ")";

    // web_page table create statement
    private static final String CREATE_TABLE_WEB_PAGE =
            "CREATE TABLE " + TABLE_WEB_PAGE + " ("
                    + KEY_URL + " TEXT PRIMARY KEY, "
                    + KEY_CHAPTER + " TEXT, "
                    + KEY_TITLE + " TEXT, "
                    + KEY_FILE_PATH + " TEXT, "
                    + KEY_NOVEL_ID + " INTEGER, "
                    + "FOREIGN KEY (" + KEY_NOVEL_ID + ") REFERENCES " + TABLE_NOVEL + "(" + KEY_ID + ")"
                    + ")";

    // genre table create statement
    private static final String CREATE_TABLE_GENRE =
            "CREATE TABLE " + TABLE_GENRE + " ("
                    + KEY_ID + " INTEGER PRIMARY KEY, "
                    + KEY_NAME + " TEXT"
                    + ")";

    // novel_genre table create statement
    private static final String CREATE_TABLE_NOVEL_GENRE =
            "CREATE TABLE " + TABLE_NOVEL_GENRE + " ("
                    + KEY_NOVEL_ID + " INTEGER, "
                    + KEY_GENRE_ID + " INTEGER, "
                    + "FOREIGN KEY (" + KEY_NOVEL_ID + ") REFERENCES " + TABLE_NOVEL + "(" + KEY_ID + "), "
                    + "FOREIGN KEY (" + KEY_GENRE_ID + ") REFERENCES " + TABLE_GENRE + "(" + KEY_ID + ")"
                    + ")";

    // genre table create statement
    private static final String CREATE_TABLE_DOWNLOAD_QUEUE =
            "CREATE TABLE " + TABLE_DOWNLOAD_QUEUE + " ("
                    + KEY_NOVEL_ID + " INTEGER PRIMARY KEY, "
                    + KEY_STATUS + " INTEGER, "
                    + KEY_TOTAL_CHAPTERS + " INTEGER, "
                    + KEY_CHAPTER_URLS_CACHED + " INTEGER, "
                    + KEY_CURRENT_CHAPTER + " INTEGER"
                    + ")";


    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // creating required tables
        db.execSQL(CREATE_TABLE_NOVEL);
        db.execSQL(CREATE_TABLE_WEB_PAGE);
        db.execSQL(CREATE_TABLE_GENRE);
        db.execSQL(CREATE_TABLE_NOVEL_GENRE);
        db.execSQL(CREATE_TABLE_DOWNLOAD_QUEUE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // on upgrade drop older tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOVEL);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEB_PAGE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GENRE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOVEL_GENRE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DOWNLOAD_QUEUE);

        // create new tables
        onCreate(db);
    }

    // closing database
//    private void closeDB() {
//        SQLiteDatabase db = this.getReadableDatabase();
//        if (db != null && db.isOpen())
//            db.close();
//    }


    // CRUD operations for novel

    public long createNovel(Novel novel) {
        long novelId = getNovelId(novel.getName());
        if (novelId != -1) return novelId;

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, novel.getName());
        values.put(KEY_URL, novel.getUrl());
        values.put(KEY_AUTHOR, novel.getAuthor());
        values.put(KEY_IMAGE_URL, novel.getImageUrl());
        values.put(KEY_RATING, novel.getRating());
        values.put(KEY_SHORT_DESCRIPTION, novel.getShortDescription());
        values.put(KEY_LONG_DESCRIPTION, novel.getLongDescription());
        values.put(KEY_IMAGE_FILE_PATH, novel.getImageFilePath());
        values.put(KEY_CURRENT_PAGE_URL, novel.getCurrentPageUrl());

        return db.insert(TABLE_NOVEL, null, values);
    }

    public Novel getNovel(String novelName) {
        String selectQuery = "SELECT * FROM " + TABLE_NOVEL + " WHERE "
                + KEY_NAME + " = \"" + novelName + "\"";
        return getNovelFromQuery(selectQuery);
    }

    public Novel getNovel(long novelId) {
        String selectQuery = "SELECT * FROM " + TABLE_NOVEL + " WHERE "
                + KEY_ID + " = " + novelId;
        return getNovelFromQuery(selectQuery);
    }

    public Novel getNovelFromQuery(String selectQuery) {
        Log.d(LOG, selectQuery);
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        Novel novel = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                novel = new Novel();
                novel.setId(cursor.getLong(cursor.getColumnIndex(KEY_ID)));
                novel.setName(cursor.getString(cursor.getColumnIndex(KEY_NAME)));
                novel.setUrl(cursor.getString(cursor.getColumnIndex(KEY_URL)));
                novel.setAuthor(cursor.getString(cursor.getColumnIndex(KEY_AUTHOR)));
                novel.setImageUrl(cursor.getString(cursor.getColumnIndex(KEY_IMAGE_URL)));
                novel.setRating(cursor.getDouble(cursor.getColumnIndex(KEY_RATING)));
                novel.setShortDescription(cursor.getString(cursor.getColumnIndex(KEY_SHORT_DESCRIPTION)));
                novel.setLongDescription(cursor.getString(cursor.getColumnIndex(KEY_LONG_DESCRIPTION)));
                novel.setImageFilePath(cursor.getString(cursor.getColumnIndex(KEY_IMAGE_FILE_PATH)));
                novel.setCurrentPageUrl(cursor.getString(cursor.getColumnIndex(KEY_CURRENT_PAGE_URL)));
            }
            cursor.close();
        }
        return novel;
    }

    public long getNovelId(String novelName) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT id FROM " + TABLE_NOVEL + " WHERE "
                + KEY_NAME + " = \"" + novelName + "\"";

        Log.d(LOG, selectQuery);
        Cursor cursor = db.rawQuery(selectQuery, null);
        long id = -1;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                id = cursor.getLong(cursor.getColumnIndex(KEY_ID));
            }
            cursor.close();
        }
        return id;
    }

    public List<Novel> getAllNovels() {
        String selectQuery = " SELECT n.id, n.name, n.url, n.rating, n.image_file_path, group_concat(g.name)  as Genres\n" +
                " FROM novel_genre ng, genre g, novel n\n" +
                " WHERE ng.genre_id = g.id AND ng.novel_id = n.id\n" +
                " GROUP BY ng.novel_id";

        Log.d(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        List<Novel> list = new ArrayList<Novel>();
        if (cursor.moveToFirst()) {
            do {
                Novel entry = new Novel();
                entry.setId(cursor.getLong(cursor.getColumnIndex(KEY_ID)));
                entry.setName(cursor.getString(cursor.getColumnIndex(KEY_NAME)));
                entry.setUrl(cursor.getString(cursor.getColumnIndex(KEY_URL)));
                entry.setRating(cursor.getDouble(cursor.getColumnIndex(KEY_RATING)));
                entry.setGenres(Arrays.asList(cursor.getString(cursor.getColumnIndex("Genres")).split(",")));
                entry.setImageFilePath(cursor.getString(cursor.getColumnIndex(KEY_IMAGE_FILE_PATH)));
//                entry.setAuthor(cursor.getString(cursor.getColumnIndex(KEY_AUTHOR)));
//                entry.setImageUrl(cursor.getString(cursor.getColumnIndex(KEY_IMAGE_URL)));
//                entry.setShortDescription(cursor.getString(cursor.getColumnIndex(KEY_SHORT_DESCRIPTION)));
//                entry.setLongDescription(cursor.getString(cursor.getColumnIndex(KEY_LONG_DESCRIPTION)));
//                entry.setCurrentPageUrl(cursor.getString(cursor.getColumnIndex(KEY_CURRENT_PAGE_URL)));

                list.add(entry);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return list;
    }

    public long updateNovel(Novel arg) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(KEY_ID, arg.getId());
        values.put(KEY_NAME, arg.getName());
        values.put(KEY_URL, arg.getUrl());
        values.put(KEY_AUTHOR, arg.getAuthor());
        values.put(KEY_IMAGE_URL, arg.getImageUrl());
        values.put(KEY_RATING, arg.getRating());
        values.put(KEY_SHORT_DESCRIPTION, arg.getShortDescription());
        values.put(KEY_LONG_DESCRIPTION, arg.getLongDescription());
        values.put(KEY_IMAGE_FILE_PATH, arg.getImageFilePath());
        values.put(KEY_CURRENT_PAGE_URL, arg.getCurrentPageUrl());

        // updating row
        return db.update(TABLE_NOVEL, values, KEY_ID + " = ?",
                new String[]{String.valueOf(arg.getId())});
    }

    public void deleteNovel(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOVEL, KEY_ID + " = ?",
                new String[]{String.valueOf(id)});
    }


    // CRUD operations for web_page

    public long createWebPage(WebPage arg) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_URL, arg.getUrl());
        values.put(KEY_TITLE, arg.getTitle());
        values.put(KEY_CHAPTER, arg.getChapter());
        values.put(KEY_FILE_PATH, arg.getFilePath());
        values.put(KEY_NOVEL_ID, arg.getNovelId());

        return db.insert(TABLE_WEB_PAGE, null, values);
    }

    public WebPage getWebPage(long novelId, String url) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT  * FROM " + TABLE_WEB_PAGE + " WHERE "
                + KEY_NOVEL_ID + " = " + novelId + " AND " + KEY_URL + " = " + "\"" + url + "\"";
        Log.d(LOG, selectQuery);
        WebPage entry = null;
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                entry = new WebPage();
                entry.setUrl(cursor.getString(cursor.getColumnIndex(KEY_URL)));
                entry.setChapter(cursor.getString(cursor.getColumnIndex(KEY_CHAPTER)));
                entry.setTitle(cursor.getString(cursor.getColumnIndex(KEY_TITLE)));
                entry.setFilePath(cursor.getString(cursor.getColumnIndex(KEY_FILE_PATH)));
                entry.setNovelId(cursor.getLong(cursor.getColumnIndex(KEY_NOVEL_ID)));
            }
            cursor.close();
        }
        return entry;
    }

    public List<WebPage> getAllWebPages(long novelId) {
        List<WebPage> list = new ArrayList<WebPage>();
        String selectQuery = "SELECT * FROM " + TABLE_WEB_PAGE + " WHERE "
                + KEY_NOVEL_ID + " = " + novelId + " ORDER BY " + KEY_TITLE + " DESC";
        Log.d(LOG, selectQuery);
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    WebPage entry = new WebPage();
                    entry.setUrl(cursor.getString(cursor.getColumnIndex(KEY_URL)));
                    entry.setChapter(cursor.getString(cursor.getColumnIndex(KEY_CHAPTER)));
                    entry.setTitle(cursor.getString(cursor.getColumnIndex(KEY_TITLE)));
                    entry.setFilePath(cursor.getString(cursor.getColumnIndex(KEY_FILE_PATH)));
                    entry.setNovelId(cursor.getLong(cursor.getColumnIndex(KEY_NOVEL_ID)));

                    list.add(entry);
                } while (cursor.moveToNext());
            }
        }
        return list;
    }

    public long updateWebPage(WebPage arg) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_URL, arg.getUrl());
        values.put(KEY_CHAPTER, arg.getChapter());
        values.put(KEY_TITLE, arg.getTitle());
        values.put(KEY_FILE_PATH, arg.getFilePath());
        values.put(KEY_NOVEL_ID, arg.getNovelId());

        // updating row
        return db.update(TABLE_WEB_PAGE, values, KEY_NOVEL_ID + " = ? AND " + KEY_URL + " = \"" + arg.getUrl() + "\"",
                new String[]{String.valueOf(arg.getNovelId())});
    }

    public void deleteWebPage(long novelId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_WEB_PAGE, KEY_NOVEL_ID + " = ?",
                new String[]{String.valueOf(novelId)});
    }


    // CRUD operations for genre

    public long createGenre(String genreName) {
        Genre genre = getGenre(genreName);
        if (genre != null) return genre.getId();
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, genreName);
        return db.insert(TABLE_GENRE, null, values);
    }

    public Genre getGenre(String genreName) {
        String selectQuery = "SELECT * FROM " + TABLE_GENRE + " WHERE "
                + KEY_NAME + " = \"" + genreName + "\"";
        return getGenreFromQuery(selectQuery);
    }

    public Genre getGenre(long genreId) {
        String selectQuery = "SELECT * FROM " + TABLE_GENRE + " WHERE "
                + KEY_ID + " = " + genreId;
        return getGenreFromQuery(selectQuery);
    }

    public Genre getGenreFromQuery(String selectQuery) {
        SQLiteDatabase db = this.getReadableDatabase();
        Log.d(LOG, selectQuery);
        Cursor cursor = db.rawQuery(selectQuery, null);
        Genre genre = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                genre = new Genre();
                genre.setId(cursor.getLong(cursor.getColumnIndex(KEY_ID)));
                genre.setName(cursor.getString(cursor.getColumnIndex(KEY_NAME)));
            }
            cursor.close();
        }
        return genre;
    }

    public List<Genre> getAllGenre() {
        List<Genre> list = new ArrayList<Genre>();
        String selectQuery = "SELECT  * FROM " + TABLE_GENRE;

        Log.d(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                Genre entry = new Genre();
                entry.setId(c.getLong(c.getColumnIndex(KEY_ID)));
                entry.setName(c.getString(c.getColumnIndex(KEY_NAME)));

                list.add(entry);
            } while (c.moveToNext());
        }

        return list;
    }

    public long updateGenre(Genre arg) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(KEY_ID, arg.getId());
        values.put(KEY_NAME, arg.getName());

        // updating row
        return db.update(TABLE_GENRE, values, KEY_ID + " = ?",
                new String[]{String.valueOf(arg.getId())});
    }

    public void deleteGenre(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_GENRE, KEY_ID + " = ?",
                new String[]{String.valueOf(id)});
    }


    // CRUD operations for novel_genre

    public long createNovelGenre(NovelGenre arg) {
        if (!hasNovelGenreEntry(arg)) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(KEY_NOVEL_ID, arg.getNovelId());
            values.put(KEY_GENRE_ID, arg.getGenreId());
            return db.insert(TABLE_NOVEL_GENRE, null, values);
        }
        return 0;
    }

    public boolean hasNovelGenreEntry(NovelGenre novelGenre) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT  * FROM " + TABLE_NOVEL_GENRE + " WHERE "
                + KEY_NOVEL_ID + " = " + novelGenre.getNovelId() + " AND " + KEY_GENRE_ID + " = " + novelGenre.getGenreId();
        Log.d(LOG, selectQuery);
        Cursor cursor = db.rawQuery(selectQuery, null);
        boolean exists = false;
        if (cursor != null) {
            exists = cursor.moveToFirst();
            cursor.close();
        }
        return exists;
    }

    public List<NovelGenre> getAllNovelGenre(long novelId) {
        String selectQuery = "SELECT  * FROM " + TABLE_NOVEL_GENRE + " WHERE " + KEY_NOVEL_ID + " = " + novelId;
        Log.d(LOG, selectQuery);
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        List<NovelGenre> list = new ArrayList<>();
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    NovelGenre entry = new NovelGenre();
                    entry.setNovelId(cursor.getLong(cursor.getColumnIndex(KEY_NOVEL_ID)));
                    entry.setGenreId(cursor.getLong(cursor.getColumnIndex(KEY_GENRE_ID)));

                    list.add(entry);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return list;
    }

    public long updateNovelGenre(NovelGenre arg) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(KEY_NOVEL_ID, arg.getNovelId());
        values.put(KEY_GENRE_ID, arg.getGenreId());

        // updating row
        return db.update(TABLE_NOVEL_GENRE, values, KEY_NOVEL_ID + " = ?",
                new String[]{String.valueOf(arg.getNovelId())});
    }

    public void deleteNovelGenre(long novelId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOVEL_GENRE, KEY_NOVEL_ID + " = ?",
                new String[]{String.valueOf(novelId)});
    }

    public boolean createDownloadQueue(long novelId) {
        DownloadQueue dq = getDownloadQueue(novelId);
        if (dq != null) return false;

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NOVEL_ID, novelId);
        values.put(KEY_STATUS, 0);
        values.put(KEY_TOTAL_CHAPTERS, -1);
        values.put(KEY_CURRENT_CHAPTER, -1);
        values.put(KEY_CHAPTER_URLS_CACHED, 0);

        db.insert(TABLE_DOWNLOAD_QUEUE, null, values);
        return true;
    }

    public DownloadQueue getDownloadQueue(long novelId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_DOWNLOAD_QUEUE + " WHERE "
                + KEY_NOVEL_ID + " = " + novelId;
        Log.d(LOG, selectQuery);
        Cursor cursor = db.rawQuery(selectQuery, null);
        DownloadQueue dq = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                dq = new DownloadQueue();
                dq.setNovelId(cursor.getLong(cursor.getColumnIndex(KEY_NOVEL_ID)));
                dq.setStatus(cursor.getLong(cursor.getColumnIndex(KEY_STATUS)));
                dq.setTotalChapters(cursor.getLong(cursor.getColumnIndex(KEY_TOTAL_CHAPTERS)));
                dq.setCurrentChapter(cursor.getLong(cursor.getColumnIndex(KEY_CURRENT_CHAPTER)));
                dq.setChapterUrlsCached(cursor.getLong(cursor.getColumnIndex(KEY_CHAPTER_URLS_CACHED)));
            }
            cursor.close();
        }
        return dq;
    }

    public List<DownloadQueue> getAllDownloadQueue() {
        List<DownloadQueue> list = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_DOWNLOAD_QUEUE;
        Log.d(LOG, selectQuery);
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    DownloadQueue dq = new DownloadQueue();
                    dq.setNovelId(cursor.getLong(cursor.getColumnIndex(KEY_NOVEL_ID)));
                    dq.setStatus(cursor.getLong(cursor.getColumnIndex(KEY_STATUS)));
                    dq.setTotalChapters(cursor.getLong(cursor.getColumnIndex(KEY_TOTAL_CHAPTERS)));
                    dq.setCurrentChapter(cursor.getLong(cursor.getColumnIndex(KEY_CURRENT_CHAPTER)));
                    dq.setChapterUrlsCached(cursor.getLong(cursor.getColumnIndex(KEY_CHAPTER_URLS_CACHED)));

                    list.add(dq);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        return list;
    }

    public long updateDownloadQueue(DownloadQueue downloadQueue) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NOVEL_ID, downloadQueue.getNovelId());
        values.put(KEY_STATUS, downloadQueue.getStatus());
        values.put(KEY_TOTAL_CHAPTERS, downloadQueue.getTotalChapters());
        values.put(KEY_CURRENT_CHAPTER, downloadQueue.getCurrentChapter());
        values.put(KEY_CHAPTER_URLS_CACHED, downloadQueue.getChapterUrlsCached());

        // updating row
        return db.update(TABLE_DOWNLOAD_QUEUE, values, KEY_NOVEL_ID + " = ?",
                new String[]{String.valueOf(downloadQueue.getNovelId())});
    }

    public long updateDownloadQueueStatus(long status, long novelId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_STATUS, status);
        return db.update(TABLE_DOWNLOAD_QUEUE, values, KEY_NOVEL_ID + " = ?",
                new String[]{String.valueOf(novelId)});
    }

    public long updateDownloadQueueChapterCount(long totalChapters, long currentChapter, long novelId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_TOTAL_CHAPTERS, totalChapters);
        values.put(KEY_CURRENT_CHAPTER, currentChapter);
        return db.update(TABLE_DOWNLOAD_QUEUE, values, KEY_NOVEL_ID + " = ?",
                new String[]{String.valueOf(novelId)});
    }

    public long updateChapterUrlsCached(long chapterUrlsCached, long novelId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_CHAPTER_URLS_CACHED, chapterUrlsCached);
        return db.update(TABLE_DOWNLOAD_QUEUE, values, KEY_NOVEL_ID + " = ?",
                new String[]{String.valueOf(novelId)});
    }


    public void deleteDownloadQueue(long novelId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_DOWNLOAD_QUEUE, KEY_NOVEL_ID + " = ?",
                new String[]{String.valueOf(novelId)});
    }


    // Custom Methods
    public void insertNovel(Novel novel) {
        long novelId = createNovel(novel);
        if (novel.getGenres() != null && !novel.getGenres().isEmpty())
            for (String genre : novel.getGenres()) {
                long genreId = createGenre(genre);
                createNovelGenre(new NovelGenre(novelId, genreId));
            }

    }

    public List<String> getGenres(long novelId) {
        List<NovelGenre> novelGenres = getAllNovelGenre(novelId);
        List<String> genres = new ArrayList<>();
        for (NovelGenre novelGenre : novelGenres) {
            Genre genre = getGenre(novelGenre.getGenreId());
            if (genre != null)
                genres.add(genre.getName());
        }
        return genres;
    }


    public DownloadQueue getFirstDownloadableQueueItem() {
        DownloadQueue downloadQueue = null;
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_DOWNLOAD_QUEUE + " WHERE "
                + KEY_STATUS + " = " + Constants.STATUS_DOWNLOAD + " ORDER BY " + KEY_NOVEL_ID + " ASC LIMIT 1";
        Log.d(LOG, selectQuery);
        Cursor cursor = db.rawQuery(selectQuery, null);
        DownloadQueue dq = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                dq = new DownloadQueue();
                dq.setNovelId(cursor.getLong(cursor.getColumnIndex(KEY_NOVEL_ID)));
                dq.setStatus(cursor.getLong(cursor.getColumnIndex(KEY_STATUS)));
                dq.setTotalChapters(cursor.getLong(cursor.getColumnIndex(KEY_TOTAL_CHAPTERS)));
                dq.setCurrentChapter(cursor.getLong(cursor.getColumnIndex(KEY_CURRENT_CHAPTER)));
                dq.setChapterUrlsCached(cursor.getLong(cursor.getColumnIndex(KEY_CHAPTER_URLS_CACHED)));
            }
            cursor.close();
        }
        return dq;
    }

    public void insertWebPages(@NotNull ArrayList<WebPage> webPages, long novelId) {
        for (WebPage webPage : webPages) {
            webPage.setNovelId(novelId);
            if (getWebPage(novelId, webPage.getUrl()) == null)
                createWebPage(webPage);
        }
    }

    @NotNull
    public int getDownloadedChapterCount(long novelId) {
        int currentChapterCount = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT count(novel_id) FROM " + TABLE_WEB_PAGE + " WHERE "
                + KEY_NOVEL_ID + " = " + novelId + " AND file_path IS NOT NULL";
        Log.d(LOG, selectQuery);
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                currentChapterCount = cursor.getInt(0);
            }
            cursor.close();
        }
        return currentChapterCount;
    }
}


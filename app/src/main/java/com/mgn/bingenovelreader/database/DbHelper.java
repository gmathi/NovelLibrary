package com.mgn.bingenovelreader.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.mgn.bingenovelreader.models.Genre;
import com.mgn.bingenovelreader.models.Novel;
import com.mgn.bingenovelreader.models.NovelGenre;
import com.mgn.bingenovelreader.models.WebPage;

import java.util.ArrayList;
import java.util.List;


public class DbHelper extends SQLiteOpenHelper {
    private static final String LOG = "DbHelper";

    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_NAME = "bnr_db";

    // Table names
    private static final String TABLE_NOVEL = "novel";
    private static final String TABLE_WEB_PAGE = "web_page";
    private static final String TABLE_GENRE = "genre";
    private static final String TABLE_NOVEL_GENRE = "novel_genre";

    // Common column names
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_URL = "url";
    private static final String KEY_FICTION_ID = "fiction_id";

    // Table novel columns
    private static final String KEY_AUTHOR = "author";
    private static final String KEY_IMAGE_URL = "image_url";
    private static final String KEY_RATING = "rating";
    private static final String KEY_SHORT_DESCRIPTION = "short_description";
    private static final String KEY_LONG_DESCRIPTION = "long_description";
    private static final String KEY_IMAGE_DATA = "image_data";
    private static final String KEY_CURRENT_PAGE_URL = "current_page_url";
    // Table web_page columns
    private static final String KEY_TITLE = "title";
    private static final String KEY_FILE_NAME = "file_name";

    // Table novel_genre columns
    private static final String KEY_GENRE_ID = "genre_id";
    // novel table create statement
    private static final String CREATE_TABLE_NOVEL =
        "CREATE TABLE " + TABLE_NOVEL + " ("
    +    KEY_ID + " INTEGER PRIMARY KEY, "
    +    KEY_NAME + " TEXT, "
    +    KEY_URL + " TEXT, "
    +    KEY_AUTHOR + " TEXT, "
    +    KEY_IMAGE_URL + " TEXT, "
    +    KEY_RATING + " REAL, "
    +    KEY_SHORT_DESCRIPTION + " TEXT, "
    +    KEY_LONG_DESCRIPTION + " TEXT, "
    +    KEY_IMAGE_DATA + " BLOB, "
    +    KEY_CURRENT_PAGE_URL + " TEXT"
    +    ")";

    // web_page table create statement
    private static final String CREATE_TABLE_WEB_PAGE =
        "CREATE TABLE " + TABLE_WEB_PAGE + " ("
    +    KEY_URL + " TEXT PRIMARY KEY, "
    +    KEY_TITLE + " TEXT, "
    +    KEY_FILE_NAME + " TEXT, "
    +    KEY_FICTION_ID + " INTEGER, "
    +    "FOREIGN KEY ("+KEY_FICTION_ID+") REFERENCES "+TABLE_NOVEL+"("+KEY_ID+")"
    +    ")";

    // genre table create statement
    private static final String CREATE_TABLE_GENRE =
        "CREATE TABLE " + TABLE_GENRE + " ("
    +    KEY_ID + " INTEGER PRIMARY KEY, "
    +    KEY_NAME + " TEXT"
    +    ")";

    // novel_genre table create statement
    private static final String CREATE_TABLE_NOVEL_GENRE =
        "CREATE TABLE " + TABLE_NOVEL_GENRE + " ("
    +    KEY_FICTION_ID + " INTEGER, "
    +    KEY_GENRE_ID + " INTEGER, "
    +    "FOREIGN KEY ("+KEY_FICTION_ID+") REFERENCES "+TABLE_NOVEL+"("+KEY_ID+"), "
    +    "FOREIGN KEY ("+KEY_GENRE_ID+") REFERENCES "+TABLE_GENRE+"("+KEY_ID+")"
    +    ")";


    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // creating required tables
        db.execSQL(CREATE_TABLE_NOVEL);
        db.execSQL(CREATE_TABLE_WEB_PAGE);
        db.execSQL(CREATE_TABLE_GENRE);
        db.execSQL(CREATE_TABLE_NOVEL_GENRE);
    }

     @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // on upgrade drop older tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOVEL);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEB_PAGE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GENRE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOVEL_GENRE);

        // create new tables
        onCreate(db);
    }

    // closing database
    public void closeDB() {
        SQLiteDatabase db = this.getReadableDatabase();
        if (db != null && db.isOpen())
            db.close();
    }


    // CRUD operations for novel

    public long createNovel(Novel arg) {
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
        values.put(KEY_IMAGE_DATA, arg.getImageData());
        values.put(KEY_CURRENT_PAGE_URL, arg.getCurrentPageUrl());

        return db.insert(TABLE_NOVEL, null, values);
    }

    public Novel getNovel(long id) {
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT  * FROM " + TABLE_NOVEL + " WHERE "
            + KEY_ID + " = " + id;

        Log.e(LOG, selectQuery);

        Cursor c = db.rawQuery(selectQuery, null);

        if (c != null) {
            c.moveToFirst();
        }

        Novel entry = new Novel();
        entry.setId(c.getLong(c.getColumnIndex(KEY_ID)));
        entry.setName(c.getString(c.getColumnIndex(KEY_NAME)));
        entry.setUrl(c.getString(c.getColumnIndex(KEY_URL)));
        entry.setAuthor(c.getString(c.getColumnIndex(KEY_AUTHOR)));
        entry.setImageUrl(c.getString(c.getColumnIndex(KEY_IMAGE_URL)));
        entry.setRating(c.getDouble(c.getColumnIndex(KEY_RATING)));
        entry.setShortDescription(c.getString(c.getColumnIndex(KEY_SHORT_DESCRIPTION)));
        entry.setLongDescription(c.getString(c.getColumnIndex(KEY_LONG_DESCRIPTION)));
        entry.setImageData(c.getBlob(c.getColumnIndex(KEY_IMAGE_DATA)));
        entry.setCurrentPageUrl(c.getString(c.getColumnIndex(KEY_CURRENT_PAGE_URL)));

        return entry;
    }

    public List<Novel> getAllNovel() {
        List<Novel> list = new ArrayList<Novel>();
        String selectQuery = "SELECT  * FROM " + TABLE_NOVEL;

        Log.e(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                Novel entry = new Novel();
                entry.setId(c.getLong(c.getColumnIndex(KEY_ID)));
                entry.setName(c.getString(c.getColumnIndex(KEY_NAME)));
                entry.setUrl(c.getString(c.getColumnIndex(KEY_URL)));
                entry.setAuthor(c.getString(c.getColumnIndex(KEY_AUTHOR)));
                entry.setImageUrl(c.getString(c.getColumnIndex(KEY_IMAGE_URL)));
                entry.setRating(c.getDouble(c.getColumnIndex(KEY_RATING)));
                entry.setShortDescription(c.getString(c.getColumnIndex(KEY_SHORT_DESCRIPTION)));
                entry.setLongDescription(c.getString(c.getColumnIndex(KEY_LONG_DESCRIPTION)));
                entry.setImageData(c.getBlob(c.getColumnIndex(KEY_IMAGE_DATA)));
                entry.setCurrentPageUrl(c.getString(c.getColumnIndex(KEY_CURRENT_PAGE_URL)));

                list.add(entry);
            } while (c.moveToNext());
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
        values.put(KEY_IMAGE_DATA, arg.getImageData());
        values.put(KEY_CURRENT_PAGE_URL, arg.getCurrentPageUrl());

        // updating row
        return db.update(TABLE_NOVEL, values, KEY_ID + " = ?",
                new String[] { String.valueOf(arg.getId()) });
    }

    public void deleteNovel(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOVEL, KEY_ID + " = ?",
                new String[] { String.valueOf(id) });
    }


    // CRUD operations for web_page

    public long createWebPage(WebPage arg) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_URL, arg.getUrl());
        values.put(KEY_TITLE, arg.getTitle());
        values.put(KEY_FILE_NAME, arg.getFileName());
        values.put(KEY_FICTION_ID, arg.getFictionId());

        return db.insert(TABLE_WEB_PAGE, null, values);
    }

    public WebPage getWebPage(long id) {
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT  * FROM " + TABLE_WEB_PAGE + " WHERE "
            + KEY_ID + " = " + id;

        Log.e(LOG, selectQuery);

        Cursor c = db.rawQuery(selectQuery, null);

        if (c != null) {
            c.moveToFirst();
        }

        WebPage entry = new WebPage();
        entry.setUrl(c.getString(c.getColumnIndex(KEY_URL)));
        entry.setTitle(c.getString(c.getColumnIndex(KEY_TITLE)));
        entry.setFileName(c.getString(c.getColumnIndex(KEY_FILE_NAME)));
        entry.setFictionId(c.getLong(c.getColumnIndex(KEY_FICTION_ID)));

        return entry;
    }

    public List<WebPage> getAllWebPage() {
        List<WebPage> list = new ArrayList<WebPage>();
        String selectQuery = "SELECT  * FROM " + TABLE_WEB_PAGE;

        Log.e(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                WebPage entry = new WebPage();
                entry.setUrl(c.getString(c.getColumnIndex(KEY_URL)));
                entry.setTitle(c.getString(c.getColumnIndex(KEY_TITLE)));
                entry.setFileName(c.getString(c.getColumnIndex(KEY_FILE_NAME)));
                entry.setFictionId(c.getLong(c.getColumnIndex(KEY_FICTION_ID)));

                list.add(entry);
            } while (c.moveToNext());
        }

        return list;
    }

    public long updateWebPage(WebPage arg) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(KEY_URL, arg.getUrl());
        values.put(KEY_TITLE, arg.getTitle());
        values.put(KEY_FILE_NAME, arg.getFileName());
        values.put(KEY_FICTION_ID, arg.getFictionId());

        // updating row
        return db.update(TABLE_WEB_PAGE, values, KEY_ID + " = ?",
                new String[] { String.valueOf(arg.getId()) });
    }

    public void deleteWebPage(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_WEB_PAGE, KEY_ID + " = ?",
                new String[] { String.valueOf(id) });
    }


    // CRUD operations for genre

    public long createGenre(Genre arg) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_ID, arg.getId());
        values.put(KEY_NAME, arg.getName());

        return db.insert(TABLE_GENRE, null, values);
    }

    public Genre getGenre(long id) {
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT  * FROM " + TABLE_GENRE + " WHERE "
            + KEY_ID + " = " + id;

        Log.e(LOG, selectQuery);

        Cursor c = db.rawQuery(selectQuery, null);

        if (c != null) {
            c.moveToFirst();
        }

        Genre entry = new Genre();
        entry.setId(c.getLong(c.getColumnIndex(KEY_ID)));
        entry.setName(c.getString(c.getColumnIndex(KEY_NAME)));

        return entry;
    }

    public List<Genre> getAllGenre() {
        List<Genre> list = new ArrayList<Genre>();
        String selectQuery = "SELECT  * FROM " + TABLE_GENRE;

        Log.e(LOG, selectQuery);

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
                new String[] { String.valueOf(arg.getId()) });
    }

    public void deleteGenre(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_GENRE, KEY_ID + " = ?",
                new String[] { String.valueOf(id) });
    }


    // CRUD operations for novel_genre

    public long createNovelGenre(NovelGenre arg) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_FICTION_ID, arg.getFictionId());
        values.put(KEY_GENRE_ID, arg.getGenreId());

        return db.insert(TABLE_NOVEL_GENRE, null, values);
    }

    public NovelGenre getNovelGenre(long id) {
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT  * FROM " + TABLE_NOVEL_GENRE + " WHERE "
            + KEY_ID + " = " + id;

        Log.e(LOG, selectQuery);

        Cursor c = db.rawQuery(selectQuery, null);

        if (c != null) {
            c.moveToFirst();
        }

        NovelGenre entry = new NovelGenre();
        entry.setFictionId(c.getLong(c.getColumnIndex(KEY_FICTION_ID)));
        entry.setGenreId(c.getLong(c.getColumnIndex(KEY_GENRE_ID)));

        return entry;
    }

    public List<NovelGenre> getAllNovelGenre() {
        List<NovelGenre> list = new ArrayList<NovelGenre>();
        String selectQuery = "SELECT  * FROM " + TABLE_NOVEL_GENRE;

        Log.e(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                NovelGenre entry = new NovelGenre();
                entry.setFictionId(c.getLong(c.getColumnIndex(KEY_FICTION_ID)));
                entry.setGenreId(c.getLong(c.getColumnIndex(KEY_GENRE_ID)));

                list.add(entry);
            } while (c.moveToNext());
        }

        return list;
    }

    public long updateNovelGenre(NovelGenre arg) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(KEY_FICTION_ID, arg.getFictionId());
        values.put(KEY_GENRE_ID, arg.getGenreId());

        // updating row
        return db.update(TABLE_NOVEL_GENRE, values, KEY_ID + " = ?",
                new String[] { String.valueOf(arg.getId()) });
    }

    public void deleteNovelGenre(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOVEL_GENRE, KEY_ID + " = ?",
                new String[] { String.valueOf(id) });
    }


}


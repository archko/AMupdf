package cn.archko.pdf.common;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.util.ArrayList;

import cn.archko.pdf.entity.BookProgress;

/**
 * @author archko
 */
public class RecentTableManager {

    public static class ProgressTbl implements BaseColumns {

        public static final String TABLE_NAME = "progress";
        public static final String KEY_INDEX = "pindex";
        public static final String KEY_PATH = "path";
        public static final String KEY_NAME = "name";
        public static final String KEY_EXT = "ext";
        public static final String KEY_MD5 = "md5";
        public static final String KEY_PAGE_COUNT = "page_count";
        public static final String KEY_SIZE = "size";

        public static final String KEY_RECORD_FIRST_TIMESTAMP = "record_first_timestamp";
        public static final String KEY_RECORD_LAST_TIMESTAMP = "record_last_timestamp";
        public static final String KEY_RECORD_READ_TIMES = "record_read_times";

        public static final String KEY_RECORD_PROGRESS = "record_progress";
        public static final String KEY_RECORD_PAGE = "record_page";
        public static final String KEY_RECORD_ABSOLUTE_ZOOM_LEVEL = "record_absolute_zoom_level";
        public static final String KEY_RECORD_ROTATION = "record_rotation";
        public static final String KEY_RECORD_OFFSETX = "record_offsetX";
        public static final String KEY_RECORD_OFFSETY = "record_offsety";
        public static final String KEY_RECORD_AUTOCROP = "record_autocrop";
        public static final String KEY_RECORD_REFLOW = "record_reflow";
        public static final String KEY_RECORD_IS_FAVORITED = "is_favorited";
        /**
         * 0:in recent list, -1:not in recent list,but it can be showed in favorite list
         */
        public static final String KEY_RECORD_IS_IN_RECENT = "is_in_recent";
    }

    /**
     * version:4,add KEY_RECORD_IS_FAVORITED
     * version:5,add KEY_RECORD_IS_IN_RECENT
     */
    private static final int DB_VERSION = 5;
    private static final String DB_NAME = "book_progress.db";

    private static final String DATABASE_CREATE = "create table "
            + ProgressTbl.TABLE_NAME
            + "(" + ProgressTbl._ID + " integer primary key autoincrement,"
            + "" + ProgressTbl.KEY_INDEX + " integer,"
            + "" + ProgressTbl.KEY_PATH + " text ,"
            + "" + ProgressTbl.KEY_NAME + " text ,"
            + "" + ProgressTbl.KEY_EXT + " text ,"
            + "" + ProgressTbl.KEY_MD5 + " text ,"
            + "" + ProgressTbl.KEY_PAGE_COUNT + " text ,"
            + "" + ProgressTbl.KEY_SIZE + " integer ,"

            + "" + ProgressTbl.KEY_RECORD_FIRST_TIMESTAMP + " integer ,"
            + "" + ProgressTbl.KEY_RECORD_LAST_TIMESTAMP + " integer ,"
            + "" + ProgressTbl.KEY_RECORD_READ_TIMES + " integer ,"

            + "" + ProgressTbl.KEY_RECORD_PROGRESS + " integer ,"
            + "" + ProgressTbl.KEY_RECORD_PAGE + " integer ,"
            + "" + ProgressTbl.KEY_RECORD_ABSOLUTE_ZOOM_LEVEL + " real ,"
            + "" + ProgressTbl.KEY_RECORD_ROTATION + " integer ,"
            + "" + ProgressTbl.KEY_RECORD_OFFSETX + " integer ,"
            + "" + ProgressTbl.KEY_RECORD_OFFSETY + " integer ,"
            + "" + ProgressTbl.KEY_RECORD_AUTOCROP + " integer ,"
            + "" + ProgressTbl.KEY_RECORD_REFLOW + " integer ,"
            + "" + ProgressTbl.KEY_RECORD_IS_FAVORITED + " integer ,"
            + "" + ProgressTbl.KEY_RECORD_IS_IN_RECENT + " integer "
            + ");";

    private final Context context;

    private DatabaseHelper DBHelper;
    private SQLiteDatabase db;

    public RecentTableManager(Context ctx) {
        this.context = ctx;
        DBHelper = new DatabaseHelper(context);
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                db.execSQL("ALTER TABLE " + ProgressTbl.TABLE_NAME + " ADD " + ProgressTbl.KEY_RECORD_AUTOCROP + " integer");
                oldVersion = 2;
            }
            if (oldVersion < 3) {
                db.execSQL("ALTER TABLE " + ProgressTbl.TABLE_NAME + " ADD " + ProgressTbl.KEY_RECORD_REFLOW + " integer");
                oldVersion = 3;
            }
            if (oldVersion < 4) {
                db.execSQL("ALTER TABLE " + ProgressTbl.TABLE_NAME + " ADD " + ProgressTbl.KEY_RECORD_IS_FAVORITED + " integer");
                oldVersion = 4;
            }
            if (oldVersion < 5) {
                db.execSQL("ALTER TABLE " + ProgressTbl.TABLE_NAME + " ADD " + ProgressTbl.KEY_RECORD_IS_IN_RECENT + " integer");
                StringBuilder sql = new StringBuilder(120);
                sql.append("UPDATE ");
                sql.append(ProgressTbl.TABLE_NAME);
                sql.append(" SET ");
                sql.append(ProgressTbl.KEY_RECORD_IS_IN_RECENT);
                sql.append("=0");
                db.execSQL(sql.toString());
                oldVersion = 5;
            }
        }
    }

    public RecentTableManager open() throws SQLException {
        db = DBHelper.getWritableDatabase();
        return this;
    }

    public SQLiteDatabase getDb() {
        return db;
    }

    public void close() {
        DBHelper.close();
    }

    public long addProgress(BookProgress progress) {
        ContentValues cv = new ContentValues();
        cv.put(ProgressTbl.KEY_INDEX, progress.index);
        cv.put(ProgressTbl.KEY_PATH, progress.path);
        cv.put(ProgressTbl.KEY_NAME, progress.name);
        cv.put(ProgressTbl.KEY_EXT, progress.ext);
        cv.put(ProgressTbl.KEY_MD5, progress.md5);
        cv.put(ProgressTbl.KEY_PAGE_COUNT, progress.pageCount);
        cv.put(ProgressTbl.KEY_SIZE, progress.size);
        cv.put(ProgressTbl.KEY_RECORD_FIRST_TIMESTAMP, progress.firstTimestampe);
        cv.put(ProgressTbl.KEY_RECORD_LAST_TIMESTAMP, progress.lastTimestampe);
        cv.put(ProgressTbl.KEY_RECORD_READ_TIMES, progress.readTimes);
        cv.put(ProgressTbl.KEY_RECORD_PROGRESS, progress.progress);
        cv.put(ProgressTbl.KEY_RECORD_PAGE, progress.page);
        cv.put(ProgressTbl.KEY_RECORD_ABSOLUTE_ZOOM_LEVEL, progress.zoomLevel);
        cv.put(ProgressTbl.KEY_RECORD_ROTATION, progress.rotation);
        cv.put(ProgressTbl.KEY_RECORD_OFFSETX, progress.offsetX);
        cv.put(ProgressTbl.KEY_RECORD_OFFSETY, progress.offsetY);
        cv.put(ProgressTbl.KEY_RECORD_AUTOCROP, progress.autoCrop);
        cv.put(ProgressTbl.KEY_RECORD_REFLOW, progress.reflow);
        cv.put(ProgressTbl.KEY_RECORD_IS_FAVORITED, progress.isFavorited);
        cv.put(ProgressTbl.KEY_RECORD_IS_IN_RECENT, progress.inRecent);
        return db.insert(ProgressTbl.TABLE_NAME, null, cv);
    }

    public long updateProgress(BookProgress progress) {
        ContentValues cv = new ContentValues();
        cv.put(ProgressTbl.KEY_INDEX, progress.index);
        cv.put(ProgressTbl.KEY_PATH, progress.path);
        cv.put(ProgressTbl.KEY_NAME, progress.name);
        cv.put(ProgressTbl.KEY_EXT, progress.ext);
        cv.put(ProgressTbl.KEY_MD5, progress.md5);
        cv.put(ProgressTbl.KEY_PAGE_COUNT, progress.pageCount);
        cv.put(ProgressTbl.KEY_SIZE, progress.size);
        cv.put(ProgressTbl.KEY_RECORD_FIRST_TIMESTAMP, progress.firstTimestampe);
        cv.put(ProgressTbl.KEY_RECORD_LAST_TIMESTAMP, progress.lastTimestampe);
        cv.put(ProgressTbl.KEY_RECORD_READ_TIMES, progress.readTimes);
        cv.put(ProgressTbl.KEY_RECORD_PROGRESS, progress.progress);
        cv.put(ProgressTbl.KEY_RECORD_PAGE, progress.page);
        cv.put(ProgressTbl.KEY_RECORD_ABSOLUTE_ZOOM_LEVEL, progress.zoomLevel);
        cv.put(ProgressTbl.KEY_RECORD_ROTATION, progress.rotation);
        cv.put(ProgressTbl.KEY_RECORD_OFFSETX, progress.offsetX);
        cv.put(ProgressTbl.KEY_RECORD_OFFSETY, progress.offsetY);
        cv.put(ProgressTbl.KEY_RECORD_AUTOCROP, progress.autoCrop);
        cv.put(ProgressTbl.KEY_RECORD_REFLOW, progress.reflow);
        cv.put(ProgressTbl.KEY_RECORD_IS_FAVORITED, progress.isFavorited);
        cv.put(ProgressTbl.KEY_RECORD_IS_IN_RECENT, progress.inRecent);
        long count = 0;
        count = db.update(ProgressTbl.TABLE_NAME, cv, ProgressTbl.KEY_NAME + "='" + progress.name + "'", null);
        return count;
    }

    /**
     * get bookprogress by inRecent
     *
     * @param name
     * @param inRecent
     * @return
     */
    public BookProgress getProgress(String name, int inRecent) {
        BookProgress entry = null;

        Cursor cur = null;

        try {
            String selection = ProgressTbl.KEY_NAME + "=? and "
                    + ProgressTbl.KEY_RECORD_IS_IN_RECENT + "='" + inRecent + "'";
            if (inRecent == BookProgress.ALL) {
                selection = ProgressTbl.KEY_NAME + "=?";
            }
            cur = db.query(true, ProgressTbl.TABLE_NAME, null,
                    selection, new String[]{name},
                    null, null, null, "1");
            if (cur != null) {
                if (cur.moveToFirst()) {
                    entry = fillProgress(cur);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != cur) {
                cur.close();
            }
        }
        return entry;
    }

    private BookProgress fillProgress(Cursor cur) {
        BookProgress entry;
        int _id = cur.getInt(cur.getColumnIndex(ProgressTbl._ID));
        int index = cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_INDEX));
        String path = cur.getString(cur.getColumnIndex(ProgressTbl.KEY_PATH));
        String name = cur.getString(cur.getColumnIndex(ProgressTbl.KEY_NAME));
        String ext = cur.getString(cur.getColumnIndex(ProgressTbl.KEY_EXT));
        String md5 = cur.getString(cur.getColumnIndex(ProgressTbl.KEY_MD5));
        int pageCount = cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_PAGE_COUNT));
        long size = cur.getLong(cur.getColumnIndex(ProgressTbl.KEY_SIZE));

        long firstT = cur.getLong(cur.getColumnIndex(ProgressTbl.KEY_RECORD_FIRST_TIMESTAMP));
        long lastT = cur.getLong(cur.getColumnIndex(ProgressTbl.KEY_RECORD_LAST_TIMESTAMP));
        int readTime = cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_RECORD_READ_TIMES));

        int progress = cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_RECORD_PROGRESS));
        int page = cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_RECORD_PAGE));
        float zoomLevel = cur.getFloat(cur.getColumnIndex(ProgressTbl.KEY_RECORD_ABSOLUTE_ZOOM_LEVEL));
        int rotation = cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_RECORD_ROTATION));
        int offsetX = cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_RECORD_OFFSETX));
        int offsetY = cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_RECORD_OFFSETY));
        int autoCrop = cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_RECORD_AUTOCROP));
        int reflow = cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_RECORD_REFLOW));
        int isFavorited = cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_RECORD_IS_FAVORITED));
        int inRecent = cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_RECORD_IS_IN_RECENT));

        entry = new BookProgress(_id, index, path, name, ext, md5, pageCount, size, firstT, lastT,
                readTime, progress, page, zoomLevel, rotation, offsetX, offsetY, autoCrop, reflow, isFavorited, inRecent);
        return entry;
    }

    public ArrayList<BookProgress> getProgresses() {
        ArrayList<BookProgress> list = null;

        Cursor cur = null;
        try {
            cur = db.query(ProgressTbl.TABLE_NAME, null,
                    null, null, null, null, ProgressTbl.KEY_RECORD_LAST_TIMESTAMP + " desc");
            if (cur != null) {
                list = new ArrayList<BookProgress>();
                if (cur.moveToFirst()) {
                    do {
                        list.add(fillProgress(cur));
                    } while (cur.moveToNext());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != cur) {
                cur.close();
            }
        }

        //Collections.sort(list);

        return list;
    }

    public ArrayList<BookProgress> getProgresses(int start, int count) {
        return getProgresses(start, count, null);
    }

    public ArrayList<BookProgress> getProgresses(int start, int count, String selection) {
        ArrayList<BookProgress> list = null;

        Cursor cur = null;
        try {
            cur = db.query(ProgressTbl.TABLE_NAME, null,
                    selection, null, null, null, ProgressTbl.KEY_RECORD_LAST_TIMESTAMP + " desc", start + " , " + count);
            if (cur != null) {
                list = new ArrayList<BookProgress>();
                if (cur.moveToFirst()) {
                    do {
                        list.add(fillProgress(cur));
                    } while (cur.moveToNext());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != cur) {
                cur.close();
            }
        }

        //Collections.sort(list);

        return list;
    }

    public int getProgressCount() {
        Cursor cur = null;
        try {
            cur = db.query(ProgressTbl.TABLE_NAME, new String[]{"_id"},
                    null, null, null, null, null);
            if (cur != null && cur.getCount() > 0) {
                return cur.getCount();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != cur) {
                cur.close();
            }
        }

        return 0;
    }

    public void deleteProgress(String name) {
        db.delete(ProgressTbl.TABLE_NAME, ProgressTbl.KEY_NAME + "='" + name + "'", null);
    }

    public void deleteProgress(BookProgress progress) {
        db.delete(ProgressTbl.TABLE_NAME, ProgressTbl.KEY_NAME + "='" + progress.name + "'", null);
    }

    //===================== favorite =====================

    public int getFavoriteProgressCount(int isFavorited) {
        Cursor cur = null;
        try {
            cur = db.query(ProgressTbl.TABLE_NAME, new String[]{"_id"},
                    ProgressTbl.KEY_RECORD_IS_FAVORITED + "='" + isFavorited + "'", null, null, null, null);
            if (cur != null && cur.getCount() > 0) {
                return cur.getCount();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != cur) {
                cur.close();
            }
        }

        return 0;
    }
}

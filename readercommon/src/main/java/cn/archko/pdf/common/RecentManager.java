package cn.archko.pdf.common;

import android.os.AsyncTask;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;

import cn.archko.pdf.App;
import cn.archko.pdf.entity.BookProgress;
import cn.archko.pdf.listeners.DataListener;
import cn.archko.pdf.utils.DateUtil;
import cn.archko.pdf.utils.FileUtils;
import cn.archko.pdf.utils.StreamUtils;
import cn.archko.pdf.utils.Utils;

/**
 * 存储最近阅读的记录
 *
 * @author: archko 2014/4/17 :15:05
 */
public class RecentManager {

    public static final String TAG = "RecentManager";

    public static RecentManager getInstance() {
        return Factory.instance;
    }

    private RecentTableManager recentTableManager;

    private static final class Factory {
        private static final RecentManager instance = new RecentManager();
    }

    private RecentManager() {
        recentTableManager = new RecentTableManager(App.getInstance());
    }

    public RecentTableManager getRecentTableManager() {
        return recentTableManager;
    }

    //------------------- operation of db -------------------

    public void addAsyncToDB(final BookProgress progress, final DataListener dataListener) {
        Utils.execute(true, new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                addToDb(progress);
                if (null != dataListener) {
                    dataListener.onSuccess();
                }
                return null;
            }
        }, (Void[]) null);
    }

    public void addToDb(BookProgress progress) {
        if (null == progress || TextUtils.isEmpty(progress.path) || TextUtils.isEmpty(progress.name)) {
            Logcat.d("", "path is null." + progress);
            return;
        }

        try {
            String filepath = FileUtils.getStoragePath(progress.path);
            File file = new File(filepath);

            BookProgress old = recentTableManager.getProgress(file.getName(), BookProgress.ALL);
            if (old == null) {
                old = progress;

                old.lastTimestampe = System.currentTimeMillis();

                recentTableManager.addProgress(old);
            } else {
                progress.lastTimestampe = System.currentTimeMillis();
                progress.isFavorited = old.isFavorited;

                recentTableManager.updateProgress(progress);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ArrayList<BookProgress> deleteFromDb(final String absolutePath) {
        Logcat.d(TAG, "remove:" + absolutePath);
        if (TextUtils.isEmpty(absolutePath)) {
            Logcat.d("", "path is null.");
            return null;
        }

        try {
            File file = new File(absolutePath);

            recentTableManager.deleteProgress(file.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * only remove progress,not isfavorited
     *
     * @param absolutePath
     * @return
     */
    public ArrayList<BookProgress> removeRecentFromDb(final String absolutePath) {
        Logcat.d(TAG, "removeRecentFromDb:" + absolutePath);
        if (TextUtils.isEmpty(absolutePath)) {
            Logcat.d("", "path is null.");
            return null;
        }

        try {
            File file = new File(absolutePath);

            BookProgress bookProgress = recentTableManager.getProgress(file.getName(), BookProgress.ALL);
            bookProgress.firstTimestampe = 0;
            bookProgress.page = 0;
            bookProgress.progress = 0;
            bookProgress.inRecent = -1;
            recentTableManager.updateProgress(bookProgress);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public BookProgress readRecentFromDb(String absolutePath) {
        return readRecentFromDb(absolutePath, BookProgress.IN_RECENT);
    }

    public BookProgress readRecentFromDb(String absolutePath, int recent) {
        BookProgress progress = null;
        try {
            File file = new File(absolutePath);

            progress = recentTableManager.getProgress(file.getName(), recent);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return progress;
    }

    public ArrayList<BookProgress> readRecentFromDb(int start, int count) {
        ArrayList<BookProgress> list = null;
        try {
            list = recentTableManager.getProgresses(start, count, RecentTableManager.ProgressTbl.KEY_RECORD_IS_IN_RECENT + "='0'");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public int getProgressCount() {
        try {
            return recentTableManager.getProgressCount();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public String backupFromDb() {
        String name = "mupdf_" + DateUtil.formatTime(System.currentTimeMillis(), "yyyy-MM-dd-HH-mm-ss");
        return backupFromDb(name);
    }

    public String backupFromDb(String name) {
        try {
            ArrayList<BookProgress> list = recentTableManager.getProgresses();
            JSONObject root = new JSONObject();
            JSONArray ja = new JSONArray();
            root.put("root", ja);
            root.put("name", name);

            for (BookProgress progress : list) {
                BookProgressParser.addProgressToJson(progress, ja);
            }
            File dir = FileUtils.getStorageDir("amupdf");
            if (dir != null && dir.exists()) {
                String filePath = dir.getAbsolutePath() + File.separator + name;
                Logcat.d(TAG, "backup.name:" + filePath + " root:" + root);
                StreamUtils.copyStringToFile(root.toString(), filePath);
            }
            return name;
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean restoreToDb(String absolutePath) {
        return restoreToDb(new File(absolutePath));
    }

    public boolean restoreToDb(File file) {
        boolean flag = false;
        try {
            String content = StreamUtils.readStringFromFile(file);
            Logcat.longLog(TAG, "restore.file:" + file.getAbsolutePath() + " content:" + content);
            ArrayList<BookProgress> progresses = BookProgressParser.parseProgresses(content);

            try {
                recentTableManager.getDb().beginTransaction();
                recentTableManager.getDb().delete(RecentTableManager.ProgressTbl.TABLE_NAME, null, null);
                for (BookProgress progress : progresses) {
                    if (!TextUtils.isEmpty(progress.name)) {
                        recentTableManager.addProgress(progress);
                        //Logcat.d(TAG, "add progress:" + progress);
                    }
                }
                flag = true;
                recentTableManager.getDb().setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                recentTableManager.getDb().endTransaction();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    public File getBackupFile() {
        File dir = FileUtils.getStorageDir("amupdf");
        if (!dir.exists()) {
            return null;
        }
        File file = null;
        try {
            File[] files = dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.getName().startsWith("mupdf_");
                }
            });

            if (files.length > 0) {
                Arrays.sort((files), (f1, f2) -> {
                    if (f1 == null) throw new RuntimeException("f1 is null inside sort");
                    if (f2 == null) throw new RuntimeException("f2 is null inside sort");
                    try {
                        return (int) (f2.lastModified() - f1.lastModified());
                    } catch (NullPointerException e) {
                        throw new RuntimeException("failed to compare " + f1 + " and " + f2, e);
                    }
                });
                file = files[0];
            }
        } catch (NullPointerException e) {
            throw new RuntimeException("failed to sort file list " + " for path ", e);
        }
        return file;
    }


    //===================== favorite =====================

    public ArrayList<BookProgress> readFavoriteFromDb(int start, int count) {
        ArrayList<BookProgress> list = null;
        try {
            list = recentTableManager.getProgresses(start, count, RecentTableManager.ProgressTbl.KEY_RECORD_IS_FAVORITED + "='1'");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public int getFavoriteProgressCount() {
        try {
            return recentTableManager.getFavoriteProgressCount(1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
}

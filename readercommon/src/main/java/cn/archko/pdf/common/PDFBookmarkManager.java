package cn.archko.pdf.common;

import android.util.Log;

import cn.archko.pdf.entity.BookProgress;
import cn.archko.pdf.listeners.DataListener;
import cn.archko.pdf.utils.FileUtils;

import static cn.archko.pdf.common.RecentManager.TAG;

/**
 * @author: archko 2018/7/22 :12:43
 */
public class PDFBookmarkManager {

    private BookProgress bookmarkToRestore = null;

    public BookProgress getBookmarkToRestore() {
        return bookmarkToRestore;
    }

    public void setStartBookmark(String absolutePath, int autoCrop) {
        BookProgress progress = RecentManager.getInstance().readRecentFromDb(absolutePath, BookProgress.ALL);
        bookmarkToRestore = progress;
        if (null == bookmarkToRestore) {
            bookmarkToRestore = new BookProgress(FileUtils.getRealPath(absolutePath));
            bookmarkToRestore.autoCrop = autoCrop;
        }
        bookmarkToRestore.readTimes = bookmarkToRestore.readTimes + 1;
        bookmarkToRestore.inRecent = 0;
    }

    public int getBookmark() {
        if (bookmarkToRestore == null) {
            return 0;
        }
        int currentPage = 0;

        if (0 < bookmarkToRestore.page) {
            currentPage = bookmarkToRestore.page;
        }
        return currentPage;
    }

    public int restoreBookmark(int pageCount) {
        if (bookmarkToRestore == null) {
            return 0;
        }
        int currentPage = 0;

        if (bookmarkToRestore.pageCount != pageCount || bookmarkToRestore.page > pageCount) {
            bookmarkToRestore = null;
            return currentPage;
        }

        if (0 < bookmarkToRestore.page) {
            currentPage = bookmarkToRestore.page;
        }
        return currentPage;
    }

    public void saveCurrentPage(String absolutePath, int pageCount, int currentPage, float zoom, int scrollX, int scrollY) {
        if (null == bookmarkToRestore) {
            bookmarkToRestore = new BookProgress(FileUtils.getRealPath(absolutePath));
        } else {
            bookmarkToRestore.path = FileUtils.getRealPath(absolutePath);
            bookmarkToRestore.readTimes = bookmarkToRestore.readTimes++;
        }
        bookmarkToRestore.inRecent = 0;
        bookmarkToRestore.pageCount = pageCount;
        bookmarkToRestore.page = currentPage;
        //if (zoom != 1000f) {
        bookmarkToRestore.zoomLevel = zoom;
        //}

        if (scrollX >= 0) { //for mupdfrecycleractivity,don't modify scrollx
            bookmarkToRestore.offsetX = scrollX;
        }
        bookmarkToRestore.offsetY = scrollY;
        bookmarkToRestore.progress = currentPage * 100 / pageCount;
        Log.i(TAG, String.format("last page saved for currentPage:%s, :%s", currentPage, bookmarkToRestore));
        RecentManager.getInstance().addAsyncToDB(bookmarkToRestore,
                new DataListener() {
                    @Override
                    public void onSuccess(Object... args) {
                        Log.i(TAG, "onSuccess");
                    }

                    @Override
                    public void onFailed(Object... args) {
                        Log.i(TAG, "onFailed");
                    }
                });
    }
}

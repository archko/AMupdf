package cn.archko.pdf.common;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

import cn.archko.pdf.entity.FileBean;
import cn.archko.pdf.listeners.DataListener;
import cn.archko.pdf.entity.BookProgress;
import cn.archko.pdf.utils.Utils;

/**
 * @author: archko 2018/2/24 :14:58
 */
public class ProgressScaner {

    private AsyncTask<Void, Void, List<FileBean>> mAsyncTask;

    public void startScan(final List<FileBean> fileListEntries, final String currentPath, final DataListener dataListener) {
        if (null != mAsyncTask) {
            mAsyncTask.cancel(true);
        }

        mAsyncTask = new AsyncTask<Void, Void, List<FileBean>>() {
            @Override
            protected List<FileBean> doInBackground(Void... voids) {
                List<FileBean> entries = new ArrayList<>();
                RecentManager recent = RecentManager.getInstance();
                for (FileBean entry : fileListEntries) {
                    FileBean listEntry = entry.clone();
                    if (null != listEntry) {
                        entries.add(listEntry);
                        if (!listEntry.isDirectory() && entry.getFile() != null) {
                            BookProgress progress = recent.readRecentFromDb(entry.getFile().getAbsolutePath(), BookProgress.ALL);
                            if (null != progress) {
                                listEntry.setBookProgress(progress);
                            }
                        }
                    }
                }
                return entries;
            }

            @Override
            protected void onPostExecute(List<FileBean> listEntries) {
                if (null != dataListener && listEntries.size() > 0) {
                    dataListener.onSuccess(currentPath, listEntries);
                }
            }
        };
        Utils.execute(true, mAsyncTask);
    }
}

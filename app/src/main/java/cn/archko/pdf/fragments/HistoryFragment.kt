package cn.archko.pdf.fragments

import android.app.ProgressDialog
import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.mupdf.R
import cn.archko.pdf.App
import cn.archko.pdf.activities.PdfOptionsActivity
import cn.archko.pdf.adapters.BookAdapter
import cn.archko.pdf.common.Event
import cn.archko.pdf.common.Event.*
import cn.archko.pdf.common.ImageLoader
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.RecentManager
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.utils.LengthUtils
import cn.archko.pdf.widgets.IMoreView
import cn.archko.pdf.widgets.ListMoreView
import com.jeremyliao.liveeventbus.LiveEventBus
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.File
import java.util.*

/**
 * @description:history list
 * *
 * @author: archko 11-11-17
 */
class HistoryFragment : BrowserFragment() {

    private var curPage = 0
    internal var mListMoreView: ListMoreView? = null
    private var mStyle: Int = STYLE_GRID;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = IntentFilter()
        filter.addAction(ACTION_STOPPED)
        filter.addAction(ACTION_FAVORITED)
        filter.addAction(ACTION_UNFAVORITED)
        LiveEventBus
                .get(Event.ACTION_STOPPED, FileBean::class.java)
                .observe(this, object : Observer<FileBean> {
                    override fun onChanged(t: FileBean?) {
                        loadData()
                    }
                })
        LiveEventBus
                .get(Event.ACTION_FAVORITED, FileBean::class.java)
                .observe(this, object : Observer<FileBean> {
                    override fun onChanged(t: FileBean?) {
                        updateItem(t)
                    }
                })
        LiveEventBus
                .get(Event.ACTION_UNFAVORITED, FileBean::class.java)
                .observe(this, object : Observer<FileBean> {
                    override fun onChanged(t: FileBean?) {
                        updateItem(t)
                    }
                })
    }

    override fun updateItem() {
        currentBean = null
    }

    private fun updateItem(fileBean: FileBean?) {
        if (null != fileBean && null != fileListAdapter && null != fileBean.bookProgress) {
            for (fb in fileListAdapter!!.data) {
                if (null != fb.bookProgress && fb.bookProgress._id == fileBean.bookProgress._id) {
                    fb.bookProgress.isFavorited = fileBean.bookProgress.isFavorited
                    break
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val options = PreferenceManager.getDefaultSharedPreferences(activity)
        mStyle = Integer.parseInt(options.getString(PdfOptionsActivity.PREF_LIST_STYLE, "0")!!)
        applyStyle()
    }

    override fun onDestroy() {
        super.onDestroy()
        ImageLoader.getInstance().recycle()
    }

    override fun onBackPressed(): Boolean {
        return false
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.action_backup -> backup()
            R.id.action_restore -> restore()
            R.id.action_style -> {
                if (mStyle == STYLE_LIST) {
                    mStyle = STYLE_GRID;
                } else {
                    mStyle = STYLE_LIST
                }
                PreferenceManager.getDefaultSharedPreferences(activity)
                        .edit()
                        .putString(PdfOptionsActivity.PREF_LIST_STYLE, mStyle.toString())
                        .apply()
                applyStyle()
            }
        }

        return super.onOptionsItemSelected(menuItem)
    }

    private fun backup() {
        val progressDialog = ProgressDialog(activity)
        progressDialog.setTitle("Waiting...")
        progressDialog.setMessage("Waiting...")
        val now = System.currentTimeMillis()
        doAsync {
            uiThread {
                progressDialog.setCancelable(false)
                progressDialog.show()
            }
            val filepath = RecentManager.getInstance().backupFromDb()
            var newTime = System.currentTimeMillis() - now
            if (newTime < 1500L) {
                newTime = 1500L - newTime
            } else {
                newTime = 0
            }

            try {
                Thread.sleep(newTime)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            uiThread {
                progressDialog.dismiss()

                if (!LengthUtils.isEmpty(filepath)) {
                    Logcat.d("", "file:" + filepath)
                    Toast.makeText(App.getInstance(), "备份成功:" + filepath, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(App.getInstance(), "备份失败", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun restore() {
        val progressDialog = ProgressDialog(activity)
        progressDialog.setTitle("Waiting...")
        progressDialog.setMessage("Waiting...")
        val now = System.currentTimeMillis()
        doAsync {
            uiThread {
                progressDialog.setCancelable(false)
                progressDialog.show()
            }
            var file: File? = RecentManager.getInstance().getBackupFile();
            var flag = false
            if (null == file) {
                Toast.makeText(App.getInstance(), "没有发现amupdf目录中有备份!", Toast.LENGTH_LONG).show()
            } else {
                flag = RecentManager.getInstance().restoreToDb(file)
                var newTime = System.currentTimeMillis() - now
                if (newTime < 1500L) {
                    newTime = 1500L - newTime
                } else {
                    newTime = 0
                }

                try {
                    Thread.sleep(newTime)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            uiThread {
                progressDialog.dismiss()

                if (flag) {
                    Toast.makeText(App.getInstance(), "恢复成功:" + flag, Toast.LENGTH_LONG).show()
                    loadData()
                } else {
                    Toast.makeText(App.getInstance(), "恢复失败", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        this.pathTextView!!.visibility = View.GONE
        filesListView!!.setOnScrollListener(onScrollListener)
        mListMoreView = ListMoreView(filesListView)
        fileListAdapter!!.addFootView(mListMoreView?.loadMoreView)

        return view
    }

    private fun applyStyle() {
        if (mStyle == STYLE_LIST) {
            fileListAdapter!!.setMode(BookAdapter.TYPE_RENCENT)
            filesListView!!.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
            fileListAdapter!!.notifyDataSetChanged()
        } else {
            fileListAdapter!!.setMode(BookAdapter.TYPE_GRID)

            filesListView!!.layoutManager = GridLayoutManager(activity, 3)
            fileListAdapter!!.notifyDataSetChanged()
        }
    }

    private fun reset() {
        curPage = 0
        currentBean = null
    }

    override fun loadData() {
        reset()
        getHistory()
    }

    private fun getHistory() {
        mListMoreView?.onLoadingStateChanged(IMoreView.STATE_LOADING)
        doAsync {
            //final long now=System.currentTimeMillis();
            val recent = RecentManager.getInstance()
            val totalCount = recent.progressCount
            val progresses = recent.readRecentFromDb(PAGE_SIZE * (curPage), PAGE_SIZE)

            val entryList = ArrayList<FileBean>()

            var entry: FileBean
            var file: File
            val path = Environment.getExternalStorageDirectory().path
            progresses?.map {
                try {
                    file = File(path + "/" + it.path)
                    entry = FileBean(FileBean.RECENT, file, showExtension)
                    entry.bookProgress = it
                    entryList.add(entry)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            uiThread {
                mSwipeRefreshWidget!!.isRefreshing = false
                if (entryList.size > 0) {
                    if (curPage == 0) {
                        fileListAdapter!!.setData(entryList)
                        //submitList(fileListAdapter!!.data, entryList, fileListAdapter!!, totalCount)
                        //return@uiThread
                        fileListAdapter!!.notifyDataSetChanged()
                    } else {
                        val index = fileListAdapter!!.itemCount;
                        fileListAdapter!!.addData(entryList)
                        fileListAdapter!!.notifyItemRangeInserted(index, entryList.size)
                    }

                    curPage++
                }
                updateLoadingStatus(totalCount)
            }
        }
    }

    private fun updateLoadingStatus(totalCount: Int) {
        Logcat.d(String.format("total count:%s, adapter count:%s", totalCount, fileListAdapter!!.normalCount))
        if (fileListAdapter!!.normalCount > 0) {
            if (fileListAdapter!!.normalCount < totalCount) {
                mListMoreView?.onLoadingStateChanged(IMoreView.STATE_NORMAL)
            } else {
                Logcat.d("fileListAdapter!!.normalCount < totalCount")
                mListMoreView?.onLoadingStateChanged(IMoreView.STATE_NO_MORE)
            }
        } else {
            Logcat.d("fileListAdapter!!.normalCount <= 0")
            mListMoreView?.onLoadingStateChanged(IMoreView.STATE_NO_MORE)
            val sp = context!!.getSharedPreferences(PREF_BROWSER, Context.MODE_PRIVATE)
            val isFirst = sp.getBoolean(PREF_BROWSER_KEY_FIRST, true)
            if (isFirst) {
                LiveEventBus.get(Event.ACTION_ISFIRST)
                        .post(true)
                sp.edit()
                        .putBoolean(PREF_BROWSER_KEY_FIRST, false)
                        .apply()
            }
        }
    }

    private val onScrollListener: RecyclerView.OnScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                if (mListMoreView?.state == IMoreView.STATE_NORMAL
                        || mListMoreView?.state == IMoreView.STATE_LOAD_FAIL) {
                    var isReachBottom = false
                    if (mStyle == STYLE_GRID) {
                        val gridLayoutManager = filesListView?.layoutManager as GridLayoutManager
                        val rowCount = fileListAdapter!!.getItemCount() / gridLayoutManager.spanCount
                        val lastVisibleRowPosition = gridLayoutManager.findLastVisibleItemPosition() / gridLayoutManager.spanCount
                        isReachBottom = lastVisibleRowPosition >= rowCount - 1
                    } else if (mStyle == STYLE_LIST) {
                        val layoutManager: LinearLayoutManager = filesListView?.layoutManager as LinearLayoutManager
                        val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                        val rowCount = fileListAdapter!!.getItemCount()
                        isReachBottom = lastVisibleItemPosition >= rowCount - fileListAdapter!!.headersCount - fileListAdapter!!.footersCount
                    }
                    if (isReachBottom) {
                        mListMoreView?.onLoadingStateChanged(IMoreView.STATE_LOADING)
                        loadMore()
                    }
                }
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        }
    }

    private fun loadMore() {
        Logcat.d("loadMore")
        getHistory()
    }

    companion object {

        const val TAG = "HistoryFragment"
        const val PREF_BROWSER = "pref_browser"
        const val PREF_BROWSER_KEY_FIRST = "pref_browser_key_first"
        const val PAGE_SIZE = 21
        @JvmField
        val STYLE_LIST = 0;
        @JvmField
        val STYLE_GRID = 1;
    }


    /*fun submitList(oldList: List<FileBean>, newList: List<FileBean>, adapter: BookAdapter, totalCount: Int) {
        AppExecutors.instance.diskIO().execute(object : Runnable {
            override fun run() {
                var diffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {

                    override fun getOldListSize(): Int {
                        // 返回旧数据的长度
                        return if (oldList == null) {
                            0
                        } else {
                            oldList.size
                        }
                    }

                    override fun getNewListSize(): Int {
                        // 返回新数据的长度
                        if (newList == null) {
                            return 0
                        } else {
                            return oldList.size
                        }
                    }

                    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        return TextUtils.equals(oldList.get(oldItemPosition).bookProgress.name, newList.get(oldItemPosition).bookProgress.name);
                    }

                    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        return oldList.get(oldItemPosition).bookProgress.progress == newList.get(newItemPosition).bookProgress.progress;
                    }
                });
                AppExecutors.instance.mainThread().execute(object : Runnable {
                    override fun run() {
                        adapter.setData(newList);
                        diffResult.dispatchUpdatesTo(adapter)
                        curPage++
                        updateLoadingStatus(totalCount)
                    }
                })
            }
        })
    }*/
}

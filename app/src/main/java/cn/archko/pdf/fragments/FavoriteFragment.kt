package cn.archko.pdf.fragments

import android.content.IntentFilter
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.adapters.BookAdapter
import cn.archko.pdf.common.Event
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.RecentManager
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.widgets.IMoreView
import cn.archko.pdf.widgets.ListMoreView
import com.jeremyliao.liveeventbus.LiveEventBus
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.File
import java.util.*

/**
 * @description:favorite list
 * *
 * @author: archko 2019/9/19 :19:47
 */
class FavoriteFragment : BrowserFragment() {

    internal var curPage = 0
    internal var mListMoreView: ListMoreView? = null
    private var mStyle: Int = STYLE_LIST;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = IntentFilter()
        filter.addAction(Event.ACTION_FAVORITED)
        filter.addAction(Event.ACTION_UNFAVORITED)
        LiveEventBus
                .get(Event.ACTION_FAVORITED, FileBean::class.java)
                .observe(this, object : Observer<FileBean> {
                    override fun onChanged(t: FileBean?) {
                        Logcat.d(TAG, "FAVORITED:$t")
                        loadData()
                    }
                })
        LiveEventBus
                .get(Event.ACTION_UNFAVORITED, FileBean::class.java)
                .observe(this, object : Observer<FileBean> {
                    override fun onChanged(t: FileBean?) {
                        Logcat.d(TAG, "UNFAVORITED:$t")
                        loadData()
                    }
                })
    }

    override fun onResume() {
        super.onResume()
        /*val options = PreferenceManager.getDefaultSharedPreferences(activity)
        mStyle = Integer.parseInt(options.getString(PdfOptionsActivity.PREF_LIST_STYLE, "0")!!)
        applyStyle()*/
    }

    override fun onDestroy() {
        super.onDestroy()
        //ImageLoader.getInstance(activity).recycle()
    }

    override fun onBackPressed(): Boolean {
        return false
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        /*when (menuItem.itemId) {
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
        }*/

        return super.onOptionsItemSelected(menuItem)
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
    }

    override fun loadData() {
        reset()
        getFavorities()
    }

    private fun getFavorities() {
        mListMoreView?.onLoadingStateChanged(IMoreView.STATE_LOADING)
        doAsync {
            val recent = RecentManager.getInstance()
            val totalCount = recent.favoriteProgressCount
            val progresses = recent.readFavoriteFromDb(PAGE_SIZE * (curPage), PAGE_SIZE)
            val entryList = ArrayList<FileBean>()

            var entry: FileBean
            var file: File
            val path = Environment.getExternalStorageDirectory().path
            progresses?.map {
                try {
                    file = File(path + "/" + it.path)
                    entry = FileBean(FileBean.FAVORITE, file, true)
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
        Logcat.d(String.format("$this, total count:%s, adapter count:%s", totalCount, fileListAdapter!!.normalCount))
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
        }
    }

    val onScrollListener: RecyclerView.OnScrollListener = object : RecyclerView.OnScrollListener() {
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
        getFavorities()
    }

    companion object {

        val TAG = "FavoriteFragment"
        internal val PAGE_SIZE = 21
        @JvmField
        val STYLE_LIST = 0;
        @JvmField
        val STYLE_GRID = 1;
    }

}
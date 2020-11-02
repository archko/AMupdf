package cn.archko.pdf.fragments

import android.content.IntentFilter
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.common.Event
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.RecentManager
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.widgets.IMoreView
import cn.archko.pdf.widgets.ListMoreView
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

/**
 * @description:favorite list
 * *
 * @author: archko 2019/9/19 :19:47
 */
class FavoriteFragment : BrowserFragment() {

    private var curPage = 0
    private lateinit var mListMoreView: ListMoreView
    private var mStyle: Int = HistoryFragment.STYLE_LIST

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

    override fun onBackPressed(): Boolean {
        return false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        this.pathTextView.visibility = View.GONE
        filesListView.setOnScrollListener(onScrollListener)
        mListMoreView = ListMoreView(filesListView)
        fileListAdapter!!.addFootView(mListMoreView.getLoadMoreView())

        return view
    }

    private fun reset() {
        curPage = 0
    }

    override fun loadData() {
        reset()
        getFavorities()
    }

    private fun getFavorities() {
        mListMoreView.onLoadingStateChanged(IMoreView.STATE_LOADING)
        lifecycleScope.launch {
            var totalCount = 0
            val entryList = withContext(Dispatchers.IO) {
                val recent = RecentManager.instance
                totalCount = recent.favoriteProgressCount
                val progresses = recent.readFavoriteFromDb(PAGE_SIZE * (curPage), PAGE_SIZE)
                val entryList = ArrayList<FileBean>()

                var entry: FileBean
                var file: File
                val path = Environment.getExternalStorageDirectory().path
                progresses?.map {
                    file = File(path + "/" + it.path)
                    entry = FileBean(FileBean.FAVORITE, file, showExtension)
                    entry.bookProgress = it
                    entryList.add(entry)
                }
                return@withContext entryList
            }
            mSwipeRefreshWidget.isRefreshing = false
            if (entryList.size > 0) {
                if (curPage == 0) {
                    fileListAdapter!!.setData(entryList)
                    fileListAdapter!!.notifyDataSetChanged()
                } else {
                    val index = fileListAdapter!!.itemCount
                    fileListAdapter!!.addData(entryList)
                    fileListAdapter!!.notifyItemRangeInserted(index, entryList.size)
                }

                curPage++
            }
            updateLoadingStatus(totalCount)
        }
    }

    private fun updateLoadingStatus(totalCount: Int) {
        Logcat.d(String.format("$this, total count:%s, adapter count:%s", totalCount, fileListAdapter!!.normalCount))
        if (fileListAdapter!!.normalCount > 0) {
            if (fileListAdapter!!.normalCount < totalCount) {
                mListMoreView.onLoadingStateChanged(IMoreView.STATE_NORMAL)
            } else {
                Logcat.d("fileListAdapter!!.normalCount < totalCount")
                mListMoreView.onLoadingStateChanged(IMoreView.STATE_NO_MORE)
            }
        } else {
            Logcat.d("fileListAdapter!!.normalCount <= 0")
            mListMoreView.onLoadingStateChanged(IMoreView.STATE_NO_MORE)
        }
    }

    private val onScrollListener: RecyclerView.OnScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                if (mListMoreView.state == IMoreView.STATE_NORMAL
                        || mListMoreView.state == IMoreView.STATE_LOAD_FAIL) {
                    var isReachBottom = false
                    if (mStyle == HistoryFragment.STYLE_GRID) {
                        val gridLayoutManager = filesListView.layoutManager as GridLayoutManager
                        val rowCount = fileListAdapter!!.getItemCount() / gridLayoutManager.spanCount
                        val lastVisibleRowPosition = gridLayoutManager.findLastVisibleItemPosition() / gridLayoutManager.spanCount
                        isReachBottom = lastVisibleRowPosition >= rowCount - 1
                    } else if (mStyle == HistoryFragment.STYLE_LIST) {
                        val layoutManager: LinearLayoutManager = filesListView.layoutManager as LinearLayoutManager
                        val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                        val rowCount = fileListAdapter!!.getItemCount()
                        isReachBottom = lastVisibleItemPosition >= rowCount - fileListAdapter!!.headersCount - fileListAdapter!!.footersCount
                    }
                    if (isReachBottom) {
                        mListMoreView.onLoadingStateChanged(IMoreView.STATE_LOADING)
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

        const val TAG = "FavoriteFragment"
        internal const val PAGE_SIZE = 21
    }

}

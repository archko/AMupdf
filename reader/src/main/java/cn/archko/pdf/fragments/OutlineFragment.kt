package cn.archko.pdf.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.AdapterView
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import cn.archko.pdf.R
import cn.archko.pdf.adapters.BaseViewHolder
import cn.archko.pdf.adapters.ListBaseAdapter
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.listeners.OutlineListener
import com.artifex.mupdf.viewer.OutlineActivity

/**
 * @author: archko 2019/7/11 :17:55
 */
open class OutlineFragment : Fragment() {

    private var adapter: ListBaseAdapter<OutlineActivity.Item>? = null
    lateinit var listView: ListView
    private var outline: ArrayList<OutlineActivity.Item>? = null
    var currentPage: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            currentPage = arguments!!.getInt("POSITION", 0)
            if (arguments!!.getSerializable("OUTLINE") != null) {
                outline = arguments!!.getSerializable("OUTLINE") as ArrayList<OutlineActivity.Item>
            }
        }

        if (null == outline) {
            outline = ArrayList()
        }
        adapter = object : ListBaseAdapter<OutlineActivity.Item>(activity, outline!!) {
            override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseViewHolder<*> {
                val view = mInflater.inflate(R.layout.item_outline, parent, false)
                return ViewHolder(view)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_outline, container, false)

        listView = view.findViewById(R.id.list)

        listView.adapter = adapter
        listView.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l -> onListItemClick(adapterView as ListView, view, i, l) }

        //activity?.setResult(-1)
        if (adapter!!.count > 0) {
            updateSelection(currentPage)
        }
        return view
    }

    open fun updateSelection(currentPage: Int) {
        if (currentPage < 0) {
            return
        }
        this.currentPage = currentPage;
        if (!isResumed || adapter == null) {
            return
        }
        var found = -1
        for (i in outline!!.indices) {
            val item = outline!![i]
            if (found < 0 && item.page >= currentPage) {
                found = i
            }
        }
        if (found >= 0) {
            val finalFound = found
            listView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    listView.viewTreeObserver.removeGlobalOnLayoutListener(this)
                    listView.setSelection(finalFound)
                }
            })
        }
    }

    protected fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        val item = adapter?.getItem(position) as OutlineActivity.Item
        val ac = activity as OutlineListener
        ac.onSelectedOutline(item.page)
    }

    inner class ViewHolder(itemView: View?) : BaseViewHolder<OutlineActivity.Item>(itemView) {

        var title: TextView = itemView!!.findViewById(R.id.title)
        var page: TextView = itemView!!.findViewById(R.id.page)

        override fun onBind(data: OutlineActivity.Item?, position: Int) {
            title.text = data?.title
            page.text = (data?.page?.plus(1)).toString()
        }
    }
}
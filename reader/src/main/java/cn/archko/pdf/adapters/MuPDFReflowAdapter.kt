package cn.archko.pdf.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.App
import cn.archko.pdf.common.ParseTextMain
import cn.archko.pdf.common.ReflowViewCache
import cn.archko.pdf.common.StyleHelper
import cn.archko.pdf.entity.ReflowBean
import cn.archko.pdf.utils.Utils
import com.artifex.mupdf.fitz.Document

/**
 * @author: archko 2016/5/13 :11:03
 */
class MuPDFReflowAdapter(private val mContext: Context,
                         private val mCore: Document?,
                         private var styleHelper: StyleHelper?)
    : BaseRecyclerAdapter<Any>(mContext) {

    private var screenHeight = 720
    private var screenWidth = 1080
    private val systemScale = Utils.getScale()
    private val reflowCache = ReflowViewCache()

    init {
        screenHeight = App.getInstance().screenHeight
        screenWidth = App.getInstance().screenWidth
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        return mCore?.countPages()!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<*> {
        val pdfView: ReflowTextViewHolder.PDFTextView = ReflowTextViewHolder.PDFTextView(mContext, styleHelper)
        val holder = ReflowTextViewHolder(pdfView)
        val lp: RecyclerView.LayoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        pdfView.layoutParams = lp

        return holder
    }

    override fun onBindViewHolder(holder: BaseViewHolder<Any>, pos: Int) {
        /*val result = mCore?.loadPage(pos)?.textAsText("preserve-whitespace,inhibit-spaces,preserve-images")

        (holder as ReflowTextViewHolder).bindAsList(result, screenHeight, screenWidth, systemScale)*/

        @SuppressLint("StaticFieldLeak")
        val task = object : AsyncTask<Void, Void, List<ReflowBean>?>() {
            override fun doInBackground(vararg arg0: Void): List<ReflowBean>? {
                try {
                    val result = mCore?.loadPage(pos)?.textAsText("preserve-whitespace,inhibit-spaces,preserve-images")
                    val list = result?.let { ParseTextMain.instance.parseAsList(it, pos) }
                    return list
                } catch (e: Exception) {
                }
                return null
            }

            override fun onPostExecute(result: List<ReflowBean>?) {
                if (null != result) {
                    (holder as ReflowTextViewHolder).bindAsList(result, screenHeight, screenWidth, systemScale, reflowCache)
                }
            }
        }

        Utils.execute(true, task)
    }

    fun clearCacheViews() {
        reflowCache.clear()
    }

    companion object {

        const val TYPE_TEXT = 0
    }

}

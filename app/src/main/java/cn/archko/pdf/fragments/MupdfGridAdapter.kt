package cn.archko.pdf.fragments

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.awidget.ARecyclerView
import androidx.recyclerview.awidget.GridLayoutManager
import cn.archko.pdf.AppExecutors
import cn.archko.pdf.common.BitmapCache
import cn.archko.pdf.common.BitmapPool
import cn.archko.pdf.entity.APage
import cn.archko.pdf.listeners.ClickListener
import cn.archko.pdf.mupdf.MupdfDocument
import org.vudroid.R
import java.lang.ref.WeakReference

/**
 * @author: archko 2023/3/8 :14:34
 */
class MupdfGridAdapter(
    var mupdfListener: MupdfListener,
    var context: Context,
    var recyclerView: ARecyclerView,
    var clickListener: ClickListener<View>
) :
    ARecyclerView.Adapter<ARecyclerView.ViewHolder>() {

    private var pos: Int = 0

    override fun getItemCount(): Int {
        return mupdfListener.getPageCount()
    }

    fun viewWidth(): Int {
        val lm = recyclerView.layoutManager
        if (lm is GridLayoutManager) {
            return recyclerView.width / lm.spanCount
        }
        return recyclerView.width
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ARecyclerView.ViewHolder {
        var aPage: APage? = null
        if (mupdfListener.getPageList().size > pos) {
            aPage = mupdfListener.getPageList()[pos]
        }

        val view = ImageView(context)
        val pdfHolder = PdfHolder(view)
        if (null == aPage) {
            return pdfHolder
        }

        val width: Int = viewWidth()
        val height: Int = (width * aPage.height / aPage.width).toInt()
        Log.d(
            "TAG",
            String.format(
                "create measuredWidth:%s,pageWidth:%s, pageHeight:%s",
                width,
                aPage.width,
                aPage.height
            )
        )

        view.adjustViewBounds = true
        view.scaleType = ImageView.ScaleType.CENTER_CROP
        var lp = view.layoutParams as ARecyclerView.LayoutParams?
        if (null == lp) {
            lp = ARecyclerView.LayoutParams(width, height)
            view.layoutParams = lp
        } else {
            lp.width = width
            lp.height = height
        }
        return pdfHolder
    }

    override fun onBindViewHolder(viewHolder: ARecyclerView.ViewHolder, position: Int) {
        pos = viewHolder.bindingAdapterPosition
        val pdfHolder = viewHolder as PdfHolder

        pdfHolder.onBind(pos)
    }

    override fun onViewRecycled(holder: ARecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        val pos = holder.absoluteAdapterPosition
        Log.d("TAG", String.format("decode.onViewRecycled:%s", pos))
        val task = decodeMap.remove(pos)
        task?.run {
            isCancelled = true
            //val bitmap = BitmapCache.getInstance().removeBitmap(key)
            //if (null != bitmap) {
            //    BitmapPool.getInstance().release(bitmap)
            //}
        }
        (holder as PdfHolder).view.setImageResource(android.R.color.transparent)
    }

    inner class PdfHolder(internal var view: ImageView) : ARecyclerView.ViewHolder(view) {
        fun onBind(position: Int) {
            val aPage = mupdfListener.getPageList()[position]
            val screenWidth = viewWidth()

            val key =
                "${mupdfListener.getDocument()!!}_page_$position-${aPage}"
            Log.d(
                "TAG",
                String.format(
                    "bind:position:%s,targetWidth:%s, view.w:%s-h:%s, page.w::%s-h:%s, key:%s",
                    position,
                    screenWidth,
                    recyclerView.measuredWidth,
                    recyclerView.measuredHeight,
                    aPage.width,
                    aPage.height,
                    key
                )
            )
            if (screenWidth <= 0) {
                return
            }

            val width: Int = viewWidth()
            val height: Int = (width * 4 / 3f).toInt()

            view.setOnClickListener { clickListener.click(view, position) }
            view.setOnLongClickListener {
                clickListener.longClick(it, position, it)
                //showPopupMenu(it, position)
                return@setOnLongClickListener true
            }

            val bitmap = BitmapCache.getInstance().getBitmap(key)
            if (null != bitmap) {
                Log.d("TAG", String.format("bind.hit cache:%s", aPage.index))
                view.setImageBitmap(bitmap)
                return
            }

            val task = DecodeTask(width, height, aPage, view, key)
            decodeMap[pos] = task
            view.setImageResource(android.R.color.transparent)
            view.tag = pos
            task.run()
        }
    }

    val decodeMap = mutableMapOf<Int, DecodeTask>()

    inner class DecodeTask(
        var width: Int,
        var height: Int,
        var aPage: APage,
        view: ImageView,
        var key: String
    ) {
        var viewRef: WeakReference<ImageView>?
        var isCancelled: Boolean = false

        init {
            viewRef = WeakReference(view)
        }

        fun isCancellOrNull(): Boolean {
            if (isCancelled) {
                return true
            }
            if (null == viewRef || null == viewRef!!.get()) {
                return true
            }
            return false
        }

        fun run() {
            AppExecutors.instance.diskIO().execute {
                decode(aPage, this)
            }
        }
    }

    fun decode(aPage: APage, decodeTask: DecodeTask) {
        if (decodeTask.isCancellOrNull()) {
            Log.d("TAG", String.format("decode.cancel:%s", aPage.index))
            return
        }

        var rect = Rect(0, 0, decodeTask.width, decodeTask.height)
        var scale = 1.0f
        var leftBound = 0
        var topBound = 0
        val width = rect.width()
        val height = rect.height()
        leftBound = rect.left
        topBound = rect.top
        /*Log.d(
            "TAG",
            String.format(
                "decode:position:%s, view.w:%s-h:%s, page.w::%s-h:%s",
                aPage.index,
                decodeTask.width,
                decodeTask.height,
                aPage.width,
                aPage.height
            )
        )*/

        val bitmap = BitmapPool.getInstance().acquire(width, height)
        val page = mupdfListener.getDocument()!!.loadPage(aPage.index)
        val ctm = com.artifex.mupdf.fitz.Matrix(scale)
        if (decodeTask.isCancellOrNull()) {
            Log.d("TAG", String.format("decode.cancel before run:%s", aPage.index))
            return
        }

        MupdfDocument.render(page, ctm, bitmap, 0, leftBound, topBound)
        page?.destroy()
        BitmapCache.getInstance().addBitmap(decodeTask.key, bitmap)

        if (decodeTask.isCancellOrNull()) {
            Log.d("TAG", String.format("decode.cancel after run:%s", aPage.index))
            return
        }
        AppExecutors.instance.mainThread().execute {
            if (null == decodeTask.viewRef || decodeTask.viewRef!!.get() == null) {
                return@execute
            }
            val view = decodeTask.viewRef!!.get()
            val pos = view!!.tag
            if (pos == aPage.index) {
                view.setImageBitmap(bitmap)
            }
        }
    }
}

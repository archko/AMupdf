package cn.archko.pdf.entity

import android.graphics.Bitmap
import android.graphics.RectF
import cn.archko.pdf.common.BitmapPool
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.listeners.DecodeCallback
import cn.archko.pdf.mupdf.MupdfDocument
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.Page
import com.artifex.mupdf.fitz.RectI

/**
 * @author: archko 2024/3/13 :16:55
 */
class DecodeTask(
    var width: Int,
    var height: Int,
    var index: Int,
    val aPage: APage,
    val crop: Boolean = false,
    var key: String,
    var callback: DecodeCallback,
    var mupdfDocument: MupdfDocument?,
) {
    fun run() {
        val bitmap = decode(this)
        if (null != bitmap) {
            callback.decodeComplete(bitmap, index, key)
        }
    }

    fun decode(task: DecodeTask): Bitmap? {
        //long start = SystemClock.uptimeMillis();
        val page: Page? = mupdfDocument?.loadPage(aPage.index)

        var leftBound = 0
        var topBound = 0
        var pageW = width
        var pageH = height

        val ctm = Matrix(MupdfDocument.ZOOM)
        val bbox = RectI(page?.bounds?.transform(ctm))
        val xscale = pageW.toFloat() / (bbox.x1 - bbox.x0).toFloat()
        val yscale = pageH.toFloat() / (bbox.y1 - bbox.y0).toFloat()
        ctm.scale(xscale, yscale)

        /*if (task.crop) {
            Logcat.d(
                "DecodeTask", String.format(
                    "decode crop:%s, %s-%s, task:%s-%s, bound(left-top):%s-%s, page:%s",
                    task.index, pageW, pageH, task.width, task.height,
                    leftBound, topBound, aPage
                )
            )
            if (pageW >= 40 && pageH >= 40) {
                //if (pageSize.cropBounds != null) {
                //    leftBound = pageSize.cropBounds?.left?.toInt()!!
                //    topBound = pageSize.cropBounds?.top?.toInt()!!
                //    pageH = pageSize.cropBounds?.height()?.toInt()!!
                //} else {
                val arr = MupdfDocument.getArrByCrop(page, ctm, pageW, pageH, leftBound, topBound)
                leftBound = arr[0].toInt()
                topBound = arr[1].toInt()
                pageH = arr[2].toInt()
                val cropScale = arr[3]
                aPage.setCropHeight(pageH)
                aPage.setCropWidth(pageW)
                val cropRectf = RectF(
                    leftBound.toFloat(), topBound.toFloat(),
                    (leftBound + pageW).toFloat(), (topBound + pageH).toFloat()
                );
                aPage.setCropBounds(cropRectf, cropScale)
                //}
            }
        }*/

        val recycle = task.callback.shouldRender(task.index, null)
        if (recycle) {
            if (Logcat.loggable) {
                Logcat.d(
                    "DecodeTask", String.format(
                        "decode task:isRecycled:%s, %s-%s, task:%s-%s, bound(left-top):%s-%s, page:%s",
                        task.index, pageW, pageH, task.width, task.height,
                        leftBound, topBound, aPage
                    )
                )
                return null
            }
        }

        val bitmap = BitmapPool.getInstance().acquire(pageW, pageH)
        MupdfDocument.render(page, ctm, bitmap, 0, leftBound, topBound)
        page?.destroy()

        return bitmap
    }
}

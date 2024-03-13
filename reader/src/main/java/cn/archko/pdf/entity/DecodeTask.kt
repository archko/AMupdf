package cn.archko.pdf.entity

import android.graphics.Bitmap
import android.graphics.RectF
import cn.archko.pdf.common.BitmapPool
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.listeners.DecodeCallback
import cn.archko.pdf.mupdf.MupdfDocument
import cn.archko.pdf.widgets.APDFView
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.Page
import com.artifex.mupdf.fitz.RectI

/**
 * @author: archko 2024/3/13 :16:55
 */
class DecodeTask(
    var width: Int,
    var height: Int,
    var orientation: Int,
    var index: Int,
    val aPage: APage,
    val crop: Boolean = false,
    //var key: String,
    var callback: DecodeCallback,
    var mupdfDocument: MupdfDocument?,
) {
    fun run() {
        val bitmap = decode()
        if (null != bitmap) {
            callback.decodeComplete(
                bitmap,
                index,
                APDFView.getCacheKey(
                    index,
                    bitmap.width,
                    bitmap.height,
                    aPage.cropWidth,
                    aPage.cropHeight,
                    crop
                )
            )
        }
    }

    private fun decode(): Bitmap? {
        //long start = SystemClock.uptimeMillis();
        val page: Page? = mupdfDocument?.loadPage(aPage.index)

        var recycle = callback.shouldRender(index, null)
        if (recycle) {
            if (Logcat.loggable) {
                Logcat.d(
                    "DecodeTask", String.format(
                        "decode task:isRecycled1:%s, bound(left-top):%s-%s",
                        index, page?.bounds?.x1, page?.bounds?.y1
                    )
                )
                return null
            }
        }
        
        page?.run {
            //如果没有对它赋值,外部的view的高宽与当前是不匹配的,会变形
            aPage.width = bounds.x1 - bounds.x0
            aPage.height = bounds.y1 - bounds.y0

            var leftBound = 0
            var topBound = 0
            var pageW = width
            var pageH = (width * aPage.height / aPage.width).toInt()

            val ctm = Matrix(MupdfDocument.ZOOM)
            val bbox = RectI(page.bounds?.transform(ctm))
            val xscale = pageW.toFloat() / (bbox.x1 - bbox.x0).toFloat()
            val yscale = pageH.toFloat() / (bbox.y1 - bbox.y0).toFloat()
            ctm.scale(xscale, yscale)

            if (crop) {
                if (pageW >= 40 && pageH >= 40) {
                    if (aPage.cropBounds != null) {
                        leftBound = aPage.cropBounds?.left?.toInt()!!
                        topBound = aPage.cropBounds?.top?.toInt()!!
                        pageH = aPage.cropBounds?.height()?.toInt()!!
                    } else {
                        val arr =
                            MupdfDocument.getArrByCrop(page, ctm, pageW, pageH, leftBound, topBound)
                        leftBound = arr[0].toInt()
                        topBound = arr[1].toInt()
                        pageH = arr[2].toInt()
                        val cropScale = arr[3]
                        aPage.cropWidth = (pageH)
                        aPage.cropHeight = (pageW)
                        val cropRectf = RectF(
                            leftBound.toFloat(), topBound.toFloat(),
                            (leftBound + pageW).toFloat(), (topBound + pageH).toFloat()
                        );
                        aPage.setCropBounds(cropRectf, cropScale)
                    }
                    Logcat.d(
                        "DecodeTask", String.format(
                            "decode crop:%s, %s-%s, task:%s-%s, bound(left-top):%s-%s, page:%s",
                            index, pageW, pageH, width, height,
                            leftBound, topBound, aPage
                        )
                    )
                }
            }

            var recycle = callback.shouldRender(index, null)
            if (recycle) {
                if (Logcat.loggable) {
                    Logcat.d(
                        "DecodeTask", String.format(
                            "decode task:isRecycled2:%s, %s-%s, task:%s-%s, bound(left-top):%s-%s, page:%s",
                            index, pageW, pageH, width, height,
                            leftBound, topBound, aPage
                        )
                    )
                    return null
                }
            }

            val bitmap = BitmapPool.getInstance().acquire(pageW, pageH)
            MupdfDocument.render(page, ctm, bitmap, 0, leftBound, topBound)
            page.destroy()

            return bitmap
        }
        return null
    }
}

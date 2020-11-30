package cn.archko.pdf.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.view.View
import android.widget.ImageView
import cn.archko.pdf.AppExecutors
import cn.archko.pdf.common.BitmapPool
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.entity.APage
import cn.archko.pdf.mupdf.MupdfDocument
import cn.archko.pdf.utils.Utils
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.Page
import com.artifex.mupdf.fitz.RectI

/**
 * @author: archko 2018/7/25 :12:43
 */
@SuppressLint("AppCompatCustomView")
public class APDFView(
    protected val mContext: Context,
    private val mupdfDocument: MupdfDocument?,
    private var aPage: APage,
    crop: Boolean,
) : ImageView(mContext) {

    private var mZoom: Float = 0.toFloat()
    private val mHandler: Handler = Handler()
    private val bitmapPaint = Paint()
    private val textPaint: Paint = textPaint()

    init {
        updateView()
    }

    private fun updateView() {
        scaleType = ImageView.ScaleType.MATRIX
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    private fun textPaint(): Paint {
        val paint = Paint()
        paint.color = Color.BLUE
        paint.isAntiAlias = true
        paint.textSize = Utils.sp2px(30f).toFloat()
        paint.textAlign = Paint.Align.CENTER
        return paint
    }

    fun recycle() {
        setImageBitmap(null)
        bitmap = null
        isRecycle = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var mwidth = aPage.getCropWidth()
        var mheight = aPage.getCropHeight()

        val d = drawable
        if (null != d) {
            val dwidth = d.intrinsicWidth
            val dheight = d.intrinsicHeight

            if (dwidth > 0 && dheight > 0) {
                mwidth = dwidth
                mheight = dheight
            }
        }

        setMeasuredDimension(mwidth, mheight)
        Logcat.d(
            String.format(
                "onMeasure,width:%s,height:%s, page:%s-%s, mZoom: %s, aPage:%s",
                mwidth, mheight, aPage.effectivePagesWidth, aPage.effectivePagesHeight, mZoom, aPage
            )
        )
    }

    /*override fun onDraw(canvas: Canvas) {
        canvas.drawText(
            String.format("Page %s", aPage.index + 1), (measuredWidth / 2).toFloat(),
            (measuredHeight / 2).toFloat(), textPaint
        )
        super.onDraw(canvas)
    }*/

    fun updatePage(pageSize: APage, newZoom: Float, crop: Boolean) {
        isRecycle = false
        val oldZoom = aPage.scaleZoom
        aPage = pageSize
        aPage.zoom = newZoom

        Logcat.d(
            String.format(
                "updatePage, oldZoom:%s, newScaleZoom:%s,newZoom:%s,",
                oldZoom, aPage.scaleZoom, newZoom
            )
        )

        if (null != bitmap) {
            setImageBitmap(bitmap)
            return
        }

        decodeBitmap(crop)
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        bitmap = bm
    }

    // =================== decode ===================
    private var bitmap: Bitmap? = null
    private var isRecycle = false
    private var crop: Boolean = false

    private fun decodeBitmap(crop: Boolean) {
        AppExecutors.instance.diskIO().execute(Runnable { doDecode(crop) })
    }

    private fun doDecode(crop: Boolean) {
        this.crop = crop
        val bm: Bitmap? = decode()
        if (bm != null && !isRecycle) {
            mHandler.post { setImageBitmap(bm) }
        }
    }

    fun decode(): Bitmap? {
        //long start = SystemClock.uptimeMillis();
        val page: Page? = mupdfDocument?.loadPage(aPage.index)

        var leftBound = 0
        var topBound = 0
        val pageSize: APage = aPage
        var pageW = pageSize.zoomPoint.x
        var pageH = pageSize.zoomPoint.y

        val ctm = Matrix(MupdfDocument.ZOOM)
        val bbox = RectI(page?.bounds?.transform(ctm))
        val xscale = pageW.toFloat() / (bbox.x1 - bbox.x0).toFloat()
        val yscale = pageH.toFloat() / (bbox.y1 - bbox.y0).toFloat()
        ctm.scale(xscale, yscale)

        if (pageSize.getTargetWidth() > 0) {
            pageW = pageSize.getTargetWidth()
        }

        if (crop) {
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
            pageSize.setCropHeight(pageH)
            pageSize.setCropWidth(pageW)
            val cropRectf = RectF(
                leftBound.toFloat(), topBound.toFloat(),
                (leftBound + pageW).toFloat(), (topBound + pageH).toFloat()
            );
            pageSize.setCropBounds(cropRectf, cropScale)
            //}
        }

        if (Logcat.loggable) {
            Logcat.d(
                TAG, String.format(
                    "decode bitmap:isRecycle:%s, %s-%s,page:%s-%s, bound(left-top):%s-%s, page:%s",
                    isRecycle, pageW, pageH, pageSize.zoomPoint.x, pageSize.zoomPoint.y,
                    leftBound, topBound, pageSize
                )
            )
        }

        if (isRecycle) {
            return null
        }

        val bitmap = BitmapPool.getInstance().acquire(pageW, pageH)
        //Bitmap.createBitmap(sizeX, sizeY, Bitmap.Config.ARGB_8888);

        MupdfDocument.render(page, ctm, bitmap, 0, leftBound, topBound)

        page?.destroy()

        return bitmap
    }

    companion object {
        private val TAG: String = "APDFView"
    }
}

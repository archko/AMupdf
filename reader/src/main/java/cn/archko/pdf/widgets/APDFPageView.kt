package cn.archko.pdf.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.view.View
import android.widget.ImageView
import cn.archko.pdf.common.BitmapManager
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.entity.APage
import com.artifex.mupdf.fitz.Document

/**
 * @author: archko 2019/11/25 :12:43
 */
@SuppressLint("AppCompatCustomView")
class APDFPageView(protected val mContext: Context,
                   private val mCore: Document?,
                   private var pageSize: APage,
                   private val mBitmapManager: BitmapManager?) : ImageView(mContext) {

    private var mZoom: Float = 0.toFloat()
    private lateinit var pdfPage: APDFPage;

    init {
        mZoom = pageSize.getZoom()
        initPdfPage()
        updateView()
    }

    private fun initPdfPage() {
        pdfPage = APDFPage(this, pageSize, mCore);
        pdfPage.setBounds(RectF(0f, 0f, this.pageSize.effectivePagesWidth.toFloat(), this.pageSize.effectivePagesHeight.toFloat()))
    }

    fun updateView() {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    fun recycle() {
        setImageBitmap(null)
        pdfPage.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        //super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var x: Int
        var y: Int
        x = pageSize.realCropWidth
        y = pageSize.realCropHeight

        setMeasuredDimension(x, y)
        //Logcat.d(String.format("onMeasure,width:%s,height:%s, page:%s-%s, mZoom: %s, aPage:%s",
        //        x, y, aPage.effectivePagesWidth, aPage.effectivePagesHeight, mZoom, aPage));
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        pdfPage.draw(canvas)
    }

    fun setPage(pageSize: APage, newZoom: Float, autoCrop: Boolean) {
        var isNew = false
        if (this.pageSize != pageSize) {
            this.pageSize = pageSize
            isNew = true
            pdfPage.setBounds(RectF(0f, 0f, pageSize.effectivePagesWidth.toFloat(), pageSize.effectivePagesHeight.toFloat()))
            pdfPage.update(this, pageSize)
        }
        mZoom = newZoom
        this.pageSize.zoom = newZoom

        val zoomSize = this.pageSize.zoomPoint
        val xOrigin = (zoomSize.x - this.pageSize.targetWidth) / 2

        Logcat.d(String.format("setPage:isNew:%s,width-height:%s-%s, mZoom: %s, aPage:%s",
                isNew, pageSize.effectivePagesWidth, pageSize.effectivePagesHeight, mZoom, pageSize));
        pdfPage.updateVisibility()
    }
}

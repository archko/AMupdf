package cn.archko.pdf.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.view.View
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
                   crop: Boolean) : View(mContext) {

    private var mZoom: Float = 0.toFloat()
    private lateinit var pdfPage: APDFPage;

    init {
        mZoom = pageSize.getZoom()
        initPdfPage(crop)
        updateView()
    }

    private fun initPdfPage(crop: Boolean) {
        pdfPage = APDFPage(this, pageSize, mCore, crop);
        pdfPage.setBounds(RectF(0f, 0f, this.pageSize.effectivePagesWidth.toFloat(), this.pageSize.effectivePagesHeight.toFloat()))
    }

    fun updateView() {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    fun recycle() {
        pdfPage.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        //super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var width: Int
        var height: Int
        val wmode = MeasureSpec.getMode(widthMeasureSpec)
        val hmode = MeasureSpec.getMode(heightMeasureSpec)
        if (wmode == MeasureSpec.UNSPECIFIED) {
            width = pageSize.realCropWidth
        } else {
            width = MeasureSpec.getSize(widthMeasureSpec)
        }
        if (hmode == MeasureSpec.UNSPECIFIED) {
            height = pageSize.realCropHeight
        } else {
            height = MeasureSpec.getSize(heightMeasureSpec)
        }

        setMeasuredDimension(width, height)
        Logcat.d(String.format("onMeasure,width:%s,height:%s, page:%s-%s, mZoom: %s, aPage:%s",
                width, height, pageSize.effectivePagesWidth, pageSize.effectivePagesHeight, mZoom, pageSize));
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        pdfPage.draw(canvas)
    }

    fun updatePage(pageSize: APage, newZoom: Float, crop: Boolean) {
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
        pdfPage.updateVisibility(crop, xOrigin)
    }
}

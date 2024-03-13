package cn.archko.pdf.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.awidget.LinearLayoutManager
import cn.archko.pdf.AppExecutors
import cn.archko.pdf.common.BitmapCache
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.entity.APage
import cn.archko.pdf.entity.DecodeTask
import cn.archko.pdf.listeners.DecodeCallback
import cn.archko.pdf.utils.Utils
import cn.archko.pdf.viewmodel.PDFViewModel

/**
 * @author: archko 2018/7/25 :12:43
 */
@SuppressLint("AppCompatCustomView")
public class APDFView(
    mContext: Context,
) : ImageView(mContext), DecodeCallback {

    private val textPaint: Paint = textPaint()
    private var resultWidth: Int = 1080
    private var resultHeight: Int = 1080

    private var aPage: APage? = null
    private var pageIndex = -1

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
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (aPage != null && null == drawable) {
            canvas.drawText(
                String.format("Page %s", aPage!!.index + 1), (measuredWidth / 2).toFloat(),
                (measuredHeight / 2).toFloat(), textPaint
            )
        }
    }

    private fun getCacheKey(index: Int, w: Int, h: Int, crop: Boolean): String {
        return String.format("%s-%s-%s-%s", index, w, h, crop)
    }

    fun updatePage(
        pageSize: APage,
        position: Int,
        orientation: Int,
        vwidth: Int,
        vheight: Int,
        crop: Boolean,
        pdfViewModel: PDFViewModel,
    ) {
        aPage = pageSize
        pageIndex = position

        resultWidth = vwidth
        caculateWidth(orientation, crop)
        Logcat.d(
            String.format(
                "updatePage.page:%s, size.w-h:%s-%s",
                pageSize.index,
                resultWidth,
                resultHeight
            )
        )

        val cacheKey = getCacheKey(aPage!!.index, resultWidth, resultHeight, crop)
        val bmp = BitmapCache.getInstance().getBitmap(cacheKey)

        if (null != bmp) {
            setImageBitmap(bmp)
            setLayoutSize()
            return
        }
        val task =
            DecodeTask(
                resultWidth, resultHeight,
                position, aPage!!,
                crop, cacheKey,
                this,
                pdfViewModel.mupdfDocument
            )

        setImageDrawable(null)
        AppExecutors.instance.diskIO().execute { task.run() }
    }

    private fun setLayoutSize() {
        var lp = layoutParams as ViewGroup.LayoutParams?
        if (null == lp) {
            lp = ViewGroup.LayoutParams(width, height)
            layoutParams = lp
        } else {
            lp.width = resultWidth
            lp.height = resultHeight
        }
    }

    private fun caculateWidth(orientation: Int, crop: Boolean) {
        if (orientation == LinearLayoutManager.VERTICAL) {//垂直方向,以宽为准
            resultHeight = if (crop && aPage!!.cropBounds != null) {
                (resultWidth * aPage!!.cropBounds!!.height() / aPage!!.width).toInt()
            } else {
                (resultWidth * aPage!!.height / aPage!!.width).toInt()
            }
        } else {    //水平滚动,以高为准
            resultHeight = resultWidth
            resultWidth = if (crop && aPage!!.cropBounds != null) {
                (resultHeight * aPage!!.cropBounds!!.width() / aPage!!.height).toInt()
            } else {
                (resultHeight * aPage!!.width / aPage!!.height).toInt()
            }
        }
    }

    override fun decodeComplete(bitmap: Bitmap?, position: Int, key: String) {
        if (null != bitmap) {
            BitmapCache.getInstance().addBitmap(key, bitmap)
            if (Logcat.loggable) {
                Logcat.d(
                    TAG, String.format(
                        "decode complete:index:%s,pageIndex:%s, %s, %s-%s",
                        position, pageIndex, key, bitmap.width, bitmap.height,
                    )
                )
            }
        }
        if (position == pageIndex) {
            AppExecutors.instance.mainThread().execute {
                setImageBitmap(bitmap)
                setLayoutSize()
            }
        }
    }

    override fun shouldRender(index: Int, key: String?): Boolean {
        return pageIndex != index
    }

    companion object {
        private val TAG: String = "APDFView"
    }
}

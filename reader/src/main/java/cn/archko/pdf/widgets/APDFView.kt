package cn.archko.pdf.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.ImageView
import cn.archko.pdf.common.BitmapCache
import cn.archko.pdf.common.ImageDecoder
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.entity.APage
import com.artifex.mupdf.fitz.Document

/**
 * @author: archko 2018/7/25 :12:43
 */
@SuppressLint("AppCompatCustomView")
public class APDFView(protected val mContext: Context,
                      private val mCore: Document?,
                      private var aPage: APage?,
                      crop: Boolean) : ImageView(mContext) {

    private var mZoom: Float = 0.toFloat()

    init {
        updateView()
    }

    private fun updateView() {
        scaleType = ImageView.ScaleType.MATRIX
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    fun recycle() {
        setImageBitmap(null)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var mwidth = aPage!!.cropWidth
        var mheight = aPage!!.cropHeight

        val d = drawable
        if (null != d) {
            val dwidth = d.intrinsicWidth
            val dheight = d.intrinsicHeight

            if (dwidth > 0 && dheight > 0) {
                mwidth = dwidth
                mheight = dheight

                if (mZoom > 1.0f) {
                    val sx = (mwidth * mZoom).toInt()
                    val sy = (mheight * mZoom).toInt()
                    val dx = sx - mwidth;
                    val dy = sy - mheight;
                    mwidth = sx
                    mheight = sy
                    imageMatrix.reset()
                    imageMatrix.setScale(mZoom, mZoom)
                    imageMatrix.postTranslate((-dx).toFloat(), (-dy).toFloat())
                }
            }
        }

        setMeasuredDimension(mwidth, mheight)
        //Logcat.d(String.format("onMeasure,width:%s,height:%s, page:%s-%s, mZoom: %s, aPage:%s",
        //        x, y, aPage!!.effectivePagesWidth, aPage!!.effectivePagesHeight, mZoom, aPage));
    }

    fun updatePage(pageSize: APage, newZoom: Float, crop: Boolean) {
        val oldZoom = aPage!!.scaleZoom
        var changeScale = false
        if (mZoom != newZoom) {
            changeScale = true
        } else {
            changeScale = aPage !== pageSize
        }
        aPage = pageSize
        aPage!!.zoom = newZoom

        val zoomSize = aPage!!.zoomPoint
        val xOrigin = (zoomSize.x - aPage!!.targetWidth) / 2
        Logcat.d(String.format("updatePage xOrigin: %s,changeScale:%s, oldZoom:%s, newScaleZoom:%s,newZoom:%s,",
                xOrigin, changeScale, oldZoom, aPage!!.scaleZoom, newZoom));

        var bmp = BitmapCache.getInstance().getBitmap(ImageDecoder.getCacheKey(aPage!!.index, crop, aPage!!.scaleZoom))

        if (null != bmp) {
            setImageBitmap(bmp)
            imageMatrix.reset()
            if (!changeScale) {
                return
            }
        }

        if (changeScale && bmp == null) {
            bmp = BitmapCache.getInstance().getBitmap(ImageDecoder.getCacheKey(aPage!!.index, crop, oldZoom))
            //if (Logcat.loggable) {
            //    Logcat.d(String.format("updatePage xOrigin: %s, oldZoom:%s, newZoom:%s, bmp:%s",
            //            xOrigin, oldZoom, newZoom, bmp));
            //}
            if (null != bmp) {
                setImageBitmap(bmp)
                imageMatrix.reset()
                imageMatrix.setScale(newZoom, newZoom)
                imageMatrix.postTranslate((-xOrigin).toFloat(), 0f)
            }
        }

        ImageDecoder.getInstance().loadImage(aPage, crop, xOrigin, this, mCore) { bitmap ->
            //if (Logcat.loggable) {
            //    Logcat.d(String.format("decode2 relayout bitmap:index:%s, %s:%s imageView->%s:%s",
            //            pageSize.index, bitmap.width, bitmap.height,
            //            getWidth(), getHeight()))
            //}
            setImageBitmap(bitmap)
            imageMatrix.reset()
        }
    }

    /*@SuppressLint("StaticFieldLeak")
    protected fun getDrawPageTask(autoCrop: Boolean, pageSize: APage,
                                  xOrigin: Int, viewHeight: Int): AsyncTask<Void, Void, Bitmap> {
        return object : AsyncTask<Void, Void, Bitmap>() {

            public override fun onPreExecute() {
                drawText = aPage!!.index.toString()
                showPaint = true
            }

            override fun doInBackground(vararg params: Void): Bitmap? {
                if (isCancelled) {
                    return null
                }
                //long start = SystemClock.uptimeMillis();
                val page = mCore?.loadPage(aPage!!.index)

                var scale = 1.0f
                var leftBound = 0
                var topBound = 0
                var height = pageSize.zoomPoint.y
                if (autoCrop) {
                    val ratio = 6
                    val thumbPoint = aPage!!.getZoomPoint(aPage!!.scaleZoom / ratio)
                    val thumb = BitmapPool.getInstance().acquire(thumbPoint.x, thumbPoint.y)
                    val ctm = Matrix(aPage!!.scaleZoom / ratio)
                    ImageWorker.render(page, ctm, thumb, 0, leftBound, topBound)

                    val rectF = ImageDecoder.getCropRect(thumb)
                    //BitmapUtils.saveBitmapToFile(thumb, File(FileUtils.getStoragePath(thumb.toString()+".png")))

                    scale = thumb.width / rectF.width()
                    BitmapPool.getInstance().release(thumb)

                    leftBound = (rectF.left * ratio * scale).toInt()
                    topBound = (rectF.top * ratio * scale).toInt()

                    height = (rectF.height() * ratio * scale).toInt()
                    if (Logcat.loggable) {
                        Logcat.d(String.format("decode scale:%s,height:%s, thumb:%s,%s,rect:%s, x:%s,y:%s",
                                scale, height, thumb.width, thumb.height, rectF, pageSize.zoomPoint.x, pageSize.zoomPoint.y))
                    }
                }
                if (isCancelled) {
                    return null
                }

                var width = pageSize.zoomPoint.x
                if (pageSize.targetWidth > 0) {
                    width = pageSize.targetWidth
                }

                if (Logcat.loggable) {
                    Logcat.d(String.format("decode bitmap:width-height:%s-%s,pagesize:%s,%s,%s, bound:%s,%s, page:%s",
                            width, height, pageSize.zoomPoint.y, xOrigin, pageSize.targetWidth, leftBound, topBound, pageSize))
                }

                val bitmap = BitmapPool.getInstance().acquire(width, height)//Bitmap.createBitmap(sizeX, sizeY, Bitmap.Config.ARGB_8888);

                val ctm = Matrix(pageSize.scaleZoom * scale)
                ImageWorker.render(page, ctm, bitmap, xOrigin, leftBound, topBound)

                //BitmapUtils.saveBitmapToFile(bitmap, File(FileUtils.getStoragePath(bitmap.toString()+".png")))
                page?.destroy()
                //Logcat.d("decode:" + (SystemClock.uptimeMillis() - start));
                return bitmap
            }

            override fun onPostExecute(bitmap: Bitmap?) {
                if (isCancelled || null == bitmap) {
                    Logcat.w("decode", "cancel decode.")
                    return
                }
                showPaint = false
                mBitmap = bitmap
                mBitmapManager?.setBitmap(aPage!!.index, bitmap)
                mEntireView!!.setImageBitmap(bitmap)
                mEntireView!!.imageMatrix.reset()

                if (height != bitmap.height || width != bitmap.width) {
                    if (Logcat.loggable) {
                        Logcat.d(String.format("decode relayout bitmap:index:%s, %s:%s imageView->%s:%s view->%s:%s",
                                pageSize.index, bitmap.width, bitmap.height,
                                mEntireView!!.getWidth(), mEntireView!!.getHeight(),
                                (mEntireView!!.getParent() as View).width, (mEntireView!!.getParent() as View).height))
                    }
                    layoutParams.height = bitmap.height
                    layoutParams.width = bitmap.width
                    requestLayout()
                }
            }
        }
    }*/

}

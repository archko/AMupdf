package cn.archko.pdf.widgets

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import cn.archko.pdf.common.BitmapManager
import cn.archko.pdf.common.ImageDecoder
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.entity.APage
import com.artifex.mupdf.fitz.Document

/**
 * @author: archko 2018/7/25 :12:43
 */
class APDFView(protected val mContext: Context,
               private val mCore: Document?,
               private var aPage: APage?,
               private val mBitmapManager: BitmapManager?) : ImageView(mContext) {

    private var mZoom: Float = 0.toFloat()

    init {
        mZoom = aPage!!.getZoom()

        updateView()
    }

    fun updateView() {
        scaleType = ImageView.ScaleType.MATRIX
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    fun recycle() {
        setImageBitmap(null)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        //super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var x: Int
        var y: Int
        x = aPage!!.effectivePagesWidth
        y = aPage!!.effectivePagesHeight
        val d = drawable;
        if (d != null && (d.intrinsicWidth > 0 && d.intrinsicHeight > 0)) {
            x = d.intrinsicWidth
            y = d.intrinsicHeight
        }

        setMeasuredDimension(x, y)
        //Logcat.d(String.format("onMeasure,width:%s,height:%s, page:%s-%s, mZoom: %s, aPage:%s",
        //        x, y, aPage!!.effectivePagesWidth, aPage!!.effectivePagesHeight, mZoom, aPage));
    }

    fun setPage(pageSize: APage, newZoom: Float, autoCrop: Boolean) {
        var changeScale = false
        if (mZoom != newZoom) {
            changeScale = true
        } else {
            changeScale = aPage !== pageSize
        }
        aPage = pageSize
        mZoom = newZoom
        aPage!!.zoom = newZoom

        //when scale, mBitmap is always null.because after adapter notify,releaseResources() is invoke.
        val zoomSize = aPage!!.zoomPoint
        val xOrigin = (zoomSize.x - aPage!!.targetWidth) / 2
        //Logcat.d(String.format("xOrigin: %s,changeScale:%s, aPage:%s", xOrigin, changeScale, aPage));

        val mBitmap = mBitmapManager?.getBitmap(aPage!!.index)

        if (null != mBitmap) {
            //if (Logcat.loggable) {
            //    Logcat.d(String.format("decode relayout bitmap:index:%s, %s:%s imageView->%s:%s",
            //            pageSize.index, mBitmap.width, mBitmap.height,
            //            getWidth(), getHeight()))
            //}
            setImageBitmap(mBitmap)
            /*if (Logcat.loggable) {
                Logcat.d(String.format("changeScale: %s,cache:%s, aPage:%s", changeScale, mBitmap.toString(), aPage));
            }
            if (changeScale) {
                val matrix = android.graphics.Matrix()
                matrix.postScale(newZoom, newZoom)
                matrix.postTranslate((-xOrigin).toFloat(), 0f)
                imageMatrix = matrix
                requestLayout()
            } else {

            }*/
        }

        //mDrawTask = getDrawPageTask(autoCrop, aPage!!, xOrigin, height)
        //Utils.execute(true, mDrawTask)
        ImageDecoder.getInstance().loadImage(aPage, autoCrop, xOrigin, this, mCore) { bitmap ->
            if (Logcat.loggable) {
                Logcat.d(String.format("decode2 relayout bitmap:index:%s, %s:%s imageView->%s:%s",
                        pageSize.index, bitmap.width, bitmap.height,
                        getWidth(), getHeight()))
            }
            setImageBitmap(bitmap)
            //imageMatrix.reset()
        }
    }

    private fun relayoutIfNeeded(bitmap: Bitmap, pageSize: APage) {
        if (height != bitmap.height || width != bitmap.width) {
            if (Logcat.loggable) {
                Logcat.d(String.format("decode relayout bitmap:index:%s, %s:%s imageView->%s:%s",
                        pageSize.index, bitmap.width, bitmap.height,
                        getWidth(), getHeight()))
            }
            layoutParams.height = bitmap.height
            layoutParams.width = bitmap.width
            requestLayout()
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

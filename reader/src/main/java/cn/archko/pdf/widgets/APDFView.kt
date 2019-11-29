package cn.archko.pdf.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.AsyncTask
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import cn.archko.pdf.common.BitmapManager
import cn.archko.pdf.common.ImageDecoder
import cn.archko.pdf.common.ImageWorker
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.entity.APage
import cn.archko.pdf.utils.Utils
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import org.vudroid.core.BitmapPool

/**
 * @author: archko 2018/7/25 :12:43
 */
class APDFView(protected val mContext: Context,
               private val mCore: Document?,
               private var aPage: APage?,
               private val mBitmapManager: BitmapManager?) : RelativeLayout(mContext) {

    private var mEntireView: AImageView? = null // Image rendered at minimum zoom
    private var mBitmap: Bitmap? = null
    private var mDrawTask: AsyncTask<Void, Void, Bitmap>? = null
    private var mZoom: Float = 0.toFloat()

    //page number
    private var showPaint = false
    private var drawText = ""
    //lateinit var mTextPaint: Paint
    //lateinit var mRectPaint: Paint
    //private var textWidth: Float = 100f
    //private var textHeight: Float = 40f

    //fun setShowPaint(showPaint: Boolean) {
    //    this.showPaint = showPaint
    //}

    //fun setDrawText(drawText: String) {
    //    this.drawText = drawText
    //}

    init {
        mZoom = aPage!!.getZoom()
        //mTextPaint = Paint()
        //mTextPaint.isAntiAlias = true
        //mTextPaint.color = resources.getColor(R.color.seek_thumb)
        //mTextPaint.textSize = Utils.sp2px(60f).toFloat()
        //mTextPaint.typeface = Typeface.DEFAULT_BOLD
        //textWidth = mTextPaint.measureText("100")
        //textHeight = mTextPaint.measureText("0")
        //mRectPaint = Paint()
        //mRectPaint.isAntiAlias = true
        //mRectPaint.color = resources.getColor(R.color.seek_thumb)
        //mRectPaint.strokeWidth = Utils.dipToPixel(2f).toFloat()
        //mRectPaint.style = Paint.Style.STROKE

        updateView()
    }

    //override fun dispatchDraw(canvas: Canvas?) {
    //    super.dispatchDraw(canvas)
    //    if (!TextUtils.isEmpty(drawText) && showPaint && height > 0) {
    //        val centerX: Float = ((getWidth() - 2 * width) / 2).toFloat()
    //        val centerY: Float = ((getHeight() - height) / 2).toFloat()
    //        canvas?.drawText(drawText, centerX, centerY, mTextPaint)
    //        val rect = RectF(centerX - width / 2, centerY - height, centerX + width * 1.5f, centerY + height / 2)
    //        canvas?.drawRect(rect, mRectPaint)
    //    }
    //}

    fun updateView() {
        mEntireView = AImageView(mContext)
        mEntireView!!.scaleType = ImageView.ScaleType.MATRIX
        mEntireView!!.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        lp.addRule(CENTER_IN_PARENT)
        addView(mEntireView, lp)
        drawText = (aPage!!.index.toString())
    }

    fun recycle() {
        if (mDrawTask != null) {
            mDrawTask!!.cancel(true)
            mDrawTask = null
        }

        if (null != mBitmap) {
            //BitmapPool.getInstance().release(mBitmap);
            mBitmap = null
        }
    }

    fun setPage(pageSize: APage, newZoom: Float, autoCrop: Boolean) {
        if (mDrawTask != null) {
            mDrawTask!!.cancel(true)
            mDrawTask = null
        }

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

        showPaint = true
        mEntireView!!.setSearchBoxes(emptyArray())
        mEntireView!!.setLinks(emptyArray())
        //mEntireView.setScale(aPage.getScaleZoom());

        val zoomSize = aPage!!.zoomPoint
        val xOrigin = (zoomSize.x - aPage!!.targetWidth) / 2
        Logcat.d(String.format("scale %s,%s", xOrigin, changeScale));

        if (null == mBitmap) {
            mBitmap = mBitmapManager?.getBitmap(aPage!!.index)
        }
        if (null != mBitmap) {
            if (mBitmap?.width != aPage?.targetWidth && !changeScale) {

            } else {
                showPaint = false
                mEntireView!!.setImageBitmap(mBitmap)
                //Logcat.d(String.format("scale cache %s,%s", mBitmap.toString(), changeScale));
                if (changeScale) {
                    val matrix = android.graphics.Matrix()
                    matrix.postScale(newZoom, newZoom)
                    matrix.postTranslate((-xOrigin).toFloat(), 0f)
                    mEntireView!!.imageMatrix = matrix
                    requestLayout()
                } else {
                    return
                }
            }
        }

        //mDrawTask = getDrawPageTask(autoCrop, aPage!!, xOrigin, height)
        //Utils.execute(true, mDrawTask)
        ImageDecoder.getInstance().loadImage(aPage, autoCrop, xOrigin, mEntireView, mCore)
    }

    @SuppressLint("StaticFieldLeak")
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
    }

}

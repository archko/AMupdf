package cn.archko.pdf.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.RectF;
import android.view.View;
import android.widget.ImageView;

import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Matrix;
import com.artifex.mupdf.fitz.Page;

import org.vudroid.core.BitmapPool;

import androidx.collection.LruCache;
import cn.archko.pdf.App;
import cn.archko.pdf.entity.APage;
import cn.archko.pdf.entity.BitmapBean;
import cn.archko.pdf.utils.Utils;
import cn.archko.pdf.widgets.AImageView;
import cn.archko.pdf.widgets.APDFView;

/**
 * @author: archko 2019/8/30 :16:17
 */
public class ImageDecoder extends ImageWorker {

    public static final String TAG = "ImageDecoder";
    private LruCache<String, Bitmap> mImageCache = new LruCache<>(4);
    private LruCache<String, APage> pageLruCache = new LruCache<>(32);
    private BitmapManager mBitmapManager;

    public static ImageDecoder getInstance() {
        return Factory.instance;
    }

    private static final class Factory {
        private static final ImageDecoder instance = new ImageDecoder(App.getInstance());
    }

    private ImageDecoder(final Context context) {
        super(context);
        mContext = context.getApplicationContext();
        mResources = mContext.getResources();
    }

    public void setBitmapManager(BitmapManager bitmapManager) {
        this.mBitmapManager = bitmapManager;
    }

    @Override
    public boolean isScrolling() {
        if (mImageCache != null) {
            //return mImageCache.isScrolling();
        }
        return false;
    }

    @Override
    public void addBitmapToCache(final String key, final Bitmap bitmap) {
        //if (mImageCache != null) {
        //    mImageCache.put(key, bitmap);
        //}
        if (null != mBitmapManager) {
            mBitmapManager.setBitmap(Utils.parseInt(key), bitmap);
        }
    }

    @Override
    public Bitmap getBitmapFromCache(final String key) {
        //if (mImageCache != null) {
        //    return mImageCache.get(key);
        //}
        if (null != mBitmapManager) {
            Bitmap bb = mBitmapManager.getBitmap(Utils.parseInt(key));
            if (bb != null) {
                return bb;
            }
        }
        return null;
    }

    @Override
    public LruCache<String, Bitmap> getImageCache() {
        return mImageCache;
    }

    @Override
    public LruCache<String, APage> getPageLruCache() {
        return pageLruCache;
    }

    public void loadImage(APage aPage, boolean autoCrop, int xOrigin,
                          ImageView imageView, Document document) {
        if (document == null || aPage == null || getImageCache() == null || imageView == null) {
            return;
        }
        super.loadImage(new DecodeParam(String.valueOf(aPage.index), imageView, autoCrop, xOrigin, aPage, document));
    }

    @Override
    protected Bitmap processBitmap(DecodeParam decodeParam) {
        try {
            //long start = SystemClock.uptimeMillis();
            Page page = decodeParam.document.loadPage(decodeParam.pageSize.index);

            int leftBound = 0;
            int topBound = 0;
            APage pageSize = decodeParam.pageSize;
            int height = pageSize.getZoomPoint().y;
            Matrix ctm = new Matrix(pageSize.getScaleZoom());

            if (decodeParam.autoCrop) {
                float ratio = 6f;

                Point thumbPoint = pageSize.getZoomPoint(pageSize.getScaleZoom() / ratio);
                Bitmap thumb = BitmapPool.getInstance().acquire(thumbPoint.x, thumbPoint.y);
                Matrix matrix = new Matrix(pageSize.getScaleZoom() / ratio);
                render(page, matrix, thumb, 0, leftBound, topBound);

                RectF rectF = ImageWorker.getCropRect(thumb);

                float scale = thumb.getWidth() / rectF.width();
                BitmapPool.getInstance().release(thumb);

                leftBound = (int) (rectF.left * ratio * scale);
                topBound = (int) (rectF.top * ratio * scale);

                height = (int) (rectF.height() * ratio * scale);
                ctm.scale(scale, scale);
                if (Logcat.loggable) {
                    Logcat.d(TAG, String.format("decode t:%s:%s:%s", height, pageSize.getZoomPoint().x, pageSize.getZoomPoint().y));
                }
            }

            int width = pageSize.getZoomPoint().x;
            if ((pageSize.getTargetWidth() > 0)) {
                width = pageSize.getTargetWidth();
            }
            if (Logcat.loggable) {
                Logcat.d(TAG, String.format("decode bitmap:width-height: %s-%s,pagesize:%s,%s, bound:%s,%s, page:%s",
                        width, height, pageSize.getZoomPoint().y, decodeParam.xOrigin, leftBound, topBound, pageSize));
            }
            Bitmap bitmap = BitmapPool.getInstance().acquire(width, height);//Bitmap.createBitmap(sizeX, sizeY, Bitmap.Config.ARGB_8888);

            render(page, ctm, bitmap, decodeParam.xOrigin, leftBound, topBound);

            page.destroy();
            //Logcat.d(TAG, "decode:" + (SystemClock.uptimeMillis() - start));
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void postBitmap(BitmapWorkerTask bitmapWorkerTask, Bitmap bitmap, DecodeParam decodeParam) {
        if (bitmapWorkerTask.isCancelled() || bitmap == null) {
            Logcat.w(TAG, "cancel decode.");
            return;
        }
        final ImageView imageView = bitmapWorkerTask.getAttachedImageView();
        if (imageView != null) {
            //((APDFView) imageView.getParent()).setDrawText("");
            //((APDFView) imageView.getParent()).setShowPaint(false);
            if (null != mBitmapManager) {
                mBitmapManager.setBitmap(decodeParam.pageSize.index, bitmap);
            }
            imageView.setImageBitmap(bitmap);
            imageView.getImageMatrix().reset();

            View parent = (View) imageView.getParent();
            if (parent.getHeight() != bitmap.getHeight() || parent.getWidth() != bitmap.getWidth()) {
                if (Logcat.loggable) {
                    Logcat.d(TAG, String.format("decode relayout bitmap:index:%s, %s:%s imageView->%s:%s view->%s:%s",
                            decodeParam.pageSize.index, bitmap.getWidth(), bitmap.getHeight(),
                            imageView.getWidth(), imageView.getHeight(),
                            parent.getWidth(), parent.getHeight()));
                }
                parent.getLayoutParams().height = bitmap.getHeight();
                parent.getLayoutParams().width = bitmap.getWidth();
                parent.requestLayout();
            }
        }
    }

    @Override
    public void recycle() {
        mImageCache.evictAll();
        pageLruCache.evictAll();
        mBitmapManager = null;
    }
}

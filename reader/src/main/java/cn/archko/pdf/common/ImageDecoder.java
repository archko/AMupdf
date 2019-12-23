package cn.archko.pdf.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.widget.ImageView;

import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Matrix;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.RectI;

import org.vudroid.core.DecodeService;

import androidx.collection.LruCache;
import cn.archko.pdf.App;
import cn.archko.pdf.entity.APage;
import cn.archko.pdf.pdf.MupdfDocument;
import cn.archko.pdf.utils.Utils;

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
                          ImageView imageView, Document document, DecodeService.DecodeCallback callback) {
        if (document == null || aPage == null || getImageCache() == null || imageView == null) {
            return;
        }
        super.loadImage(new DecodeParam(String.valueOf(aPage.index), imageView, autoCrop, xOrigin, aPage, document, callback));
    }

    @Override
    protected Bitmap processBitmap(DecodeParam decodeParam) {
        try {
            //long start = SystemClock.uptimeMillis();
            Page page = decodeParam.document.loadPage(decodeParam.pageSize.index);

            int leftBound = 0;
            int topBound = 0;
            APage pageSize = decodeParam.pageSize;
            int pageW = pageSize.getZoomPoint().x;
            int pageH = pageSize.getZoomPoint().y;

            if ((pageSize.getTargetWidth() > 0)) {
                pageW = pageSize.getTargetWidth();
            }

            Matrix ctm = new Matrix(MupdfDocument.ZOOM);
            RectI bbox = new RectI(page.getBounds().transform(ctm));
            float xscale = (float) pageW / (float) (bbox.x1 - bbox.x0);
            float yscale = (float) pageH / (float) (bbox.y1 - bbox.y0);
            ctm.scale(xscale, yscale);

            if (decodeParam.autoCrop) {
                int[] arr = MupdfDocument.getArrByCrop(page, ctm, pageW, pageH, leftBound, topBound);
                leftBound = arr[0];
                topBound = arr[1];
                pageH = arr[2];
            }
            if (Logcat.loggable) {
                Logcat.d(TAG, String.format("decode bitmap: %s-%s,page:%s-%s,xOrigin:%s, bound(left-top):%s-%s, page:%s",
                        pageW, pageH, pageSize.getZoomPoint().x, pageSize.getZoomPoint().y,
                        decodeParam.xOrigin, leftBound, topBound, pageSize));
            }
            Bitmap bitmap = BitmapPool.getInstance().acquire(pageW, pageH);//Bitmap.createBitmap(sizeX, sizeY, Bitmap.Config.ARGB_8888);

            MupdfDocument.render(page, ctm, bitmap, decodeParam.xOrigin, leftBound, topBound);

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
        if (null != mBitmapManager) {
            mBitmapManager.setBitmap(decodeParam.pageSize.index, bitmap);
        }
        if (null != decodeParam.decodeCallback) {
            decodeParam.decodeCallback.decodeComplete(bitmap);
        }
        /*final ImageView imageView = bitmapWorkerTask.getAttachedImageView();
        if (imageView != null) {
            //((APDFView) imageView.getParent()).setDrawText("");
            //((APDFView) imageView.getParent()).setShowPaint(false);
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
        }*/
    }

    @Override
    public void recycle() {
        mImageCache.evictAll();
        pageLruCache.evictAll();
        mBitmapManager = null;
    }
}

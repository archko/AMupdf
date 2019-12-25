package cn.archko.pdf.widgets;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Build;

import java.util.Objects;

import androidx.annotation.RequiresApi;
import cn.archko.pdf.common.BitmapCache;
import cn.archko.pdf.common.BitmapPool;
import cn.archko.pdf.common.Logcat;
import cn.archko.pdf.entity.APage;
import cn.archko.pdf.pdf.MupdfDocument;
import cn.archko.pdf.utils.Utils;

class PageTreeNode {
    private final RectF pageSliceBounds;
    private final APDFPage apdfPage;
    private Matrix matrix = new Matrix();
    private final Paint bitmapPaint = new Paint();
    private final Paint strokePaint = strokePaint();
    private Rect targetRect;
    private AsyncTask<String, String, Bitmap> bitmapAsyncTask;
    private boolean isRecycle = false;

    PageTreeNode(RectF localPageSliceBounds, APDFPage page) {
        this.pageSliceBounds = evaluatePageSliceBounds(localPageSliceBounds, null);
        this.apdfPage = page;
    }

    void updateVisibility() {
        if (isVisible()) {
            if (getBitmap() != null) {
                //apdfPage.documentView.postInvalidate();
            } else {
                decodePageTreeNode();
            }
        }
    }

    void invalidateNodeBounds() {
        targetRect = null;
    }

    private Paint strokePaint() {
        final Paint strokePaint = new Paint();
        strokePaint.setColor(Color.GREEN);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(2);
        return strokePaint;
    }

    void draw(Canvas canvas) {
        Bitmap bitmap = getBitmap();
        if (bitmap != null) {
            Logcat.d(String.format("bitmap:%s,w-h:%s-%s,rect:%s", bitmap, bitmap.getWidth(), bitmap.getHeight(), getTargetRect()));
            canvas.drawBitmap(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), getTargetRect(), bitmapPaint);
            canvas.drawRect(getTargetRect(), strokePaint);
        }
    }

    private boolean isVisible() {
        return true;
    }

    public Bitmap getBitmap() {
        Bitmap bitmap = BitmapCache.getInstance().getBitmap(getKey());
        if (bitmap != null) {
            return bitmap;
        }

        return null;
    }

    private void decodePageTreeNode() {
        //if (isDecodingNow()) {
        //    return;
        //}
        //setDecodingNow(true);

        //ImageDecoder.getInstance().loadImage(page.aPage, false, 0, page.documentView, page.mDocument,
        //        new DecodeService.DecodeCallback() {
        //            @Override
        //            public void decodeComplete(Bitmap bitmap) {
        //                //if (Logcat.loggable) {
        //                //    Logcat.d(String.format("decode2 relayout bitmap:index:%s, %s:%s imageView->%s:%s",
        //                //            pageSize.index, bitmap.width, bitmap.height,
        //                //            getWidth(), getHeight()));
        //                //}
        //                setBitmap(bitmap);
        //            }
        //        });
        decode(false, 0, apdfPage.aPage);
    }

    private RectF evaluatePageSliceBounds(RectF localPageSliceBounds, PageTreeNode parent) {
        if (parent == null) {
            return localPageSliceBounds;
        }
        final Matrix matrix = new Matrix();
        matrix.postScale(parent.pageSliceBounds.width(), parent.pageSliceBounds.height());
        matrix.postTranslate(parent.pageSliceBounds.left, parent.pageSliceBounds.top);
        final RectF sliceBounds = new RectF();
        matrix.mapRect(sliceBounds, localPageSliceBounds);
        return sliceBounds;
    }

    private void setBitmap(Bitmap bitmap) {
        if (getKey() == null || bitmap == null || (bitmap.getWidth() == -1 && bitmap.getHeight() == -1)) {
            return;
        }
        BitmapCache.getInstance().addBitmap(getKey(), bitmap);
        apdfPage.documentView.postInvalidate();
    }

    private String getKey() {
        if (targetRect == null) {
            getTargetRect();
        }
        return String.format("key:%s-%s,%s-%s", apdfPage.aPage.index, targetRect.left, targetRect.top,targetRect.right,targetRect.bottom);
    }

    private Rect getTargetRect() {
        if (targetRect == null) {
            matrix.reset();
            matrix.postScale(apdfPage.bounds.width(), apdfPage.bounds.height());
            matrix.postTranslate(apdfPage.bounds.left, apdfPage.bounds.top);
            RectF targetRectF = new RectF();
            matrix.mapRect(targetRectF, pageSliceBounds);
            targetRect = new Rect((int) targetRectF.left, (int) targetRectF.top, (int) targetRectF.right, (int) targetRectF.bottom);
        }
        return targetRect;
    }

    public void recycle() {
        isRecycle = true;
        if (null != bitmapAsyncTask) {
            bitmapAsyncTask.cancel(true);
            bitmapAsyncTask = null;
        }
        setBitmap(null);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageTreeNode that = (PageTreeNode) o;
        return Objects.equals(apdfPage, that.apdfPage) &&
                Objects.equals(matrix, that.matrix) &&
                Objects.equals(targetRect, that.targetRect) &&
                Objects.equals(bitmapAsyncTask, that.bitmapAsyncTask);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public int hashCode() {
        return Objects.hash(apdfPage, matrix, targetRect, bitmapAsyncTask);
    }

    @Override
    public String toString() {
        return "PageTreeNode{" +
                ", pageSliceBounds=" + pageSliceBounds +
                ", targetRect=" + targetRect +
                ", matrix=" + matrix +
                '}';
    }

    @SuppressLint("StaticFieldLeak")
    void decode(boolean autoCrop, int xOrigin, APage pageSize) {
        if (null != bitmapAsyncTask) {
            bitmapAsyncTask.cancel(true);
        }
        if (isRecycle) {
            return;
        }
        bitmapAsyncTask = new AsyncTask<String, String, Bitmap>() {
            @Override
            protected Bitmap doInBackground(String... params) {
                if (isCancelled()) {
                    return null;
                }
                return renderBitmap();
            }

            private Bitmap renderBitmap() {
                Rect rect = getTargetRect();
                int width = pageSize.getZoomPoint().x / 2;
                int height = pageSize.getZoomPoint().y / 2;
                int leftBound = 0;
                int topBound = 0;

                width = rect.width();
                height = rect.height();
                leftBound = rect.left;
                topBound = rect.top;

                Bitmap bitmap = BitmapPool.getInstance().acquire(width, height);

                float scale = 1.0f;
                com.artifex.mupdf.fitz.Page mPage = apdfPage.mDocument.loadPage(pageSize.index);
                com.artifex.mupdf.fitz.Matrix ctm = new com.artifex.mupdf.fitz.Matrix(pageSize.getScaleZoom() * scale);
                MupdfDocument.render(mPage, ctm, bitmap, xOrigin, leftBound, topBound);

                if (Logcat.loggable) {
                    Logcat.d(String.format("decode bitmap:rect:%s, width-height:%s-%s,xOrigin:%s, bound:%s-%s, page:%s",
                            getTargetRect(),
                            width, height, xOrigin, leftBound, topBound, pageSize));
                }
                mPage.destroy();
                return bitmap;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                setBitmap(bitmap);
            }
        };
        Utils.execute(true, bitmapAsyncTask);
    }
}

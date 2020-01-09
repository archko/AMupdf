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

import com.artifex.mupdf.fitz.RectI;

import cn.archko.pdf.common.BitmapCache;
import cn.archko.pdf.common.BitmapPool;
import cn.archko.pdf.common.Logcat;
import cn.archko.pdf.entity.APage;
import cn.archko.pdf.pdf.MupdfDocument;
import cn.archko.pdf.utils.Utils;

class PageTreeNode {

    static final int PAGE_TYPE_LEFT_TOP = 0;
    static final int PAGE_TYPE_RIGHT_TOP = 1;
    static final int PAGE_TYPE_LEFT_BOTTOM = 2;
    static final int PAGE_TYPE_RIGHT_BOTTOM = 3;
    int pageType = PAGE_TYPE_LEFT_TOP;
    private final RectF pageSliceBounds;
    private final APDFPage apdfPage;
    private Matrix matrix = new Matrix();
    private Matrix cropMatrix = new Matrix();
    private final Paint bitmapPaint = new Paint();
    private final Paint strokePaint = strokePaint();
    private final Paint strokePaint2 = strokePaint2();
    private final Paint fillPaint = fillPaint();
    private Rect targetRect;
    private Rect cropTargetRect;
    private AsyncTask<String, String, Bitmap> bitmapAsyncTask;
    private boolean isRecycle = false;

    PageTreeNode(RectF localPageSliceBounds, APDFPage page, int pageType) {
        this.pageSliceBounds = evaluatePageSliceBounds(localPageSliceBounds, null);
        this.apdfPage = page;
        this.pageType = pageType;
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
        strokePaint.setStrokeWidth(4);
        return strokePaint;
    }

    private Paint strokePaint2() {
        final Paint strokePaint = new Paint();
        strokePaint.setColor(Color.RED);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(2);
        return strokePaint;
    }

    private Paint fillPaint() {
        final Paint fillPaint = new Paint();
        fillPaint.setColor(Color.GRAY);
        fillPaint.setStyle(Paint.Style.FILL);
        return fillPaint;
    }

    void draw(Canvas canvas) {
        Bitmap bitmap = getBitmap();
        if (bitmap != null) {
            Rect tRect = getTargetRect();
            Logcat.d(String.format("draw:%s-%s,w-h:%s-%s,rect:%s", tRect.width(), tRect.height(), bitmap.getWidth(), bitmap.getHeight(), tRect));
            canvas.drawBitmap(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), tRect, bitmapPaint);
            canvas.drawRect(tRect, strokePaint);
            canvas.drawRect(getCropTargetRect(), strokePaint2);
        }
    }

    private boolean isVisible() {
        return true;
    }

    public Bitmap getBitmap() {
        return BitmapCache.getInstance().getBitmap(getKey());
    }

    private void decodePageTreeNode() {
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
        decode(0, apdfPage.aPage);
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
        if (isRecycle || getKey() == null || bitmap == null /*|| (bitmap.getWidth() == -1 && bitmap.getHeight() == -1)*/) {
            return;
        }
        BitmapCache.getInstance().addBitmap(getKey(), bitmap);
        apdfPage.documentView.postInvalidate();
    }

    private String getKey() {
        if (targetRect == null) {
            getTargetRect();
        }
        return String.format("key:%s-%s,%s-%s", apdfPage.aPage.index, targetRect.left, targetRect.top, targetRect.right, targetRect.bottom);
    }

    private Rect getTargetRect() {
        if (targetRect == null) {
            matrix.reset();
            matrix.postScale(apdfPage.getBounds().width(), apdfPage.getBounds().height());
            matrix.postTranslate(apdfPage.getBounds().left, apdfPage.getBounds().top);
            RectF targetRectF = new RectF();
            matrix.mapRect(targetRectF, pageSliceBounds);
            targetRect = new Rect((int) targetRectF.left, (int) targetRectF.top, (int) targetRectF.right, (int) targetRectF.bottom);
        }
        return targetRect;
    }

    private Rect getCropTargetRect() {
        if (cropTargetRect == null) {
            cropMatrix.reset();
            RectF cropBounds = apdfPage.getCropBounds();
            cropMatrix.postScale(cropBounds.width(), cropBounds.height());
            //cropMatrix.postScale(apdfPage.aPage.getCropScale(), apdfPage.aPage.getCropScale());
            cropMatrix.postTranslate(cropBounds.left, cropBounds.top);
            RectF rectF = new RectF();
            cropMatrix.mapRect(rectF, pageSliceBounds);
            cropTargetRect = new Rect((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
        }
        return cropTargetRect;
    }

    public void recycle() {
        isRecycle = true;
        if (null != bitmapAsyncTask) {
            bitmapAsyncTask.cancel(true);
            bitmapAsyncTask = null;
        }
        setBitmap(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PageTreeNode that = (PageTreeNode) o;

        if (isRecycle != that.isRecycle) return false;
        if (pageSliceBounds != null ? !pageSliceBounds.equals(that.pageSliceBounds) : that.pageSliceBounds != null)
            return false;
        if (apdfPage != null ? !apdfPage.equals(that.apdfPage) : that.apdfPage != null)
            return false;
        if (matrix != null ? !matrix.equals(that.matrix) : that.matrix != null) return false;
        return targetRect != null ? targetRect.equals(that.targetRect) : that.targetRect == null;
    }

    @Override
    public int hashCode() {
        int result = pageSliceBounds != null ? pageSliceBounds.hashCode() : 0;
        result = 31 * result + (apdfPage != null ? apdfPage.hashCode() : 0);
        result = 31 * result + (matrix != null ? matrix.hashCode() : 0);
        result = 31 * result + (targetRect != null ? targetRect.hashCode() : 0);
        result = 31 * result + (isRecycle ? 1 : 0);
        return result;
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
    void decode(int xOrigin, APage pageSize) {
        if (null != bitmapAsyncTask) {
            bitmapAsyncTask.cancel(true);
        }
        if (isRecycle) {
            return;
        }
        if (apdfPage.isDecodingCrop) {
            return;
        }
        if (apdfPage.crop && apdfPage.cropBounds == null) {
            apdfPage.isDecodingCrop = true;
            decodeCropBounds(pageSize);
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
                float scale = 1.0f;
                if (apdfPage.crop && apdfPage.cropBounds != null) {
                    rect = getCropTargetRect();
                    scale=apdfPage.aPage.getCropScale();
                }

                int leftBound = 0;
                int topBound = 0;

                int width = rect.width();
                int height = rect.height();
                leftBound = rect.left;
                topBound = rect.top;

                Bitmap bitmap = BitmapPool.getInstance().acquire(width, height);

                com.artifex.mupdf.fitz.Page mPage = apdfPage.mDocument.loadPage(pageSize.index);
                com.artifex.mupdf.fitz.Matrix ctm = new com.artifex.mupdf.fitz.Matrix(pageSize.getScaleZoom() * scale);
                MupdfDocument.render(mPage, ctm, bitmap, xOrigin, leftBound, topBound);

                if (Logcat.loggable) {
                    Logcat.d(String.format("decode bitmap:rect:%s-%s, width-height:%s-%s,xOrigin:%s, bound:%s-%s, page:%s",
                            getTargetRect(), rect,
                            width, height, xOrigin, leftBound, topBound, pageSize));
                }
                mPage.destroy();

                //BitmapUtils.saveBitmapToFile(bitmap, new File(FileUtils.getStoragePath(pageType + "xx.png")));
                return bitmap;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                setBitmap(bitmap);
            }
        };
        Utils.execute(true, bitmapAsyncTask);
    }

    @SuppressLint("StaticFieldLeak")
    private void decodeCropBounds(APage pageSize) {
        Utils.execute(true, new AsyncTask<String, String, RectF>() {
            @Override
            protected RectF doInBackground(String... params) {
                if (isCancelled()) {
                    return null;
                }
                return renderBitmap();
            }

            private RectF renderBitmap() {
                int leftBound = 0;
                int topBound = 0;
                int pageW = pageSize.getZoomPoint().x;
                int pageH = pageSize.getZoomPoint().y;
                com.artifex.mupdf.fitz.Page page = apdfPage.mDocument.loadPage(pageSize.index);
                com.artifex.mupdf.fitz.Matrix ctm = new com.artifex.mupdf.fitz.Matrix(MupdfDocument.ZOOM);
                RectI bbox = new RectI(page.getBounds().transform(ctm));
                float xscale = (float) pageW / (float) (bbox.x1 - bbox.x0);
                float yscale = (float) pageH / (float) (bbox.y1 - bbox.y0);
                ctm.scale(xscale, yscale);

                float[] arr = MupdfDocument.getArrByCrop(page, ctm, pageW, pageH, leftBound, topBound);
                leftBound = (int) arr[0];
                topBound = (int) arr[1];
                pageH = (int) arr[2];
                float cropScale = arr[3];

                pageSize.setCropWidth(pageW);
                pageSize.setCropHeight(pageH);
                RectF cropRectf = new RectF(leftBound, topBound, leftBound + pageW, topBound + pageH);
                pageSize.setCropBounds(cropRectf, cropScale);
                return cropRectf;
            }

            @Override
            protected void onPostExecute(RectF rectF) {
                if (null != rectF) {
                    apdfPage.setCropBounds(rectF);
                }
            }
        });
    }
}

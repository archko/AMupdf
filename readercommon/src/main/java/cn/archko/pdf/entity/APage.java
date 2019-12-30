package cn.archko.pdf.entity;

import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;

/**
 * 有两个对象,一个是com.artifex.mupdf.fitz.Page,包含了这个页的原始信息.
 * 另一个是缩放值,android.graphics.PointF对象
 * +++++++++++++++++++++++++++++++++++++++++++
 * +              ------------------               +
 * +              -                    -               +
 * +              -                    -               +
 * +              -                    -               +
 * +              -                    -               +
 * +  view       -      page         -     view     +
 * +              -                    -               +
 * +              -                    -               +
 * +              -                    -               +
 * +              -                    -               +
 * +              ------------------               +
 * +++++++++++++++++++++++++++++++++++++++++++
 *
 * @author: archko 2019/2/1 :19:29
 */
public class APage {

    /**
     * pageindex
     */
    public final int index;
    /**
     * width/height
     */
    private PointF mPageSize;    // pagesize of real page
    /**
     * view zoom
     */
    private float mZoom;
    private int targetWidth;
    /**
     * viewwidth/pagewidth
     */
    private float scale = 1f;

    //===================== render a bitmap from mupdf,no crop=======================
    //val ctm = Matrix(pageSize.scale*zoom)
    //var width = pageSize.zoomPoint.x=scale*zoom*mPageSize.x
    //var height = pageSize.zoomPoint.y=scale*zoom*mPageSize.y
    //val bitmap = BitmapPool.getInstance().acquire(width, height)
    //MupdfDocument.render(page, ctm, bitmap, xOrigin, leftBound, topBound);

    //===================== render a bitmap from mupdf,crop white bounds =======================
    //1.get origin page:
    // val ctm = Matrix(pageSize.scale*zoom)
    // var width = pageSize.zoomPoint.x=scale*zoom*mPageSize.x
    // var height = pageSize.zoomPoint.y=scale*zoom*mPageSize.y
    //2.render as a thumb to get rectf.
    // var ratio=6;
    // val thumb = BitmapPool.getInstance().acquire(width / ratio, height / ratio)
    // Matrix m=ctm.scale(1/ratio)
    // MupdfDocument.render(page, m, thumb, 0, 0, 0)
    //3.caculate new width, height,and ctm. new width = viewwidth
    // val rectF = MupdfDocument.getCropRect(thumb)
    // var sscale = thumb.width / rectF.width()
    // val ctm = Matrix(pageSize.scaleZoom * sscale)
    //4.restore to a full bitmap,get bound,scale
    // leftBound = (rectF.left * sscale * ratio)
    // topBound = (rectF.top * sscale * ratio)
    // height = (rectF.height() * sscale * ratio)
    //5.render a crop page
    //val bitmap = BitmapPool.getInstance().acquire(width, height)
    // MupdfDocument.render(page, ctm, bitmap, xOrigin, leftBound, topBound)

    private float cropScale = 1.0f;
    private RectF sourceBounds;
    private RectF cropBounds;

    private int cropWidth = 0;
    private int cropHeight = 0;

    public APage(int pageNumber, PointF pageSize, float zoom, int targetWidth) {
        index = pageNumber;
        this.mPageSize = pageSize;
        this.mZoom = zoom;
        setTargetWidth(targetWidth);
        initSourceBounds(1.0f);
    }

    private void initSourceBounds(float scale) {
        sourceBounds = new RectF();
        sourceBounds.right = getEffectivePagesWidth() * scale;
        sourceBounds.bottom = getEffectivePagesHeight() * scale;
    }

    public int getTargetWidth() {
        return targetWidth;
    }

    public void setTargetWidth(int targetWidth) {
        this.targetWidth = targetWidth;
        if (targetWidth > 0) {
            scale = calculateScale(targetWidth);
        }
    }

    private float calculateScale(int tw) {
        return 1.0f * tw / mPageSize.x;
    }

    public int getEffectivePagesWidth() {
        return getScaledWidth(mPageSize, scale);
    }

    public int getEffectivePagesHeight() {
        return getScaledHeight(mPageSize, scale);
    }

    private int getScaledHeight(PointF page, float scale) {
        return (int) (scale * page.y);
    }

    private int getScaledWidth(PointF page, float scale) {
        return (int) (scale * page.x);
    }

    public int getIndex() {
        return index;
    }

    public float getZoom() {
        return mZoom;
    }

    public void setZoom(float zoom) {
        this.mZoom = zoom;
    }

    public float getScaleZoom() {
        return scale * mZoom;
    }

    public Point getZoomPoint() {
        return getZoomPoint(getScaleZoom());
    }

    public Point getZoomPoint(float scaleZoom) {
        return new Point((int) (scaleZoom * mPageSize.x), (int) (scaleZoom * mPageSize.y));
    }

    public RectF getCropBounds() {
        return cropBounds;
    }

    public void setCropBounds(RectF cropBounds, float cropScale) {
        this.cropBounds = cropBounds;
        this.cropScale = cropScale;
        initSourceBounds(cropScale);
        setCropWidth((int) cropBounds.right);
        setCropHeight((int) cropBounds.bottom);
    }

    public RectF getSourceBounds() {
        return sourceBounds;
    }

    public float getCropScale() {
        return cropScale;
    }

    public int getCropWidth() {
        return cropWidth;
    }

    public int getCropHeight() {
        return cropHeight;
    }

    public void setCropWidth(int cropWidth) {
        this.cropWidth = cropWidth;
    }

    public void setCropHeight(int cropHeight) {
        this.cropHeight = cropHeight;
    }

    public int getRealCropWidth() {
        //if (sourceBounds != null) {
        //    return (int) sourceBounds.width();
        //}
        if (cropWidth == 0) {
            cropWidth = getEffectivePagesWidth();
        }
        return cropWidth;
    }

    public int getRealCropHeight() {
        if (cropBounds != null) {
            return (int) cropBounds.height();
        }
        if (cropHeight == 0) {
            cropHeight = getEffectivePagesHeight();
        }
        return cropHeight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        APage aPage = (APage) o;

        if (index != aPage.index) return false;
        if (Float.compare(aPage.mZoom, mZoom) != 0) return false;
        if (targetWidth != aPage.targetWidth) return false;
        if (Float.compare(aPage.scale, scale) != 0) return false;
        return mPageSize != null ? mPageSize.equals(aPage.mPageSize) : aPage.mPageSize == null;
    }

    @Override
    public int hashCode() {
        int result = index;
        result = 31 * result + (mPageSize != null ? mPageSize.hashCode() : 0);
        result = 31 * result + (mZoom != +0.0f ? Float.floatToIntBits(mZoom) : 0);
        result = 31 * result + targetWidth;
        result = 31 * result + (scale != +0.0f ? Float.floatToIntBits(scale) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "APage{" +
                "index=" + index +
                ", mPageSize=" + mPageSize +
                ", mZoom=" + mZoom +
                ", targetWidth=" + targetWidth +
                ", scale=" + scale +
                ", cropScale=" + cropScale +
                ", sourceBounds=" + sourceBounds +
                ", cropBounds=" + cropBounds +
                ", cropWidth=" + cropWidth +
                ", cropHeight=" + cropHeight +
                '}';
    }
}

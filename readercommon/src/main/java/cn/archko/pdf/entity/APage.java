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
    private float aspectRatio;
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

    private RectF cropBounds;

    private int cropWidth = 0;
    private int cropHeight = 0;

    public APage(int pageNumber, PointF pageSize, float zoom, int targetWidth) {
        index = pageNumber;
        this.mPageSize = pageSize;
        this.mZoom = zoom;
        setTargetWidth(targetWidth);
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

    public void setAspectRatio(float aspectRatio) {
        if (this.aspectRatio != aspectRatio) {
            this.aspectRatio = aspectRatio;
        }
    }

    public void setAspectRatio(int width, int height) {
        setAspectRatio(width * 1.0f / height);
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

    public void setCropBounds(RectF cropBounds) {
        this.cropBounds = cropBounds;
    }

    public int getCropWidth() {
        return cropWidth;
    }

    public int getCropHeight() {
        return cropHeight;
    }

    public int getRealCropWidth() {
        if (cropWidth == 0) {
            cropWidth = getEffectivePagesWidth();
        }
        return cropWidth;
    }

    public void setCropWidth(int cropWidth) {
        this.cropWidth = cropWidth;
    }

    public int getRealCropHeight() {
        if (cropHeight == 0) {
            cropHeight = getEffectivePagesHeight();
        }
        return cropHeight;
    }

    public void setCropHeight(int cropHeight) {
        this.cropHeight = cropHeight;
    }

    @Override
    public String toString() {
        return "APage{" +
                "index=" + index +
                ", targetWidth=" + targetWidth +
                ", scale=" + scale +
                ", mPageSize=" + mPageSize +
                ", mZoom=" + mZoom +
                '}';
    }
}

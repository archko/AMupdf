package cn.archko.pdf.entity;

import android.graphics.Point;
import android.graphics.PointF;

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

    public final int index;
    private float aspectRatio;
    private PointF mPageSize;    // pagesize of real page
    private float mZoom;
    private int targetWidth;
    private float scale = 1f;

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
            scale = calculateScale();
        }
    }

    private float calculateScale() {
        return 1.0f * targetWidth / mPageSize.x;
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

    public void update(float zoom) {
        mZoom = zoom;
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

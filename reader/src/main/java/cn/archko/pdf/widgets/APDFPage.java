package cn.archko.pdf.widgets;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.view.View;

import com.artifex.mupdf.fitz.Document;

import java.util.Arrays;

import cn.archko.pdf.entity.APage;

class APDFPage {
    APage aPage;
    private RectF bounds;
    private final Paint fillPaint = fillPaint();
    private final Paint strokePaint = strokePaint();
    private PageTreeNode[] children;
    View documentView;
    Document mDocument;
    RectF cropBounds;
    boolean crop = false;
    boolean isDecodingCrop = false;

    APDFPage(View documentView, APage aPage, Document document, boolean crop) {
        this.aPage = aPage;
        this.mDocument = document;
        this.crop = crop;
        update(documentView, aPage);
    }

    private void initChildren() {
        children = new PageTreeNode[]{
                new PageTreeNode(new RectF(0, 0, 0.5f, 0.5f), this, PageTreeNode.PAGE_TYPE_LEFT_TOP),
                new PageTreeNode(new RectF(0.5f, 0, 1.0f, 0.5f), this, PageTreeNode.PAGE_TYPE_RIGHT_TOP),
                new PageTreeNode(new RectF(0, 0.5f, 0.5f, 1.0f), this, PageTreeNode.PAGE_TYPE_LEFT_BOTTOM),
                new PageTreeNode(new RectF(0.5f, 0.5f, 1.0f, 1.0f), this, PageTreeNode.PAGE_TYPE_RIGHT_BOTTOM)
        };
    }

    void update(View documentView, APage aPage) {
        if (this.aPage != aPage || this.documentView != documentView) {
            if (children != null) {
                for (PageTreeNode node : children) {
                    node.recycle();
                }
            }
            initChildren();
        }
        this.aPage = aPage;
        this.documentView = documentView;
        if (null != aPage.getCropBounds() && cropBounds != aPage.getCropBounds()) {
            this.cropBounds = cropBounds;
        }
    }

    void checkChildren() {
        if (null == children) {
            initChildren();
        }
    }

    public int getTop() {
        return Math.round(bounds.top);
    }

    public int getBottom() {
        return Math.round(bounds.bottom);
    }

    public void draw(Canvas canvas) {
        if (!isVisible()) {
            return;
        }
        canvas.drawRect(bounds, fillPaint);
        if (children == null) {
            return;
        }
        for (PageTreeNode child : children) {
            child.draw(canvas);
        }
        canvas.drawLine(bounds.left, bounds.top, bounds.right, bounds.top, strokePaint);
        canvas.drawLine(bounds.left, bounds.bottom, bounds.right, bounds.bottom, strokePaint);
    }

    private Paint strokePaint() {
        final Paint strokePaint = new Paint();
        strokePaint.setColor(Color.BLACK);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(2);
        return strokePaint;
    }

    private Paint fillPaint() {
        final Paint fillPaint = new Paint();
        //fillPaint.setColor(Color.GRAY);
        fillPaint.setColor(Color.WHITE);
        fillPaint.setStyle(Paint.Style.FILL);
        return fillPaint;
    }

    private TextPaint textPaint() {
        final TextPaint paint = new TextPaint();
        paint.setColor(Color.BLACK);
        paint.setAntiAlias(true);
        paint.setTextSize(32);
        paint.setTextAlign(Paint.Align.CENTER);
        return paint;
    }

    public boolean isVisible() {
        //return RectF.intersects(documentView.getViewRect(), bounds);
        return true;
    }

    void setBounds(RectF pageBounds) {
        bounds = pageBounds;
        cropBounds = null;
        if (children != null) {
            for (PageTreeNode child : children) {
                child.invalidateNodeBounds();
            }
        }
    }

    public void setCropBounds(RectF cropBounds) {
        isDecodingCrop = false;
        if (this.cropBounds != cropBounds) {
            this.cropBounds = cropBounds;
            if (children != null) {
                for (PageTreeNode child : children) {
                    child.updateVisibility();
                    child.invalidateNodeBounds();
                }
            }
        }
    }

    RectF getBounds() {
        return bounds;
    }

    RectF getCropBounds() {
        if (crop && cropBounds != null) {
            return cropBounds;
        }
        return bounds;
    }

    public void updateVisibility(boolean crop, int xOrigin) {
        if (this.crop != crop) {
            recycleChildren();
        }
        checkChildren();
        this.crop = crop;
        if (children != null) {
            for (PageTreeNode child : children) {
                child.updateVisibility();
            }
        }
        documentView.postInvalidate();
    }

    public void recycle() {
        recycleChildren();
        //aPage = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        APDFPage apdfPage = (APDFPage) o;

        if (crop != apdfPage.crop) return false;
        if (aPage != null ? !aPage.equals(apdfPage.aPage) : apdfPage.aPage != null) return false;
        if (bounds != null ? !bounds.equals(apdfPage.bounds) : apdfPage.bounds != null)
            return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(children, apdfPage.children)) return false;
        if (documentView != null ? !documentView.equals(apdfPage.documentView) : apdfPage.documentView != null)
            return false;
        return cropBounds != null ? cropBounds.equals(apdfPage.cropBounds) : apdfPage.cropBounds == null;
    }

    @Override
    public int hashCode() {
        int result = aPage != null ? aPage.hashCode() : 0;
        result = 31 * result + (bounds != null ? bounds.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(children);
        result = 31 * result + (documentView != null ? documentView.hashCode() : 0);
        result = 31 * result + (cropBounds != null ? cropBounds.hashCode() : 0);
        result = 31 * result + (crop ? 1 : 0);
        return result;
    }

    private void recycleChildren() {
        if (children == null) {
            return;
        }
        for (PageTreeNode child : children) {
            child.recycle();
        }
        children = null;
    }

    @Override
    public String toString() {
        return "APDFPage{" +
                "index=" + aPage +
                ", bounds=" + bounds +
                ", children=" + children +
                '}';
    }
}

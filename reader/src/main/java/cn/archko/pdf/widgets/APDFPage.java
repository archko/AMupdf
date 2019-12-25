package cn.archko.pdf.widgets;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.view.View;

import com.artifex.mupdf.fitz.Document;

import cn.archko.pdf.entity.APage;

class APDFPage {
    APage aPage;
    RectF bounds;
    private final TextPaint textPaint = textPaint();
    private final Paint fillPaint = fillPaint();
    private final Paint strokePaint = strokePaint();
    private PageTreeNode[] children;
    View documentView;
    Document mDocument;

    APDFPage(View documentView, APage aPage, Document document) {
        this.aPage = aPage;
        this.mDocument = document;
        update(documentView, aPage);
    }

    private void initChildren() {
        children = new PageTreeNode[]{
                new PageTreeNode(new RectF(0, 0, 0.5f, 0.5f), this),
                new PageTreeNode(new RectF(0.5f, 0, 1.0f, 0.5f), this),
                new PageTreeNode(new RectF(0, 0.5f, 0.5f, 1.0f), this),
                new PageTreeNode(new RectF(0.5f, 0.5f, 1.0f, 1.0f), this)
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
    }

    void checkChildren() {
        if (null == children) {
            initChildren();
        }
    }

    private float aspectRatio;

    float getPageHeight(int mainWidth, float zoom) {
        return mainWidth / getAspectRatio() * zoom;
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

    public float getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(float aspectRatio) {
        if (this.aspectRatio != aspectRatio) {
            this.aspectRatio = aspectRatio;
            //documentView.invalidatePageSizes();
        }
    }

    public boolean isVisible() {
        //return RectF.intersects(documentView.getViewRect(), bounds);
        return true;
    }

    public void setAspectRatio(int width, int height) {
        setAspectRatio(width * 1.0f / height);
    }

    void setBounds(RectF pageBounds) {
        bounds = pageBounds;
        if (children != null) {
            for (PageTreeNode child : children) {
                child.invalidateNodeBounds();
            }
        }
    }

    public void updateVisibility() {
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
                ", aspectRatio=" + aspectRatio +
                '}';
    }
}

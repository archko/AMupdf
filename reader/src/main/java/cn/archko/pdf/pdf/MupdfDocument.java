package cn.archko.pdf.pdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Environment;

import com.artifex.mupdf.fitz.Cookie;
import com.artifex.mupdf.fitz.DisplayList;
import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Link;
import com.artifex.mupdf.fitz.Matrix;
import com.artifex.mupdf.fitz.Outline;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.Quad;
import com.artifex.mupdf.fitz.Rect;
import com.artifex.mupdf.fitz.RectI;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;
import com.artifex.mupdf.viewer.OutlineActivity;

import org.ebookdroid.core.crop.PageCropper;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import cn.archko.pdf.common.BitmapPool;
import cn.archko.pdf.common.Logcat;

/**
 * @author archko 2019/12/8 :12:43
 */
public class MupdfDocument {

    private static final String TAG = "Mupdf";
    private Context context;
    private int resolution;
    private Document document;
    private Outline[] outline;
    private int pageCount = -1;
    private int currentPage;
    private Page page;
    private float pageWidth;
    private float pageHeight;
    private DisplayList displayList;

    /* Default to "A Format" pocket book size. */
    private int layoutW = 312;
    private int layoutH = 504;
    private int layoutEM = 8;

    public Document getDocument() {
        return document;
    }

    //File selectedFile = new File(filename);
    //String documentPath = selectedFile.getAbsolutePath();
    //String acceleratorPath = getAcceleratorPath(documentPath);
    //if (acceleratorValid(selectedFile, new File(acceleratorPath))) {
    //	doc = Document.openDocument(documentPath, acceleratorPath);
    //} else {
    //	doc = Document.openDocument(documentPath);
    //}
    //doc.saveAccelerator(acceleratorPath);
    public static String getAcceleratorPath(String documentPath) {
        String acceleratorPath = documentPath.substring(1);
        acceleratorPath = acceleratorPath.replace(File.separatorChar, '%');
        acceleratorPath = acceleratorPath.replace('\\', '%');
        acceleratorPath = acceleratorPath.replace(':', '%');
        String tmpdir = Environment.getExternalStorageDirectory().getPath() + "/amupdf";
        return new StringBuffer(tmpdir).append(File.separatorChar).append(acceleratorPath).append(".accel").toString();
    }

    public static boolean acceleratorValid(File documentFile, File acceleratorFile) {
        long documentModified = documentFile.lastModified();
        long acceleratorModified = acceleratorFile.lastModified();
        return acceleratorModified != 0 && acceleratorModified > documentModified;
    }

    public MupdfDocument(Context context) {
        this.context = context;
    }

    public Document newDocument(String pfd, String password) {
        document = Document.openDocument(pfd);
        initDocument();
        return document;
    }

    public Document newDocument(byte[] data, String password) {
        document = Document.openDocument(data, "magic");
        initDocument();
        return document;
    }

    private void initDocument() {
        document.layout(layoutW, layoutH, layoutEM);
        pageCount = document.countPages();
        resolution = 160;
        currentPage = -1;
    }

    public String getTitle() {
        return document.getMetaData(Document.META_INFO_TITLE);
    }

    public int countPages() {
        return pageCount;
    }

    public boolean isReflowable() {
        return document.isReflowable();
    }

    public int layout(int oldPage, int w, int h, int em) {
        if (w != layoutW || h != layoutH || em != layoutEM) {
            System.out.println("LAYOUT: " + w + "," + h);
            layoutW = w;
            layoutH = h;
            layoutEM = em;
            long mark = document.makeBookmark(document.locationFromPageNumber(oldPage));
            document.layout(layoutW, layoutH, layoutEM);
            currentPage = -1;
            pageCount = document.countPages();
            outline = null;
            try {
                outline = document.loadOutline();
            } catch (Exception ex) {
                /* ignore error */
            }
            return document.pageNumberFromLocation(document.findBookmark(mark));
        }
        return oldPage;
    }

    public void gotoPage(int pageNum) {
        /* TODO: page cache */
        if (pageNum > pageCount - 1)
            pageNum = pageCount - 1;
        else if (pageNum < 0)
            pageNum = 0;
        if (pageNum != currentPage) {
            currentPage = pageNum;
            if (page != null)
                page.destroy();
            page = null;
            if (displayList != null)
                displayList.destroy();
            displayList = null;
            page = document.loadPage(pageNum);
            Rect b = page.getBounds();
            pageWidth = b.x1 - b.x0;
            pageHeight = b.y1 - b.y0;
        }
    }

    public PointF getPageSize(int pageNum) {
        gotoPage(pageNum);
        return new PointF(pageWidth, pageHeight);
    }

    public void onDestroy() {
        if (displayList != null)
            displayList.destroy();
        displayList = null;
        if (page != null)
            page.destroy();
        page = null;
        if (document != null)
            document.destroy();
        document = null;
    }

    /**
     * 渲染页面,传入一个Bitmap对象.使用硬件加速,虽然速度影响不大.
     *
     * @param bm     需要渲染的位图,配置为ARGB8888
     * @param page   当前渲染页面页码
     * @param pageW  页面的宽,由缩放级别计算得到的最后宽,由于这个宽诸页面的裁剪大小,如果不正确,得到的Tile页面是不正确的
     * @param pageH  页面的宽,由缩放级别计算得到的最后宽,由于这个宽诸页面的裁剪大小,如果不正确,得到的Tile页面是不正确的
     * @param patchX 裁剪的页面的左顶点
     * @param patchY 裁剪的页面的上顶点
     * @param patchW 页面的宽,具体渲染的页面实际大小.显示出来的大小.
     * @param patchH 页面的高,具体渲染的页面实际大小.显示出来的大小.
     */
    public void drawPage(Bitmap bm, int pageNum,
                         int pageW, int pageH,
                         int patchX, int patchY,
                         Cookie cookie) {
        gotoPage(pageNum);

        if (displayList == null)
            displayList = page.toDisplayList(false);

        float zoom = resolution / 72;
        Matrix ctm = new Matrix(zoom, zoom);
        RectI bbox = new RectI(page.getBounds().transform(ctm));
        float xscale = (float) pageW / (float) (bbox.x1 - bbox.x0);
        float yscale = (float) pageH / (float) (bbox.y1 - bbox.y0);
        ctm.scale(xscale, yscale);

        AndroidDrawDevice dev = new AndroidDrawDevice(bm, patchX, patchY);
        displayList.run(dev, ctm, cookie);
        dev.close();
        dev.destroy();
    }


    public Link[] getPageLinks(int pageNum) {
        gotoPage(pageNum);
        return page.getLinks();
    }

    public int resolveLink(Link link) {
        return document.pageNumberFromLocation(document.resolveLink(link));
    }

    public Quad[] searchPage(int pageNum, String text) {
        gotoPage(pageNum);
        return page.search(text);
    }

    public boolean hasOutline() {
        if (outline == null) {
            try {
                outline = document.loadOutline();
            } catch (Exception ex) {
                /* ignore error */
            }
        }
        return outline != null;
    }

    private void flattenOutlineNodes(ArrayList<OutlineActivity.Item> result, Outline list[], String indent) {
        for (Outline node : list) {
            if (node.title != null) {
                int page = document.pageNumberFromLocation(document.resolveLink(node));
                result.add(new OutlineActivity.Item(indent + node.title, page));
            }
            if (node.down != null)
                flattenOutlineNodes(result, node.down, indent + "    ");
        }
    }

    public ArrayList<OutlineActivity.Item> getOutline() {
        ArrayList<OutlineActivity.Item> result = new ArrayList<OutlineActivity.Item>();
        flattenOutlineNodes(result, outline, "");
        return result;
    }

    public boolean needsPassword() {
        return document.needsPassword();
    }

    public boolean authenticatePassword(String password) {
        return document.authenticatePassword(password);
    }

    public Bitmap nativeRender(int pageNum, boolean autoCrop,
                               int pageW, int pageH,
                               int patchX, int patchY) {
        Page page = document.loadPage(pageNum);

        final float zoom = 2;
        final Matrix ctm = new Matrix(zoom, zoom);
        final RectI bbox = new RectI(page.getBounds().transform(ctm));
        final float xscale = (float) pageW / (float) (bbox.x1 - bbox.x0);
        final float yscale = (float) pageH / (float) (bbox.y1 - bbox.y0);
        ctm.scale(xscale, yscale);

        int leftBound = 0;
        int topBound = 0;
        int width = pageW;
        int height = pageH;
        if (autoCrop) {
            Object[] arr = MupdfDocument.getArrByCrop(page, ctm, pageW, pageH, leftBound, topBound);
            leftBound = (int) arr[0];
            topBound = (int) arr[1];
            height = (int) arr[2];
            RectF rectF = (RectF) arr[3];
        }

        //if (Logcat.loggable) {
        //    Logcat.d(String.format("decode bitmap:width-height: %s-%s,pagesize:%s,%s, bound:%s,%s,%s",
        //            width, height, pageW, pageH, leftBound, topBound, ctm));
        //}

        Bitmap bitmap = BitmapPool.getInstance().acquire(width, height);

        render(page, ctm, bitmap, 0, leftBound, topBound);

        page.destroy();
        return bitmap;
    }

    public static Object[] getArrByCrop(Page page, Matrix ctm, int pageW, int pageH, int leftBound, int topBound) {
        float ratio = 6f;
        Bitmap thumb = BitmapPool.getInstance().acquire((int) (pageW / ratio), (int) (pageH / ratio));
        Matrix matrix = new Matrix(ctm.a / ratio, ctm.d / ratio);
        render(page, matrix, thumb, 0, leftBound, topBound);

        RectF rectF = getCropRect(thumb);

        float scale = thumb.getWidth() / rectF.width();
        rectF.left = rectF.left * ratio * scale;
        rectF.top = rectF.top * ratio * scale;
        rectF.right = rectF.right * ratio * scale;
        rectF.bottom = rectF.bottom * ratio * scale;
        leftBound = (int) (rectF.left);
        topBound = (int) (rectF.top);

        int height = (int) (rectF.height() * ratio * scale);
        ctm.scale(scale, scale);
        if (Logcat.loggable) {
            Logcat.d(TAG, String.format("decode height:%s page:%s:%s,crop rect:%s, ctm:%s", height, pageW, pageH, rectF, ctm));
        }
        Object[] arr = {leftBound, topBound, height, rectF};
        return arr;
    }

    public static void render(Page page, Matrix ctm, Bitmap bitmap, int xOrigin, int leftBound, int topBound) {
        AndroidDrawDevice dev = new AndroidDrawDevice(bitmap, xOrigin + leftBound, topBound);
        page.run(dev, ctm, null);
        dev.close();
        dev.destroy();
    }

    public static RectF getCropRect(Bitmap bitmap) {
        //long start = SystemClock.uptimeMillis();
        ByteBuffer byteBuffer = PageCropper.create(bitmap.getByteCount()).order(ByteOrder.nativeOrder());
        bitmap.copyPixelsToBuffer(byteBuffer);
        //Log.d("test", String.format("%s,%s,%s,%s", bitmap.getWidth(), bitmap.getHeight(), (SystemClock.uptimeMillis() - start), rectF));

        //view: view:Point(1920, 1080) patchX:71 mss:6.260591 mZoomSize:Point(2063, 3066) zoom:1.0749608
        //test: 2063,3066,261,RectF(85.0, 320.0, 1743.0, 2736.0)
        return PageCropper.getCropBounds(byteBuffer, bitmap.getWidth(), bitmap.getHeight(), new RectF(0f, 0f, bitmap.getWidth(), bitmap.getHeight()));
    }
}

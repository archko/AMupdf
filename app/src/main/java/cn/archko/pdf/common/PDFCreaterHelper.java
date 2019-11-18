package cn.archko.pdf.common;

import android.os.AsyncTask;

import com.artifex.mupdf.fitz.Buffer;
import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Image;
import com.artifex.mupdf.fitz.PDFDocument;
import com.artifex.mupdf.fitz.PDFObject;
import com.artifex.mupdf.fitz.PDFPage;
import com.artifex.mupdf.fitz.Rect;

import cn.archko.pdf.common.Logcat;
import cn.archko.pdf.utils.FileUtils;
import cn.archko.pdf.utils.Utils;

/**
 * @author: archko 2018/12/21 :1:03 PM
 */
public class PDFCreaterHelper {

    public static final String OPTS = "compress-images;compress;incremental;linearize;pretty;compress-fonts";
    PDFDocument mDocument;
    String imgname = "DCIM/Camera/IMG_20181221_125752.jpg";
    String filename = "test.pdf";

    public PDFCreaterHelper() {
    }

    public void save() {
        Utils.execute(false, new AsyncTask<String, String, String>() {
            @Override
            protected String doInBackground(String... params) {
                try {
                    mDocument = new PDFDocument();
                    mDocument.addObject(mDocument.newString("test pdf"));
                    PDFObject pdfObject = mDocument.newString("test2.pdf");
                    Buffer buffer = new Buffer();
                    buffer.writeLine("test2");

                    Rect mediabox = new Rect(0, 0, 1000, 1000);
                    PDFObject pdfPage = mDocument.addPage(mediabox, 0, pdfObject, buffer);
                    //mDocument.insertPage(0, pdfPage);
                    Image image = new Image(FileUtils.getStoragePath(imgname));
                    mDocument.addImage(image);
                    //int save = mDocument.save(FileUtils.getStoragePath(filename), OPTS);
                    //mDocument = (PDFDocument) PDFDocument.openDocument(FileUtils.getStoragePath(filename));
                    Logcat.d(String.format("%s,%s,%s", 0, mDocument.toString(), mDocument.countPages()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
    }

}

package cn.archko.pdf.entity;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

/**
 * @author: archko 2019/11/30 :3:18 PM
 */
public class BitmapBean {
    public int index;
    public Bitmap bitmap;
    public float width;
    public float height;

    public BitmapBean(Bitmap bitmap, int index) {
        this(bitmap, index, bitmap.getWidth(), bitmap.getHeight());
    }

    public BitmapBean(Bitmap bitmap, float width, float height) {
        this(bitmap, 0, width, height);
    }

    public BitmapBean(Bitmap bitmap, int index, float width, float height) {
        this.bitmap = bitmap;
        this.index = index;
        this.width = width;
        this.height = height;
    }

    @NonNull
    @Override
    public String toString() {
        return "FileBean{" +
                "index:" + index +
                ",bitmap:" + bitmap +
                '}';
    }
}

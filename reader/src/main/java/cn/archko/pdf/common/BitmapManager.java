package cn.archko.pdf.common;

import android.graphics.Bitmap;

import java.lang.ref.SoftReference;

import cn.archko.pdf.entity.BitmapBean;

/**
 * bitmap cache
 * cache bitmap in memory
 *
 * @author: archko 2018/7/26 :10:49
 */
public class BitmapManager {

    private final int COUNT = 4;
    private SoftReference<BitmapBean>[] bitmaps = new SoftReference[COUNT];
    private int mCurrIndex = 0;
    private int hitCount = 0;

    public Bitmap getBitmap(int pageNumber) {
        for (int i = 0; i < COUNT; i++) {
            if (bitmaps[i] != null
                    && bitmaps[i].get() != null
                    && bitmaps[i].get().index == pageNumber) {
                hitCount++;
                //if (Logcat.loggable) {
                //    Logcat.d("hitCount:" + hitCount);
                //}
                return bitmaps[i].get().bitmap;
            }
        }
        //if (Logcat.loggable) {
        //    Logcat.d(String.format("miss:%s", pageNumber));
        //}
        return null;
    }

    public void setBitmap(int pageNumber, Bitmap bitmap) {
        boolean hasExist = false;
        for (int i = 0; i < COUNT; i++) {
            if (bitmaps[i] != null
                    && bitmaps[i].get() != null
                    && bitmaps[i].get().index == pageNumber) {
                hasExist = true;
                bitmaps[i].get().bitmap = bitmap;
                if (Logcat.loggable) {
                    Logcat.d(String.format("override:%s", i));
                }
                break;
            }
        }
        if (!hasExist) {
            for (int i = 0; i < COUNT; i++) {
                if (bitmaps[i] == null) {
                    hasExist = true;
                    bitmaps[i] = new SoftReference<>(new BitmapBean(bitmap, pageNumber));
                    //if (Logcat.loggable) {
                    //    Logcat.d(String.format("add new one:%s", i));
                    //}
                    return;
                }
            }
            if (!hasExist) {
                BitmapBean oldPageNumber = null;
                if (bitmaps[mCurrIndex] != null) {
                    oldPageNumber = bitmaps[mCurrIndex].get();
                }
                bitmaps[mCurrIndex] = new SoftReference<>(new BitmapBean(bitmap, pageNumber));

                //if (Logcat.loggable) {
                //    Logcat.d(String.format("is full:old:%s,new:%s, %s", oldPageNumber, bitmaps[mCurrIndex], mCurrIndex));
                //}
                mCurrIndex++;
                if (mCurrIndex >= COUNT) {
                    mCurrIndex = 0;
                }
            }
        }
    }

    public void recycle() {
        for (int i = 0; i < COUNT; i++) {
            if (bitmaps[i] != null
                    && bitmaps[i].get() != null
                    && bitmaps[i].get().bitmap != null) {
                bitmaps[i].get().bitmap.recycle();
                bitmaps[i] = null;
            }
        }
    }

    public void clear() {
        for (int i = 0; i < COUNT; i++) {
            if (bitmaps[i] != null) {
                if (bitmaps[i].get() != null && bitmaps[i].get().bitmap != null) {
                    BitmapPool.getInstance().release(bitmaps[i].get().bitmap);
                }
                bitmaps[i] = null;
            }
        }
        mCurrIndex = 0;
    }

    public static Bitmap createBitmap(int width, int height) {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    }
}

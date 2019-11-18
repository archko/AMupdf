package cn.archko.pdf.common;

import android.graphics.Bitmap;

/**
 * bitmap cache
 * cache bitmap in memory
 *
 * @author: archko 2018/7/26 :10:49
 */
public class BitmapManager {

    private final int COUNT = 3;
    private Bitmap[] bitmaps = new Bitmap[COUNT];
    private int[] index = new int[COUNT];
    private int mCurrIndex = 0;
    private int hitCount = 0;

    public Bitmap getBitmap(int pageNumber) {
        for (int i = 0; i < COUNT; i++) {
            if (index[i] == pageNumber) {
                hitCount++;
                if (Logcat.loggable) {
                    Logcat.d("hitCount:" + hitCount);
                }
                return bitmaps[i];
            }
        }
        if (Logcat.loggable) {
            Logcat.d(String.format("miss:%s, %s", pageNumber, index));
        }
        return null;
    }

    public void setBitmap(int pageNumber, Bitmap bitmap) {
        boolean hasExist = false;
        for (int i = 0; i < COUNT; i++) {
            if (index[i] == pageNumber) {
                hasExist = true;
                bitmaps[i] = bitmap;
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
                    bitmaps[i] = bitmap;
                    index[i] = pageNumber;
                    if (Logcat.loggable) {
                        Logcat.d(String.format("add new one:%s", i));
                    }
                    return;
                }
            }
            if (!hasExist) {
                bitmaps[mCurrIndex] = bitmap;
                int oldPageNumber = index[mCurrIndex];
                index[mCurrIndex] = pageNumber;

                if (Logcat.loggable) {
                    Logcat.d(String.format("is full:old:%s,new:%s, %s", oldPageNumber, pageNumber, mCurrIndex));
                }
                mCurrIndex++;
                if (mCurrIndex >= COUNT) {
                    mCurrIndex = 0;
                }
            }
        }
    }

    public void recycle() {
        for (int i = 0; i < COUNT; i++) {
            if (bitmaps[i] != null) {
                bitmaps[i].recycle();
                bitmaps[i] = null;
            }
        }
    }

    public void clear() {
        for (int i = 0; i < COUNT; i++) {
            if (bitmaps[i] != null) {
                bitmaps[i] = null;
            }
            index[i] = -1;
        }
        mCurrIndex = 0;
    }

    public static Bitmap createBitmap(int width, int height) {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    }
}

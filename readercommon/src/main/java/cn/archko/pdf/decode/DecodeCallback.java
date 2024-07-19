package cn.archko.pdf.decode;

import android.graphics.Bitmap;

/**
 * @author: archko 2019/12/25 :10:18 下午
 */
public interface DecodeCallback {
    void decodeComplete(Bitmap bitmap, DecodeParam param);
    boolean shouldRender(int index, DecodeParam param);
}
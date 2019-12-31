package cn.archko.pdf.common;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;

/**
 * @author: wushuyong 2019/12/25 :15:54
 */
public class BitmapCache {

    public static BitmapCache getInstance() {
        return Factory.instance;
    }

    private static final class Factory {

        private static final BitmapCache instance = new BitmapCache();
    }

    private BitmapCache() {
    }

    private LruCache<Object, Bitmap> cacheKt = new RecycleLruCache(8);

    public LruCache<Object, Bitmap> getCache() {
        return cacheKt;
    }

    public void clear() {
        cacheKt.evictAll();
    }

    public void addBitmap(Object key, Bitmap val) {
        cacheKt.put(key, val);
    }

    public Bitmap getBitmap(Object key) {
        return cacheKt.get(key);
    }

    private static class RecycleLruCache extends LruCache<Object, Bitmap> {

        public RecycleLruCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected void entryRemoved(boolean evicted, @NonNull Object key, @NonNull Bitmap oldValue, @Nullable Bitmap newValue) {
            BitmapPool.getInstance().release(oldValue);
        }
    }
}

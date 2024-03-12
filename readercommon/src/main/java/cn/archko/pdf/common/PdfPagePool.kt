package cn.archko.pdf.common

import android.graphics.Bitmap
import android.util.SparseArray
import java.util.LinkedList
import java.util.Queue

/**
 * @author: archko 2024/3/12 :11:51
 */
class PdfPagePool {

    private val mMaxPoolSizeInBytes = 100 * 1024 * 1024

    private var mPoolSizeInBytes = 0

    private val mPool: SparseArray<Bitmap?> = SparseArray()

    private val mRemoveQueue: Queue<Int> = LinkedList()

    fun put(position: Int, bitmap: Bitmap) {
        if (mPool[position] == null) {
            while (mPoolSizeInBytes > mMaxPoolSizeInBytes) {
                removeLast()
            }

            mRemoveQueue.offer(position)

            mPool[position] = bitmap
            mPoolSizeInBytes += bitmap.byteCount
        }
    }

    fun get(position: Int): Bitmap? {
        return mPool[position]
    }

    fun remove(position: Int) {
        mPool.remove(position)
    }

    fun clear() {
        mPool.clear()
        mRemoveQueue.clear()
        mPoolSizeInBytes = 0
    }

    private fun removeLast() {
        mRemoveQueue.poll()?.let { removePos ->
            mPoolSizeInBytes -= mPool[removePos]!!.byteCount
            mPool.remove(removePos)
        }
    }

    fun removeCache(start: Int, end: Int) {
        for (i in start until end) {
            remove(i)
        }
    }
}
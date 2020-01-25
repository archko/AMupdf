package cn.archko.pdf.entity;

import java.io.File;
import java.io.Serializable;
import java.util.Comparator;

import cn.archko.pdf.utils.FileUtils;

/**
 * @author: archko 2014/4/17 :16:27
 */
public class BookProgress implements Serializable, Comparator<BookProgress> {

    public int _id; //db id
    /**
     * 索引
     */
    public int index;
    /**
     * 文件路径,不是全路径,是除去/sdcard/这部分的路径.
     */
    public String path;
    /**
     * 文件名.包含扩展名
     */
    public String name;
    public String ext;
    public String md5;
    public int pageCount;
    public long size;
    public long firstTimestampe;
    public long lastTimestampe;
    public int readTimes;
    /**
     * 进度0-100,not used
     */
    public int progress = 0;
    public int page;
    public float zoomLevel;
    public int rotation;
    public int offsetX;
    public int offsetY;
    /**
     * 2.5.9 add auto crop,0:autocrop,1:no crop, 2:manunal crop
     */
    public int autoCrop = 0;
    /**
     * 3.2.0 add textreflow:0,no reflow mode,1,reflow mode
     */
    public int reflow = 0;
    //3.4.0 add isFavorited: 0,not in favorities,1,is in favorities
    public int isFavorited = 0;
    public int inRecent = 0;    //0:in recent,-1:not in recent,-2:all

    public static final int IN_RECENT = 0;
    public static final int NOT_IN_RECENT = -1;
    public static final int ALL = -2;

    public BookProgress() {
    }

    public BookProgress(String path) {
        index = 0;
        this.path = path;

        File file = new File(FileUtils.getStoragePath(path));
        if (file.exists()) {
            size = file.length();
            ext = FileUtils.getExtension(file);
            name = file.getName();
        } else {
            size = 0;
            name = path;
        }
        firstTimestampe = System.currentTimeMillis();
        lastTimestampe = System.currentTimeMillis();
        readTimes = 1;
    }

    /*public BookProgress(int _id, int index, String path, String name, String ext, String md5, int pageCount,
                        long size, long firstTimestampe, long lastTimestampe, int readTimes, int progress,
                        int page, float zoomLevel, int rotation, int offsetX, int offsety, int autoCrop, int reflow) {
        this(_id, index, path, name, ext, md5, pageCount, size, firstTimestampe, lastTimestampe,
                readTimes, progress, page, zoomLevel, rotation, offsetX, offsety, autoCrop, reflow, 0, 0);
    }*/

    public BookProgress(int _id, int index, String path, String name, String ext, String md5, int pageCount,
                        long size, long firstTimestampe, long lastTimestampe, int readTimes, int progress,
                        int page, float zoomLevel, int rotation, int offsetX, int offsety, int autoCrop, int reflow, int isFavorited, int inRecent) {
        this._id = _id;
        this.index = index;
        this.path = path;
        this.name = name;
        this.ext = ext;
        this.md5 = md5;
        this.pageCount = pageCount;
        this.size = size;
        this.firstTimestampe = firstTimestampe;
        this.lastTimestampe = lastTimestampe;
        this.readTimes = readTimes;
        this.progress = progress;
        this.page = page;
        this.zoomLevel = zoomLevel;
        this.rotation = rotation;
        this.offsetX = offsetX;
        this.offsetY = offsety;
        this.autoCrop = autoCrop;
        this.reflow = reflow;
        this.isFavorited = isFavorited;
        this.inRecent = inRecent;
    }

    @Override
    public String toString() {
        return "BookProgress{" +
                "_id=" + _id +
                ", index=" + index +
                ", name='" + name + '\'' +
                ", pageCount=" + pageCount +
                ", size=" + size +
                ", readTimes=" + readTimes +
                ", progress=" + progress +
                ", page=" + page +
                ", firstTimestampe=" + firstTimestampe +
                ", lastTimestampe=" + lastTimestampe +
                ", autoCrop=" + autoCrop +
                ", reflow=" + reflow +
                ", isFavorited=" + isFavorited +
                ", inRecent=" + inRecent +
                ", ext='" + ext + '\'' +
                ", md5='" + md5 + '\'' +
                ", path='" + path + '\'' +
                ", zoomLevel=" + zoomLevel +
                ", rotation=" + rotation +
                ", offsetX=" + offsetX +
                ", offsetY=" + offsetY +
                '}';
    }

    @Override
    public int compare(BookProgress lhs, BookProgress rhs) {
        if (lhs.lastTimestampe > rhs.lastTimestampe) {    //时间大的放前面
            return -1;
        } else if (lhs.lastTimestampe < rhs.lastTimestampe) {
            return 1;
        }
        return 0;
    }
}

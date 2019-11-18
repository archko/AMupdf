package cn.archko.pdf.entity;

import java.io.File;
import java.io.Serializable;

import cn.archko.pdf.utils.FileUtils;

public class FileBean implements Serializable, Cloneable {

    private String label = null;
    private File file = null;
    private boolean isDirectory = false;
    public static final int NORMAL = 0;
    public static final int HOME = 1;
    public static final int RECENT = 2;
    public static final int FAVORITE = 3;
    /**
     * bean type.
     */
    private int type = NORMAL;
    private BookProgress mBookProgress;

    public BookProgress getBookProgress() {
        return mBookProgress;
    }

    public void setBookProgress(BookProgress bookProgress) {
        this.mBookProgress = bookProgress;
    }

    public FileBean(int type, File file, String label) {
        this.file = file;
        this.label = file.getName();
        this.isDirectory = file.isDirectory();
        this.type = type;
        this.label = label;

        if (!isDirectory) {
            if (null == mBookProgress) {
                mBookProgress = new BookProgress(FileUtils.getRealPath(file.getAbsolutePath()));
            }
        }
    }

    public FileBean(int type, File file, Boolean showPDFExtension) {
        this(type, file, getLabel(file, showPDFExtension));
    }

    public FileBean(int type, String label) {
        this.type = type;
        this.label = label;
    }

    private static String getLabel(File file, boolean showPDFExtension) {
        String label = file.getName();

        if (!showPDFExtension && label.length() > 4 && !file.isDirectory()
            /*&& label.substring(label.length()-4, label.length()).equalsIgnoreCase(".pdf")*/) {
            return label.substring(0, label.length() - 4);
        } else {
            return label;
        }
    }

    public int getType() {
        return this.type;
    }

    public File getFile() {
        return this.file;
    }

    public long getFileSize() {
        if (null != mBookProgress) {
            return mBookProgress.size;
        }
        return getFile() != null ? getFile().length() : 0;
    }

    public String getLabel() {
        return this.label;
    }

    public boolean isDirectory() {
        return this.isDirectory;
    }

    public boolean isUpFolder() {
        return this.isDirectory && this.label.equals("..");
    }

    @Override
    public FileBean clone() {
        try {
            return (FileBean) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String toString() {
        return "FileBean{" +
                ", isDirectory=" + isDirectory +
                ", type=" + type +
                ", mBookProgress=" + mBookProgress +
                '}';
    }
}

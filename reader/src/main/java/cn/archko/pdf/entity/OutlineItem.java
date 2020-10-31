package cn.archko.pdf.entity;

import java.io.Serializable;

import cn.archko.pdf.tree.RvTree;

/**
 * @author: wushuyong 2020/10/31 :11:07 AM
 */
public class OutlineItem implements RvTree, Serializable {
    private int id;
    private int pid;
    private String title;
    private int page;
    private int resId;

    public OutlineItem(int id, int pid, String title) {
        this.id = id;
        this.pid = pid;
        this.title = title;
    }

    @Override
    public long getNid() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public long getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    @Override
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getId() {
        return id;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getResId() {
        return resId;
    }

    public void setResId(int resId) {
        this.resId = resId;
    }

    @Override
    public int getImageResId() {
        return 0;
    }
}

package cn.archko.pdf.tree;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * @author: archko 2020/10/29 :15:30
 */
public class Tree implements Serializable {

    public int level = 0;
    public String node;
    public String page;
    public Tree parent;
    public Boolean tag = false;
    public ArrayList<Tree> child = new ArrayList<>();

    public Tree(String node, Tree parent, int level) {
        this(node, parent, level, null);
    }

    public Tree(String node, Tree parent, int level, String page) {
        this.node = node;
        this.parent = parent;
        this.level = level;
        this.page = page;
    }

    public void addChild(Tree node) {
        child.add(node);
    }
}

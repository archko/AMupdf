package cn.archko.pdf.common

import android.app.Activity
import cn.archko.pdf.mupdf.MupdfDocument
import cn.archko.pdf.tree.Tree
import com.artifex.mupdf.fitz.Outline
import com.artifex.mupdf.viewer.OutlineActivity

/**
 * @author: archko 2018/12/15 :9:11
 */
class OutlineHelper public constructor(private var mupdfDocument: MupdfDocument?, private var activity: Activity?) {

    private var outline: Array<Outline>? = null
    private var items: ArrayList<OutlineActivity.Item>? = null

    fun getOutline(): ArrayList<OutlineActivity.Item> {
        if (null != items) {
            return items!!
        } else {
            items = ArrayList<OutlineActivity.Item>()
            flattenOutlineNodes(items!!, outline, " ")
        }
        return items!!
    }

    private fun flattenOutlineNodes(result: ArrayList<OutlineActivity.Item>, list: Array<Outline>?, indent: String) {
        for (node in list!!) {
            if (node.title != null) {
                val page = mupdfDocument?.pageNumberFromLocation(node)
                result.add(OutlineActivity.Item(indent + node.title, page!!))
            }
            if (node.down != null) {
                flattenOutlineNodes(result, node.down, "$indent  ")
            }
        }
    }

    private var _tree: Tree = Tree("title", null, 0)
    val tree: Tree?
        get() {
            _tree.child.clear();
            _tree.level = 0;
            addTreeNodes(_tree, outline, _tree.level + 1)
            return _tree
        }

    private fun addTreeNodes(parent: Tree, list: Array<Outline>?, level: Int) {
        for (node in list!!) {
            val newNode = Tree(node.title, parent, level)
            parent.addChild(newNode)
            if (node.title != null) {
                val page = mupdfDocument?.pageNumberFromLocation(node)
                newNode.page = page.toString();
            }
            if (node.down != null) {
                addTreeNodes(newNode, node.down, newNode.level + 1)
            }
        }
    }


    fun hasOutline(): Boolean {
        if (outline == null) {
            outline = mupdfDocument?.loadOutline()
        }
        return outline != null
    }
}

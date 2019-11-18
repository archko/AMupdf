package cn.archko.pdf.common

import android.app.Activity
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Outline
import com.artifex.mupdf.viewer.OutlineActivity

/**
 * @author: archko 2018/12/15 :9:11
 */
class OutlineHelper public constructor(private var doc: Document?, private var activity: Activity?) {

    private var outline: Array<Outline>? = null
    private var items: ArrayList<OutlineActivity.Item>? = null

    fun getOutline(): ArrayList<OutlineActivity.Item> {
        if (null != items) {
            return items!!
        } else {
            items = ArrayList<OutlineActivity.Item>()
            flattenOutlineNodes(items!!, outline, "")
        }
        return items!!
    }

    private fun flattenOutlineNodes(result: ArrayList<OutlineActivity.Item>, list: Array<Outline>?, indent: String) {
        for (node in list!!) {
            if (node.title != null) {
                val page = doc?.pageNumberFromLocation(doc?.resolveLink(node))
                result.add(OutlineActivity.Item(indent + node.title, page!!))
            }
            if (node.down != null) {
                flattenOutlineNodes(result, node.down, "$indent    ")
            }
        }
    }

    fun hasOutline(): Boolean {
        if (outline == null) {
            outline = doc?.loadOutline()
        }
        return outline != null
    }
}

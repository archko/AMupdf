package cn.archko.pdf.fragments

import cn.archko.pdf.decode.MupdfDocument
import cn.archko.pdf.entity.APage

interface MupdfListener {

    fun getPageCount(): Int
    fun getDocument(): MupdfDocument?
    fun getPageList(): List<APage>
}
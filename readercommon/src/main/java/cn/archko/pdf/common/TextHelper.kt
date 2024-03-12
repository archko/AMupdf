package cn.archko.pdf.common

import cn.archko.pdf.entity.ReflowBean
import cn.archko.pdf.utils.StreamUtils
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

object TextHelper {
    private const val READ_LINE = 10
    private const val READ_CHAR_COUNT = 400
    private const val TEMP_LINE = "\n"
    const val HEADER_HEIGHT = 60f

    @Throws(IOException::class)
    fun getFileCharsetName(fileName: String?): String {
        val inputStream: InputStream = FileInputStream(fileName)
        val head = ByteArray(3)
        inputStream.read(head)
        var charsetName = "GBK" //或GB2312，即ANSI
        if (head[0].toInt() == -1 && head[1].toInt() == -2) //0xFFFE
            charsetName = "UTF-16" else if (head[0].toInt() == -2 && head[1].toInt() == -1) //0xFEFF
            charsetName = "Unicode" //包含两种编码格式：UCS2-Big-Endian和UCS2-Little-Endian
        else if (head[0].toInt() == -27 && head[1].toInt() == -101 && head[2].toInt() == -98) charsetName =
            "UTF-8" //UTF-8(不含BOM)
        else if (head[0].toInt() == -17 && head[1].toInt() == -69 && head[2].toInt() == -65) charsetName =
            "UTF-8" //UTF-8-BOM
        inputStream.close()

        //System.out.println(code);
        return charsetName
    }

    @JvmStatic
    fun readString(path: String): List<ReflowBean> {
        var bufferedReader: BufferedReader? = null
        val reflowBeans = mutableListOf<ReflowBean>()
        var lineCount = 0
        val sb = StringBuilder()
        try {
            val fileCharsetName = getFileCharsetName(path)
            val isr = InputStreamReader(FileInputStream(path), fileCharsetName)
            bufferedReader = BufferedReader(isr)
            var temp: String?
            while (bufferedReader.readLine().also { temp = it } != null) {
                temp = temp?.trimIndent()
                if (null != temp && temp!!.length > READ_CHAR_COUNT + 40) {
                    //如果一行大于READ_CHAR_COUNT个字符,就应该把这一行按READ_CHAR_COUNT一个字符换行.
                    addLargeLine(temp!!, reflowBeans)
                } else {
                    if (lineCount < READ_LINE) {
                        sb.append(temp)
                        lineCount++
                    } else {
                        Logcat.d("======================:$sb")
                        reflowBeans.add(ReflowBean(sb.toString(), ReflowBean.TYPE_STRING))
                        sb.setLength(0)
                        lineCount = 0
                    }
                }
            }
            if (sb.isNotEmpty()) {
                reflowBeans.add(ReflowBean(sb.toString(), ReflowBean.TYPE_STRING))
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            StreamUtils.closeStream(bufferedReader)
        }

        return reflowBeans
    }

    private fun addLargeLine(temp: String, reflowBeans: MutableList<ReflowBean>) {
        val length = temp.length
        var start = 0;
        while (start < length) {
            var end = start + READ_CHAR_COUNT
            if (end > length) {
                end = length
            }
            val line = temp.subSequence(start, end)
            reflowBeans.add(ReflowBean(line.toString(), ReflowBean.TYPE_STRING))
            start = end
        }
    }


    fun isText(path: String?): Boolean {
        return path!!.endsWith(".txt", true)
                || path.endsWith(".log", true)
                || path.endsWith(".xml", true)
                || path.endsWith(".html", true)
                || path.endsWith(".xhtml", true)
                || path.endsWith(".js", true)
                || path.endsWith(".json", true)
    }

    fun isImage(path: String?): Boolean {
        return path!!.endsWith(".png", true)
                || path.endsWith(".jpg", true)
                || path.endsWith(".jpeg", true)
                || path.endsWith(".webp", true)
                || path.endsWith(".bmp", true)
    }

    fun isPdf(path: String?): Boolean {
        return path!!.endsWith(".pdf", true)
                || path.endsWith(".xps", true)
                || path.endsWith(".cbz", true)
                || path.endsWith(".png", true)
                || path.endsWith(".jpg", true)
                || path.endsWith(".jpeg", true)
                || path.endsWith(".jfif", true)
                || path.endsWith(".jfif-tbnl", true)
                || path.endsWith(".tif", true)
                || path.endsWith(".tiff", true)
                || path.endsWith(".epub", true)
                || path.endsWith(".mobi", true)
    }
}
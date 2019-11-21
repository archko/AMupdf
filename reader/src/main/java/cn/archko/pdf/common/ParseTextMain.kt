package cn.archko.pdf.common

import cn.archko.pdf.entity.ReflowBean
import cn.archko.pdf.utils.StreamUtils
import cn.archko.pdf.common.Logcat
import java.util.regex.Pattern
import kotlin.collections.ArrayList

/**
 * @author: archko 2019/2/18 :15:57
 */
class ParseTextMain private constructor() {

    private val txtParser: TxtParser

    private object Factory {
        val instance = ParseTextMain()
    }

    init {
        txtParser = TxtParser()
    }

    fun parseAsText(bytes: ByteArray): String {
        val content = String(bytes)
        return txtParser.parseAsText(content)
    }

    fun parseAsList(bytes: ByteArray, pageIndex: Int): List<ReflowBean> {
        val content = String(bytes)
        return txtParser.parseAsList(content, pageIndex)
    }

    fun parseXHtmlResult(bytes: ByteArray): String {
        val content = String(bytes)
        //return txtParser.parseTxt(UnicodeDecoder.parseXHtml(UnicodeDecoder.unEscape(content)))
        return content
    }

    class TxtParser {
        lateinit var path: String
        internal var joinLine = true
        internal var deleteEmptyLine = true

        constructor() {}

        constructor(path: String) {
            this.path = path
        }

        fun parseTxt() {
            /*String content = parse(StreamUtils.readStringAsList(path));
            String saveFile = "F:\\pdf3.text2";
            saveToFile(content, saveFile);*/
        }

        internal fun saveToFile(content: String, saveFile: String) {
            StreamUtils.saveStringToFile(content, saveFile)
        }

        internal fun parse(lists: List<String>): String {
            val sb = StringBuilder()
            var isImage = false
            for (s in lists) {
                val ss = s.trim { it <= ' ' }
                if (ss.length > 0) {
                    if (ss.startsWith(IMAGE_START_MARK)) {
                        isImage = true
                        sb.append("&nbsp;<br>")
                    }
                    if (!isImage) {
                        parseLine(ss, sb, isImage, MAX_PAGEINDEX)
                    } else {
                        sb.append(ss)
                    }

                    if (ss.endsWith("</p>")) {
                        isImage = false;
                    }
                }
            }
            return sb.toString()
        }

        fun parseAsText(content: String): String {
            //Logcat.d("parse:==>" + content);
            val sb = StringBuilder()
            val list = ArrayList<String>()
            var aChar: Char
            for (i in 0 until content.length) {
                aChar = content[i]
                if (aChar == '\n') {
                    list.add(sb.toString())
                    sb.setLength(0)
                } else {
                    sb.append(aChar)
                }
            }
            //Logcat.d("result=>>" + result);
            return parse(list)
        }

        /**
         * parse text as List<ReflowBean>
         */
        internal fun parseList(lists: List<String>, pageIndex: Int): List<ReflowBean> {
            val sb = StringBuilder()
            var isImage = false
            val reflowBeans = ArrayList<ReflowBean>()
            var reflowBean: ReflowBean? = null
            for (s in lists) {
                val ss = s.trim { it <= ' ' }
                if (ss.length > 0) {
                    if (Logcat.loggable) {
                        Logcat.longLog("text", ss)
                    }
                    if (ss.startsWith(IMAGE_START_MARK)) {
                        isImage = true
                        sb.setLength(0)
                        reflowBean = ReflowBean(null, ReflowBean.TYPE_STRING)
                        reflowBean.type = ReflowBean.TYPE_IMAGE
                        reflowBeans.add(reflowBean)
                    }
                    if (!isImage) {
                        if (null == reflowBean) {
                            reflowBean = ReflowBean(null, ReflowBean.TYPE_STRING)
                            reflowBeans.add(reflowBean)
                        }
                        parseLine(ss, sb, isImage, pageIndex)
                        reflowBean.data = sb.toString()
                    } else {
                        sb.append(ss)
                    }

                    if (ss.endsWith(IMAGE_END_MARK)) {
                        isImage = false
                        reflowBean?.data = sb.toString()
                        reflowBean = null
                        sb.setLength(0)
                    }
                }
            }

            if (Logcat.loggable) {
                Logcat.d("result", "length:${lists.size}")
                for (rb in reflowBeans) {
                    Logcat.longLog("result", rb.toString())
                }
            }
            return reflowBeans
        }

        private fun parseLine(ss: String, sb: StringBuilder, isImage: Boolean, pageIndex: Int) {
            var lineLength = ss.length
            if (lineLength > 4) {
                lineLength = 4;
            }
            val start = ss.substring(0, lineLength)
            val end = ss.substring(ss.length - 1)
            if (ss.length < LINE_LENGTH && pageIndex < MAX_PAGEINDEX) {
                if (!END_MARK.contains(end)) {
                    sb.append("<br>")
                }
            }

            var hasNewLine = false
            val find = START_MARK.matcher(start).find()
            //Logcat.d("find:$find")
            if (find) {
                hasNewLine = true
                sb.append("&nbsp;<br>")
            }

            sb.append(ss)
            if (END_MARK.contains(end) || PROGRAM_MARK.contains(end)) {
                if (!hasNewLine) {
                    sb.append("&nbsp;<br>")
                }
            } else {
                if (!isImage) {
                    if (isLetterDigitOrChinese(end)) {
                        sb.append("&nbsp;")
                    }
                    if (hasNewLine && lineLength < LINE_LENGTH && ss.substring(0, 1).matches("^[0-9]+$".toRegex())) {
                        sb.append("<br>")
                    }
                }
            }
        }

        fun parseAsList(content: String, pageIndex: Int): List<ReflowBean> {
            //Logcat.d("parse:==>" + content);
            val sb = StringBuilder()
            val list = ArrayList<String>()
            var aChar: Char
            for (i in 0 until content.length) {
                aChar = content[i]
                if (aChar == '\n') {
                    list.add(sb.toString())
                    sb.setLength(0)
                } else {
                    sb.append(aChar)
                }
            }
            //Logcat.d("result=>>" + result);
            return parseList(list, pageIndex)
        }

        fun isLetterDigitOrChinese(str: String): Boolean {
            val regex = "^[a-z0-9A-Z]+$"//其他需要，直接修改正则表达式就好
            return str.matches(regex.toRegex())
        }
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            val filepath = "F:\\ebook\\pdf.text"
            val txtParser = TxtParser(filepath)
            txtParser.parseTxt()
        }

        /**
         * 段落的开始字符可能是以下的:
         * 第1章,第四章.
         * 总结,小结,●,■,（2）,（3）
         */
        internal val START_MARK = Pattern.compile("(第\\w*[^章]章)|总结|小结|●|■")
        /**
         * 段落的结束字符可能是以下.
         */
        internal const val END_MARK = ".!?．！？。！?:：」？” "
        /**
         * 如果遇到的是代码,通常是以这些结尾
         */
        internal const val PROGRAM_MARK = "\\]>){};"
        /**
         * 解析pdf得到的文本,取出其中的图片
         */
        internal const val IMAGE_START_MARK = "<p><img"
        /**
         * 图片结束,jni中的特定结束符.
         */
        internal const val IMAGE_END_MARK = "</p>"
        /**
         * 一行如果不到20个字符,有可能是目录或是标题.
         */
        internal const val LINE_LENGTH = 15
        /**
         * 最大的页面是30页,如果是30页前的,一行小于25字,认为可能是目录.在这之后的,文本重排时不认为是目录.合并为一行.
         */
        internal const val MAX_PAGEINDEX = 30

        val instance: ParseTextMain
            get() = Factory.instance
    }
}

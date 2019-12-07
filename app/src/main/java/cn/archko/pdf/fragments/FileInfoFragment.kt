package cn.archko.pdf.fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import cn.archko.mupdf.R
import cn.archko.pdf.App
import cn.archko.pdf.common.ImageLoader
import cn.archko.pdf.common.RecentManager
import cn.archko.pdf.entity.BookProgress
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.listeners.DataListener
import cn.archko.pdf.utils.DateUtil
import cn.archko.pdf.utils.FileUtils
import cn.archko.pdf.utils.Utils
import com.artifex.mupdf.fitz.Document
import com.umeng.analytics.MobclickAgent
import java.math.BigDecimal

/**
 * @author: archko 2016/1/16 :14:34
 */
class FileInfoFragment : DialogFragment() {

    var mEntry: FileBean? = null
    lateinit var mLocation: TextView
    lateinit var mFileName: TextView
    lateinit var mFileSize: TextView
    //lateinit var mLastModified: TextView
    lateinit var mLastReadLayout: View
    lateinit var mLastRead: TextView
    lateinit var mReadCount: TextView
    lateinit var mPageCount: TextView
    lateinit var mProgressBar: ProgressBar
    lateinit var mIcon: ImageView
    var bookProgress: BookProgress? = null
    var mDataListener: DataListener? = null

    public fun setListener(dataListener: DataListener?) {
        mDataListener = dataListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var themeId = android.R.style.Theme_Holo_Light_Dialog
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            themeId = android.R.style.Theme_Material_Light_Dialog;
        }
        setStyle(DialogFragment.STYLE_NORMAL, themeId)
    }

    override fun onResume() {
        super.onResume()
        MobclickAgent.onPageStart(TAG);
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPageEnd(TAG);
    }

    override fun setArguments(args: Bundle?) {
        super.setArguments(args)
        if (null != args) {
            mEntry = args.getSerializable(FILE_LIST_ENTRY) as FileBean
            bookProgress = mEntry!!.bookProgress
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.detail_book_info, container, false)
        mLocation = view.findViewById<TextView>(R.id.location)
        mFileName = view.findViewById<TextView>(R.id.fileName)
        mFileSize = view.findViewById<TextView>(R.id.fileSize)
        mLastReadLayout = view.findViewById(R.id.lay_last_read)
        mLastRead = view.findViewById<TextView>(R.id.lastRead)
        mReadCount = view.findViewById(R.id.readCount)
        mProgressBar = view.findViewById<ProgressBar>(R.id.progressbar)
        //mLastModified = view.findViewById<TextView>(R.id.lastModified)
        mPageCount = view.findViewById<TextView>(R.id.pageCount)
        mIcon = view.findViewById<ImageView>(R.id.icon)
        var button = view.findViewById<Button>(R.id.btn_cancel)
        button.setOnClickListener { this@FileInfoFragment.dismiss() }
        button = view.findViewById<Button>(R.id.btn_ok)
        button.setOnClickListener {
            read()
        }

        dialog?.setTitle(R.string.menu_info)

        return view
    }

    private fun read() {
        this@FileInfoFragment.dismiss()
        mDataListener?.onSuccess(mEntry)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (null == mEntry || mEntry!!.file == null) {
            Toast.makeText(activity, "file is null.", Toast.LENGTH_LONG).show()
            dismiss()
            return
        }

        val file = mEntry!!.file
        mLocation.text = FileUtils.getDir(file)
        mFileName.text = file.name
        mFileSize.text = Utils.getFileSize(mEntry!!.fileSize)

        if (null == bookProgress || bookProgress?.pageCount == 0) {
            val recentManager = RecentManager.getInstance().recentTableManager
            try {
                bookProgress = recentManager.getProgress(file.name, BookProgress.ALL)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (null == bookProgress) {
                bookProgress = BookProgress(FileUtils.getRealPath(file.absolutePath))
            }
        }

        showProgress(bookProgress!!)

        showIcon(file.path)

        //mLastModified.text = DateUtil.formatTime(file.lastModified(), DateUtil.TIME_FORMAT_TWO)
    }

    private fun showIcon(path: String) {
        ImageLoader.getInstance().loadImage(path, 0, 1.0f, App.getInstance().screenWidth, mIcon);
    }

    private fun setPage() {
        mPageCount.setText(String.format("%s/%s", bookProgress!!.page, bookProgress!!.pageCount))
        if (bookProgress?.pageCount == 0) {
            loadBook()
        }
    }

    private fun loadBook() {
        try {
            val core: Document? = Document.openDocument(mEntry!!.file.path)
            bookProgress?.pageCount = core!!.countPages()
            setPage()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showProgress(progress: BookProgress) {
        if (null != bookProgress && bookProgress!!.pageCount > 0) {
            mLastReadLayout.visibility = View.VISIBLE

            var text = DateUtil.formatTime(progress.firstTimestampe, DateUtil.TIME_FORMAT_TWO)
            val percent = progress.page * 100f / progress.pageCount
            val b = BigDecimal(percent.toDouble())
            text += "       " + b.setScale(2, BigDecimal.ROUND_HALF_UP).toFloat() + "%"
            mLastRead.text = text
            mProgressBar.max = progress.pageCount
            mProgressBar.progress = progress.page

            mReadCount.text = progress.readTimes.toString()
        } else {
            mLastReadLayout.visibility = View.GONE
        }

        setPage()
    }

    companion object {

        val TAG = "FileInfoFragment"
        val FILE_LIST_ENTRY = "FILE_LIST_ENTRY"

        fun showInfoDialog(activity: FragmentActivity?, entry: FileBean, dataListener: DataListener?) {
            val ft = activity?.supportFragmentManager?.beginTransaction()
            val prev = activity?.supportFragmentManager?.findFragmentByTag("diaLog")
            if (prev != null) {
                ft?.remove(prev)
            }
            ft?.addToBackStack(null)

            val fileInfoFragment = FileInfoFragment()
            val bundle = Bundle()
            bundle.putSerializable(FileInfoFragment.FILE_LIST_ENTRY, entry)
            fileInfoFragment.arguments = bundle
            fileInfoFragment.setListener(dataListener)
            fileInfoFragment.show(ft!!, "diaLog")
        }
    }
}

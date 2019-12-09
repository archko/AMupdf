package cn.archko.pdf.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.view.*
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import cn.archko.mupdf.R
import cn.archko.pdf.App
import cn.archko.pdf.activities.AMuPDFRecyclerViewActivity
import cn.archko.pdf.activities.ChooseFileFragmentActivity
import cn.archko.pdf.activities.PdfOptionsActivity
import cn.archko.pdf.adapters.BookAdapter
import cn.archko.pdf.common.*
import cn.archko.pdf.entity.BookProgress
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.listeners.DataListener
import cn.archko.pdf.listeners.OnItemClickListener
import cn.archko.pdf.utils.FileUtils
import com.artifex.mupdf.viewer.DocumentActivity
import com.github.barteksc.pdfviewer.PDFViewActivity
import com.jeremyliao.liveeventbus.LiveEventBus
import com.umeng.analytics.MobclickAgent
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.vudroid.pdfdroid.PdfViewerActivity
import java.io.File
import java.io.FileFilter
import java.util.*
import kotlin.collections.ArrayList

/**
 * @description:file browser
 *
 * @author: archko 11-11-17
 */
open class BrowserFragment : RefreshableFragment(), SwipeRefreshLayout.OnRefreshListener,
        PopupMenu.OnMenuItemClickListener {

    private var mCurrentPath: String? = null

    protected var mSwipeRefreshWidget: SwipeRefreshLayout? = null
    protected var pathTextView: TextView? = null
    protected var filesListView: RecyclerView? = null
    private var fileFilter: FileFilter? = null
    protected var fileListAdapter: BookAdapter? = null

    private val dirsFirst = true
    private var showExtension: Boolean? = false

    internal var mPathMap: MutableMap<String, Int> = HashMap()
    protected var mSelectedPos = -1
    internal var mScanner: ProgressScaner? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mCurrentPath = getHome()
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.action_set_as_home -> setAsHome()
        }
        return super.onOptionsItemSelected(menuItem)
    }

    fun setAsHome() {
        val edit = activity?.getSharedPreferences(ChooseFileFragmentActivity.PREF_TAG, 0)?.edit()
        edit?.putString(ChooseFileFragmentActivity.PREF_HOME, mCurrentPath)
        edit?.apply()
    }

    open fun onBackPressed(): Boolean {
        val path = Environment.getExternalStorageDirectory().absolutePath
        if (this.mCurrentPath != path && this.mCurrentPath != "/") {
            val upFolder = File(this.mCurrentPath!!).parentFile
            if (upFolder.isDirectory) {
                this.mCurrentPath = upFolder.absolutePath
                loadData()
                return true
            }
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        Logcat.d(TAG, ".onResume." + this)
        MobclickAgent.onPageStart(javaClass.name);
        val options = PreferenceManager.getDefaultSharedPreferences(App.getInstance())
        showExtension = options.getBoolean(PdfOptionsActivity.PREF_SHOW_EXTENSION, true)
    }

    override fun onPause() {
        super.onPause()
        Logcat.i(TAG, ".onPause." + this)
        MobclickAgent.onPageEnd(javaClass.name);
    }

    override fun onDestroy() {
        super.onDestroy()
        Logcat.i(TAG, ".onDestroy." + this)
    }

    override fun onDetach() {
        super.onDetach()
        Logcat.i(TAG, ".onDetach." + this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.list_book_choose, container, false)

        this.pathTextView = view.findViewById<TextView>(R.id.path)
        this.filesListView = view.findViewById(R.id.files)
        filesListView!!.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)

        mSwipeRefreshWidget = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh_widget) as SwipeRefreshLayout
        mSwipeRefreshWidget!!.setColorSchemeResources(R.color.text_border_pressed, R.color.text_border_pressed,
                R.color.text_border_pressed, R.color.text_border_pressed)
        mSwipeRefreshWidget!!.setOnRefreshListener(this)
        fileListAdapter = BookAdapter(activity as Context, itemClickListener)

        return view
    }

    override fun onRefresh() {
        loadData()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        this.filesListView?.adapter = this.fileListAdapter
        loadData()
    }

    override fun update() {
        if (!isResumed) {
            return
        }

        loadData()
    }

    open fun loadData() {
        if (null == fileFilter) {
            fileFilter = FileFilter { file ->
                //return (file.isDirectory() || file.getName().toLowerCase().endsWith(".pdf"));
                if (file.isDirectory)
                    return@FileFilter true
                val fname = file.name.toLowerCase()

                if (fname.endsWith(".pdf"))
                    return@FileFilter true
                if (fname.endsWith(".xps"))
                    return@FileFilter true
                if (fname.endsWith(".cbz"))
                    return@FileFilter true
                if (fname.endsWith(".png"))
                    return@FileFilter true
                if (fname.endsWith(".jpe"))
                    return@FileFilter true
                if (fname.endsWith(".jpeg"))
                    return@FileFilter true
                if (fname.endsWith(".jpg"))
                    return@FileFilter true
                if (fname.endsWith(".jfif"))
                    return@FileFilter true
                if (fname.endsWith(".jfif-tbnl"))
                    return@FileFilter true
                if (fname.endsWith(".tif"))
                    return@FileFilter true
                if (fname.endsWith(".tiff"))
                    return@FileFilter true
                if (fname.endsWith(".epub"))
                    return@FileFilter true
                if (fname.endsWith(".txt"))
                    return@FileFilter true
                false
            }
        }
        val fileList: ArrayList<FileBean> = ArrayList()
        this.pathTextView!!.text = this.mCurrentPath
        var entry: FileBean

        entry = FileBean(FileBean.HOME, resources.getString(R.string.go_home))
        fileList.add(entry)

        if (this.mCurrentPath != "/") {
            val upFolder = File(this.mCurrentPath!!).parentFile
            entry = FileBean(FileBean.NORMAL, upFolder, "..")
            fileList.add(entry)
        }

        val files = File(this.mCurrentPath!!).listFiles(this.fileFilter)

        if (files != null) {
            try {
                Arrays.sort(files, Comparator<File> { f1, f2 ->
                    if (f1 == null) throw RuntimeException("f1 is null inside sort")
                    if (f2 == null) throw RuntimeException("f2 is null inside sort")
                    try {
                        if (dirsFirst && f1.isDirectory != f2.isDirectory) {
                            if (f1.isDirectory)
                                return@Comparator -1
                            else
                                return@Comparator 1
                        }
                        return@Comparator f2.lastModified().compareTo(f1.lastModified())
                    } catch (e: NullPointerException) {
                        throw RuntimeException("failed to compare $f1 and $f2", e)
                    }
                })
            } catch (e: NullPointerException) {
                throw RuntimeException("failed to sort file list " + files + " for path " + this.mCurrentPath, e)
            }

            for (file in files) {
                entry = FileBean(FileBean.NORMAL, file, showExtension)
                fileList.add(entry)
            }
        }

        fileListAdapter!!.setData(fileList)
        if (null != mPathMap[mCurrentPath!!]) {
            val pos = mPathMap[mCurrentPath!!]
            if (pos!! < fileList.size) {
                //filesListView!!.setSelection(pos)
                (filesListView!!.layoutManager as LinearLayoutManager).scrollToPosition(pos)
            }
        }
        fileListAdapter!!.notifyDataSetChanged()
        mSwipeRefreshWidget!!.isRefreshing = false

        startGetProgress(fileList, mCurrentPath)
    }

    private fun startGetProgress(fileList: ArrayList<FileBean>?, currentPath: String?) {
        if (null == mScanner) {
            mScanner = ProgressScaner()
        }
        mScanner!!.startScan(fileList, currentPath, object : DataListener {
            override fun onSuccess(vararg args: Any?) {
                val path = args[0] as String
                if (!mCurrentPath.equals(path)) {
                    return
                }

                fileListAdapter!!.setData(args[1] as ArrayList<FileBean>)
                fileListAdapter!!.notifyDataSetChanged()
            }

            override fun onFailed(vararg args: Any?) {
            }
        })
    }

    private fun getHome(): String {
        val defaultHome = Environment.getExternalStorageDirectory().absolutePath
        var path: String? = activity?.getSharedPreferences(ChooseFileFragmentActivity.PREF_TAG, 0)!!.getString(ChooseFileFragmentActivity.PREF_HOME, null)
        if (null == path) {
            Toast.makeText(activity, resources.getString(R.string.toast_set_as_home), Toast.LENGTH_SHORT)
            path = defaultHome
        }
        if (path!!.length > 1 && path!!.endsWith("/")) {
            path = path.substring(0, path.length - 2)
        }

        val pathFile = File(path)

        if (pathFile.exists() && pathFile.isDirectory)
            return path
        else
            return defaultHome
    }

    fun pdfView(f: File) {
        Logcat.i(TAG, "post intent to open file " + f)
        if (f.absolutePath.endsWith("txt", true)) {
            Toast.makeText(this@BrowserFragment.context, "can't load f:${f.absolutePath}", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent()
        intent.setDataAndType(Uri.fromFile(f), "application/pdf")
        intent.setClass(activity!!, AMuPDFRecyclerViewActivity::class.java)
        intent.action = "android.intent.action.VIEW"
        activity?.startActivity(intent)
    }

    val itemClickListener: OnItemClickListener<FileBean> = object : OnItemClickListener<FileBean> {
        override fun onItemClick(view: View?, data: FileBean?, position: Int) {
            clickItem(position)
        }

        override fun onItemClick2(view: View?, data: FileBean?, position: Int) {
            clickItem2(position, view!!)
        }
    }

    private fun clickItem(position: Int) {
        val clickedEntry = fileListAdapter!!.data[position]
        val clickedFile: File?

        if (clickedEntry.type == FileBean.HOME) {
            clickedFile = File(getHome())
        } else {
            clickedFile = clickedEntry.file
        }

        if (null == clickedFile || !clickedFile.exists())
            return

        if (clickedFile.isDirectory) {
            var pos: Int = (filesListView!!.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            if (pos < 0) {
                pos = 0
            }
            mPathMap.put(mCurrentPath!!, pos)
            this@BrowserFragment.mCurrentPath = clickedFile.absolutePath
            loadData()

            var map = mapOf("type" to "dir", "name" to clickedFile.name)
            MobclickAgent.onEvent(activity, AnalysticsHelper.A_FILE, map)
        } else {
            var map = mapOf("type" to "file", "name" to clickedFile.name)
            MobclickAgent.onEvent(activity, AnalysticsHelper.A_FILE, map)
            pdfView(clickedFile)
        }
    }

    private fun clickItem2(position: Int, view: View) {
        val entry = this.fileListAdapter!!.data.get(position) as FileBean
        if (!entry.isDirectory && entry.type != FileBean.HOME) {
            mSelectedPos = position
            prepareMenu(view, entry)
            return
        }
        mSelectedPos = -1
    }

    //--------------------- popupMenu ---------------------

    /**
     * 初始化自定义菜单

     * @param anchorView 菜单显示的锚点View。
     */
    fun prepareMenu(anchorView: View, entry: FileBean) {
        val popupMenu = PopupMenu(activity, anchorView)

        onCreateCustomMenu(popupMenu)
        onPrepareCustomMenu(popupMenu, entry)
        //return showCustomMenu(anchorView);
        popupMenu.setOnMenuItemClickListener(this)
        popupMenu.show()
    }

    /**
     * 创建菜单项，供子类覆盖，以便动态地添加菜单项。

     * @param menuBuilder
     */
    fun onCreateCustomMenu(menuBuilder: PopupMenu) {
        /*menuBuilder.add(0, 1, 0, "title1");*/
        menuBuilder.menu.clear()
    }

    /**
     * 创建菜单项，供子类覆盖，以便动态地添加菜单项。

     * @param menuBuilder
     */
    fun onPrepareCustomMenu(menuBuilder: PopupMenu, entry: FileBean) {
        /*menuBuilder.add(0, 1, 0, "title1");*/
        if (entry.type == FileBean.HOME) {
            //menuBuilder.getMenu().add(R.string.set_as_home);
            return
        }

        menuBuilder.menu.add(0, mupdfContextMenuItem, 0, getString(R.string.menu_mupdf))
        menuBuilder.menu.add(0, bartekscViewContextMenuItem, 0, "barteksc Viewer")
        menuBuilder.menu.add(0, vudroidContextMenuItem, 0, getString(R.string.menu_vudroid))
        menuBuilder.menu.add(0, documentContextMenuItem, 0, "Mupdf new Viewer")
        menuBuilder.menu.add(0, otherContextMenuItem, 0, getString(R.string.menu_other))
        menuBuilder.menu.add(0, infoContextMenuItem, 0, getString(R.string.menu_info))

        if (entry.type == FileBean.RECENT) {
            menuBuilder.menu.add(0, removeContextMenuItem, 0, getString(R.string.menu_remove_from_recent))
        } else if (!entry.isDirectory && entry.type != FileBean.HOME) {
            if (entry.bookProgress?.isFavorited == 0) {
                menuBuilder.menu.add(0, deleteContextMenuItem, 0, getString(R.string.menu_delete))
            }
        }
        setFavoriteMenu(menuBuilder, entry)
    }

    fun setFavoriteMenu(menuBuilder: PopupMenu, entry: FileBean) {
        if (null == entry.bookProgress) {
            if (null != entry.file) {
                entry.bookProgress = BookProgress(FileUtils.getRealPath(entry.file.absolutePath))
            } else {
                return
            }
        }
        if (entry.bookProgress.isFavorited == 0) {
            menuBuilder.menu.add(0, addToFavoriteContextMenuItem, 0, getString(R.string.menu_add_to_fav))
        } else {
            menuBuilder.menu.add(0, removeFromFavoriteContextMenuItem, 0, getString(R.string.menu_remove_from_fav))
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        if (fileListAdapter!!.itemCount <= 0 || mSelectedPos == -1) {
            return true
        }
        val position = mSelectedPos
        val entry = fileListAdapter!!.data[position]
        if (item.itemId == deleteContextMenuItem) {
            Logcat.d(TAG, "delete:" + entry)
            MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, "delete")
            if (entry.type == FileBean.NORMAL && !entry.isDirectory) {
                entry.file.delete()
                update()
            }
            return true
        } else if (item.itemId == removeContextMenuItem) {
            if (entry.type == FileBean.RECENT) {
                MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, "remove")
                RecentManager.getInstance().removeRecentFromDb(entry.file.absolutePath)
                update()
            }
        } else {
            val clickedFile: File = entry.file

            if (null != clickedFile && clickedFile.exists()) {
                val uri = Uri.parse(clickedFile.absolutePath)
                val intent: Intent
                intent = Intent()
                intent.action = Intent.ACTION_VIEW
                intent.data = uri

                when (item.itemId) {
                    vudroidContextMenuItem -> {
                        var map = mapOf("type" to "vudroid", "name" to clickedFile.name)
                        MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)

                        intent.setClass(activity!!, PdfViewerActivity::class.java)
                        intent.data = Uri.fromFile(clickedFile)
                        startActivity(intent)
                    }
                    mupdfContextMenuItem -> {
                        var map = mapOf("type" to "AMuPDF", "name" to clickedFile.name)
                        MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)

                        intent.setClass(activity!!, AMuPDFRecyclerViewActivity::class.java)
                        startActivity(intent)
                    }
                    bartekscViewContextMenuItem -> {
                        var map = mapOf("type" to "barteksc", "name" to clickedFile.name)
                        MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)

                        intent.setClass(activity!!, PDFViewActivity::class.java)
                        startActivity(intent)
                    }
                    documentContextMenuItem -> {
                        var map = mapOf("type" to "Document", "name" to clickedFile.name)
                        MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)

                        intent.setClass(activity!!, DocumentActivity::class.java)
                        // API>=21: intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT); /* launch as a new document */
                        intent.setAction(Intent.ACTION_VIEW)
                        intent.setData(Uri.fromFile(clickedFile))
                        startActivity(intent)
                    }
                    otherContextMenuItem -> {
                        var map = mapOf("type" to "other", "name" to clickedFile.name)
                        MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)
                        var mimeType = "application/pdf"
                        val name = clickedFile.absolutePath;
                        if (name.endsWith("pdf", true)) {
                            mimeType = "application/pdf";
                        } else if (name.endsWith("epub", true)) {
                            mimeType = "application/epub+zip";
                        } else if (name.endsWith("cbz", true)) {
                            mimeType = "application/x-cbz";
                        } else if (name.endsWith("fb2", true)) {
                            mimeType = "application/fb2";
                        } else if (name.endsWith("txt", true)) {
                            mimeType = "text/plain";
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            //val mimeType = this@BrowserFragment.activity?.contentResolver?.getType(FileProvider.getUriForFile(getContext()!!, "cn.archko.mupdf.fileProvider", clickedFile))
                            intent.setDataAndType(FileProvider.getUriForFile(getContext()!!, "cn.archko.mupdf.fileProvider", clickedFile), mimeType);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            intent.addCategory(Intent.CATEGORY_DEFAULT);
                        } else {
                            //val mimeType = this@BrowserFragment.activity?.contentResolver?.getType(Uri.fromFile(clickedFile))
                            intent.setDataAndType(Uri.fromFile(clickedFile), mimeType)
                        }
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                    //================== ==================
                    infoContextMenuItem -> {
                        var map = mapOf("type" to "info", "name" to clickedFile.name)
                        MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)

                        showFileInfoDiaLog(entry)
                    }
                    addToFavoriteContextMenuItem -> {
                        var map = mapOf("type" to "addToFavorite", "name" to clickedFile.name)
                        MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)

                        favorite(entry, 1)
                    }
                    removeFromFavoriteContextMenuItem -> {
                        var map = mapOf("type" to "removeFromFavorite", "name" to clickedFile.name)
                        MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)

                        favorite(entry, 0)
                    }
                }
            }
        }
        return false
    }

    protected fun favorite(entry: FileBean, isFavorited: Int) {
        doAsync {
            try {
                val recentManager = RecentManager.getInstance().recentTableManager
                val filepath = FileUtils.getStoragePath(entry.bookProgress.path)
                val file = File(filepath)
                var bookProgress = recentManager.getProgress(file.name, BookProgress.ALL)
                if (null == bookProgress) {
                    if (isFavorited == 0) {
                        Logcat.w(TAG, "some error:$entry")
                        return@doAsync
                    }
                    bookProgress = BookProgress(FileUtils.getRealPath(file.absolutePath))
                    entry.bookProgress = bookProgress
                    entry.bookProgress.inRecent = BookProgress.NOT_IN_RECENT;
                    entry.bookProgress.isFavorited = isFavorited
                    Logcat.d(TAG, "add favorite entry:${entry.bookProgress}")
                    recentManager.addProgress(entry.bookProgress)
                } else {
                    entry.bookProgress = bookProgress
                    entry.bookProgress.isFavorited = isFavorited
                    Logcat.d(TAG, "update favorite entry:${entry.bookProgress}")
                    recentManager.updateProgress(entry.bookProgress)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            uiThread {
                postFavoriteEvent(entry, isFavorited)
            }
        }
    }

    fun postFavoriteEvent(entry: FileBean, isFavorited: Int) {
        if (isFavorited == 1) {
            LiveEventBus
                    .get(Event.ACTION_FAVORITED)
                    .post(entry)
        } else {
            LiveEventBus
                    .get(Event.ACTION_UNFAVORITED)
                    .post(entry)
        }
    }

    protected fun showFileInfoDiaLog(entry: FileBean) {
        FileInfoFragment.showInfoDialog(activity, entry, object : DataListener {
            override fun onSuccess(vararg args: Any?) {
                val fileEntry = args[0] as FileBean
                filesListView?.let { prepareMenu(it, fileEntry) }
            }

            override fun onFailed(vararg args: Any?) {
            }
        })
    }

    companion object {

        const val TAG = "BrowserFragment"

        protected const val deleteContextMenuItem = Menu.FIRST + 100
        protected const val removeContextMenuItem = Menu.FIRST + 101

        protected const val mupdfContextMenuItem = Menu.FIRST + 110
        //protected const val apvContextMenuItem = Menu.FIRST + 111
        protected const val vudroidContextMenuItem = Menu.FIRST + 112
        protected const val otherContextMenuItem = Menu.FIRST + 113
        protected const val infoContextMenuItem = Menu.FIRST + 114
        protected const val documentContextMenuItem = Menu.FIRST + 115
        protected const val addToFavoriteContextMenuItem = Menu.FIRST + 116
        protected const val removeFromFavoriteContextMenuItem = Menu.FIRST + 117
        protected const val bartekscViewContextMenuItem = Menu.FIRST + 118
    }
}

package cn.archko.pdf.activities

import android.content.Intent
import android.graphics.PointF
import android.os.Bundle
import android.os.SystemClock
import android.preference.PreferenceManager
import android.text.TextUtils
import android.util.SparseArray
import android.view.GestureDetector
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import cn.archko.pdf.R
import cn.archko.pdf.common.BitmapCache
import cn.archko.pdf.common.Event
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.PDFBookmarkManager
import cn.archko.pdf.common.PathFromUri
import cn.archko.pdf.common.SensorHelper
import cn.archko.pdf.common.StatusBarHelper
import cn.archko.pdf.entity.APage
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.mupdf.MupdfDocument
import cn.archko.pdf.utils.Utils
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author: archko 2016/5/9 :12:43
 */
abstract class MuPDFRecyclerViewActivity : AnalysticActivity() {

    protected val OUTLINE_REQUEST = 0
    protected var mPath: String? = null

    //protected lateinit var progressDialog: ProgressDialog

    protected var gestureDetector: GestureDetector? = null
    protected var pageNumberToast: Toast? = null

    protected var pdfBookmarkManager: PDFBookmarkManager? = null
    protected var sensorHelper: SensorHelper? = null
    protected var mMupdfDocument: MupdfDocument? = null
    protected var mPageSizes = SparseArray<APage>()

    protected var mReflow = false
    protected var mCrop: Boolean = true
    protected var isDocLoaded: Boolean = false

    protected var mDocumentView: FrameLayout? = null
    protected var viewController: AViewController? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //progressDialog = ProgressDialog(this)

        if (null != savedInstanceState) {
            mPath = savedInstanceState.getString("path", null)
        }

        parseIntent()

        Logcat.d("path:" + mPath!!)

        if (TextUtils.isEmpty(mPath)) {
            Toast.makeText(
                this@MuPDFRecyclerViewActivity,
                "error file path:$mPath",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        sensorHelper = SensorHelper(this)

        loadBookmark()
        initView()

        loadDoc()
    }

    open fun loadBookmark() {
        mCrop = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(PdfOptionsActivity.PREF_AUTOCROP, true)

        pdfBookmarkManager = PDFBookmarkManager()
        var autoCrop = 0
        if (!mCrop) {
            autoCrop = 1
        }
        pdfBookmarkManager!!.setStartBookmark(mPath, autoCrop)
        val bookmark = pdfBookmarkManager?.bookmarkToRestore
        bookmark?.let {
            mCrop = it.autoCrop == 0
            mReflow = it.reflow == 1
        }
    }

    open fun doLoadDoc() {
        //try {
        //progressDialog.setMessage("Loading menu")

        isDocLoaded = true
        //} catch (e: Exception) {
        //    e.printStackTrace()
        //    finish()
        //} finally {
        //    //progressDialog.dismiss()
        //}
    }

    private fun parseIntent() {
        if (TextUtils.isEmpty(mPath)) {
            val intent = intent

            //pos = getIntent().getIntExtra("pos", 0)
            if (Intent.ACTION_VIEW == intent.action) {
                val uri = getIntent().data
                mPath = getIntent().getStringExtra("path")
                if (TextUtils.isEmpty(mPath)) {
                    val path = PathFromUri.getFilePathByUri(this, uri)
                    mPath = path
                }
            } else {
                if (!TextUtils.isEmpty(getIntent().getStringExtra("path"))) {
                    mPath = getIntent().getStringExtra("path")
                }
            }
        }
        if (TextUtils.isEmpty(mPath)) {
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("path", mPath)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mPath = savedInstanceState.getString("path", null)
    }

    override fun onDestroy() {
        super.onDestroy()
        isDocLoaded = false
        LiveEventBus
            .get<String>(Event.ACTION_STOPPED)
            .post(null)
        mMupdfDocument?.destroy()
        //progressDialog.dismiss()
        BitmapCache.getInstance().clear()
    }

    open fun initView() {
        StatusBarHelper.hideSystemUI(this)
        StatusBarHelper.setStatusBarImmerse(window)
        setFullScreen()

        setContentView(R.layout.reader)
    }

    private fun setFullScreen() {
        window.requestFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }

    open fun getDocumentView(): View? {
        return null
    }

    open fun onSingleTap() {
        if (!isDocLoaded) {
            return
        }
        val pos = getCurrentPos()
        val pageText = (pos + 1).toString() + "/" + mMupdfDocument!!.countPages()
        if (pageNumberToast != null) {
            pageNumberToast!!.setText(pageText)
        } else {
            pageNumberToast =
                Toast.makeText(this@MuPDFRecyclerViewActivity, pageText, Toast.LENGTH_SHORT)
        }
        pageNumberToast!!.setGravity(Gravity.BOTTOM or Gravity.START, Utils.dipToPixel(15f), 0)
        pageNumberToast!!.show()
    }

    open fun onDoubleTap() {
        if (!isDocLoaded) {
            return
        }
    }

    open fun updateProgress(index: Int) {
    }
    //--------------------------------------

    override fun onResume() {
        super.onResume()

        sensorHelper?.onResume()
        val options = PreferenceManager.getDefaultSharedPreferences(this)
        if (options.getBoolean(PdfOptionsActivity.PREF_KEEP_ON, false)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        Logcat.d("onResume ")
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_IMMERSIVE
        }
    }

    override fun onPause() {
        super.onPause()

        sensorHelper?.onPause()
    }

    open fun getCurrentPos(): Int {
        return viewController?.getCurrentPos()!!
    }

    open fun getPassword(): String? {
        return null
    }

    //===========================================

    companion object {

        const val TAG = "MuPDFRecyclerViewActivity"
        const val TYPE_TITLE = 0
        const val TYPE_PROGRESS = 1
        const val TYPE_ZOOM = 2
        const val TYPE_CLOSE = 3
        const val TYPE_FONT = 4
        const val TYPE_SETTINGS = 5
    }

    open fun loadDoc() {
        //progressDialog.setMessage(mPath)
        //progressDialog.show()
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    var start = SystemClock.uptimeMillis()
                    mMupdfDocument = MupdfDocument(this@MuPDFRecyclerViewActivity)
                    mMupdfDocument?.newDocument(mPath, getPassword())
                    var res = true
                    mMupdfDocument?.let {
                        if (it.document.needsPassword()) {
                            res = it.document.authenticatePassword(getPassword())
                        }
                    }

                    val cp = mMupdfDocument!!.countPages()
                    Logcat.d(TAG, "open:" + (SystemClock.uptimeMillis() - start) + " cp:" + cp)

                    //val loc = mDocument!!.layout(mLayoutW, mLayoutH, mLayoutEM)

                    preparePageSize(cp)
                    Logcat.d(TAG, "open:end." + mPageSizes.size())
                    val mill = SystemClock.uptimeMillis() - start
                    if (mill < 500L) {
                        delay(500L - mill)
                    }
                    return@withContext true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return@withContext false
            }
            if (result) {
                doLoadDoc()
            } else {
                finish()
            }
        }
    }

    open fun getPageSize(pageNum: Int): APage? {
        val p = mMupdfDocument?.loadPage(pageNum) ?: return null

        Logcat.d(TAG, "open:getPageSize.$pageNum page:$p")
        val b = p.bounds
        val w = b.x1 - b.x0
        val h = b.y1 - b.y0
        val pointf = PointF(w, h)
        p.destroy()
        return APage(pageNum, pointf, 1.0f/*zoomModel!!.zoom*/, 0)
    }

    open fun preparePageSize(cp: Int) {
        val page = getPageSize(0)
        if (null != page) {
            for (i in 0 until cp) {
                val pointF = PointF(page.width, page.height)
                mPageSizes.put(i, APage(i, pointF, 1.0f, 0))
            }
        }
    }
}

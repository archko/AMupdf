package cn.archko.pdf.activities

import android.annotation.TargetApi
import android.app.ProgressDialog
import android.content.Intent
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.PointF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.preference.PreferenceManager
import android.text.TextUtils
import android.util.SparseArray
import android.view.*
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.R
import cn.archko.pdf.common.BitmapManager
import cn.archko.pdf.common.Event
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.SensorHelper
import cn.archko.pdf.entity.APage
import cn.archko.pdf.utils.Utils
import cn.archko.pdf.widgets.APDFView
import cn.archko.pdf.widgets.ViewerDividerItemDecoration
import com.artifex.mupdf.fitz.Document
import com.jeremyliao.liveeventbus.LiveEventBus
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import org.vudroid.core.events.ZoomListener
import org.vudroid.core.models.ZoomModel
import org.vudroid.core.multitouch.MultiTouchZoom

/**
 * @author: archko 2016/5/9 :12:43
 */
abstract class MuPDFRecyclerViewActivity : AnalysticActivity(), ZoomListener {

    protected var mPath: String? = null

    protected lateinit var mRecyclerView: RecyclerView
    protected lateinit var progressDialog: ProgressDialog

    protected var gestureDetector: GestureDetector? = null
    protected var pageNumberToast: Toast? = null

    protected var sensorHelper: SensorHelper? = null
    protected var mDocument: Document? = null
    protected val mPageSizes = SparseArray<APage>()
    protected var zoomModel: ZoomModel? = null
    protected var bitmapManager: BitmapManager? = null
    protected var multiTouchZoom: MultiTouchZoom? = null

    protected var autoCrop: Boolean = false
    protected var isDocLoaded: Boolean = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initView()
        progressDialog = ProgressDialog(this)

        if (null != savedInstanceState) {
            mPath = savedInstanceState.getString("path", null)
        }

        parseIntent()

        Logcat.d("path:" + mPath!!)

        if (TextUtils.isEmpty(mPath)) {
            toast("error file path:$mPath")
            return
        }
        sensorHelper = SensorHelper(this)

        autoCrop = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PdfOptionsActivity.PREF_AUTOCROP, false)

        loadDoc()
    }

    open fun doLoadDoc() {
        try {
            progressDialog.setMessage("Loading menu")
            bitmapManager = BitmapManager()

            mRecyclerView.adapter = PDFRecyclerAdapter()
            addGesture()

            isDocLoaded = true
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        } finally {
            progressDialog.dismiss()
        }
    }

    private fun parseIntent() {
        if (TextUtils.isEmpty(mPath)) {
            val intent = intent

            if (Intent.ACTION_VIEW == intent.action) {
                var uri = intent.data
                Logcat.d("URI to open is: " + uri)
                if (uri.toString().startsWith("content://")) {
                    var reason: String? = null
                    var cursor: Cursor? = null
                    try {
                        cursor = contentResolver.query(uri!!, arrayOf("_data"), null, null, null)
                        if (cursor!!.moveToFirst()) {
                            val str = cursor.getString(0)
                            if (str == null) {
                                reason = "Couldn't parse data in intent"
                            } else {
                                uri = Uri.parse(str)
                            }
                        }
                    } catch (e2: Exception) {
                        Logcat.d("Exception in Transformer Prime file manager code: " + e2)
                        reason = e2.toString()
                    } finally {
                        if (null != cursor) {
                            cursor.close()
                        }
                    }
                }
                var path: String? = Uri.decode(uri?.encodedPath)
                if (path == null) {
                    path = uri.toString()
                }
                mPath = path
            } else {
                if (!TextUtils.isEmpty(getIntent().getStringExtra("path"))) {
                    mPath = getIntent().getStringExtra("path")
                }
            }
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
                .get(Event.ACTION_STOPPED)
                .post(null)
        mRecyclerView.adapter = null
        mDocument?.destroy()
        bitmapManager?.recycle()
        progressDialog.dismiss()
    }

    open fun initView() {
        window.requestFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.reader)

        mRecyclerView = findViewById(R.id.recycler_view)//RecyclerView(this)
        with(mRecyclerView) {
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            isNestedScrollingEnabled = false
            layoutManager = LinearLayoutManager(this@MuPDFRecyclerViewActivity, LinearLayoutManager.VERTICAL, false)
            setItemViewCacheSize(0)

            addItemDecoration(ViewerDividerItemDecoration(this@MuPDFRecyclerViewActivity, LinearLayoutManager.VERTICAL))
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        updateProgress(getCurrentPos())
                    }
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                }
            })
        }

        initTouchParams()

        zoomModel = ZoomModel()
        initMultiTouchZoomIfAvailable(zoomModel)

        zoomModel?.toggleZoomControls()
    }

    open fun addGesture() {
        mRecyclerView.setOnTouchListener { v, event ->
            gestureDetector!!.onTouchEvent(event)
            multiTouchZoom?.run {
                if (onTouchEvent(event)) {
                    return@setOnTouchListener true
                }

                if (isResetLastPointAfterZoom()) {
                    //setLastPosition(ev)
                    setResetLastPointAfterZoom(false)
                }
            }
            false
        }
    }

    private fun initMultiTouchZoomIfAvailable(zoomModel: ZoomModel?) {
        try {
            multiTouchZoom = Class.forName("org.vudroid.core.multitouch.MultiTouchZoomImpl").getConstructor(ZoomModel::class.java).newInstance(zoomModel) as MultiTouchZoom
        } catch (e: Exception) {
            Logcat.d("Multi touch zoom is not available: $e")
        }

    }

    protected fun initTouchParams() {
        var margin = mRecyclerView.height
        if (margin <= 0) {
            margin = ViewConfiguration.get(this).scaledTouchSlop * 2
        } else {
            margin = (margin * 0.03).toInt()
        }
        gestureDetector = GestureDetector(this, object : GestureDetector.OnGestureListener {

            override fun onDown(e: MotionEvent): Boolean {
                return false
            }

            override fun onShowPress(e: MotionEvent) {

            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                return false
            }

            override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                return false
            }

            override fun onLongPress(e: MotionEvent) {

            }

            override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                return false
            }
        })

        val finalMargin = margin
        gestureDetector!!.setOnDoubleTapListener(object : GestureDetector.OnDoubleTapListener {

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val top = mRecyclerView.height / 4
                val bottom = mRecyclerView.height * 3 / 4

                if (e.y.toInt() < top) {
                    var scrollY = mRecyclerView.scrollY
                    scrollY -= mRecyclerView.height
                    mRecyclerView.scrollBy(0, scrollY + finalMargin)
                    return true
                } else if (e.y.toInt() > bottom) {
                    var scrollY = mRecyclerView.scrollY
                    scrollY += mRecyclerView.height
                    mRecyclerView.scrollBy(0, scrollY - finalMargin)
                    return true
                } else {
                    onSingleTap()
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                //mPageSeekBarControls?.toggleSeekControls()
                //zoomModel?.toggleZoomControls()
                onDoubleTap()
                return true
            }

            override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                return false
            }
        })
    }

    open fun onSingleTap() {
        if (!isDocLoaded) {
            return
        }
        val pos = getCurrentPos()
        val pageText = (pos + 1).toString() + "/" + mDocument!!.countPages()
        if (pageNumberToast != null) {
            pageNumberToast!!.setText(pageText)
        } else {
            pageNumberToast = Toast.makeText(this@MuPDFRecyclerViewActivity, pageText, Toast.LENGTH_SHORT)
        }
        pageNumberToast!!.setGravity(Gravity.BOTTOM or Gravity.START, Utils.dipToPixel(15f), 0)
        pageNumberToast!!.show()
    }

    open fun onDoubleTap() {
        if (!isDocLoaded) {
            return
        }
    }

    override fun zoomChanged(newZoom: Float, oldZoom: Float) {
        mRecyclerView.adapter?.notifyItemChanged(getCurrentPos())
    }

    override fun commitZoom() {
    }

    open fun updateProgress(index: Int) {
    }
    //--------------------------------------

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mRecyclerView.stopScroll()
        mRecyclerView.adapter?.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()

        sensorHelper?.onResume()
        val options = PreferenceManager.getDefaultSharedPreferences(this)
        if (options.getBoolean(PdfOptionsActivity.PREF_KEEP_ON, false)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        if (options.getBoolean(PdfOptionsActivity.PREF_FULLSCREEN, true)) {
            //getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        Logcat.d("onResume ")

        mRecyclerView.postDelayed(object : Runnable {
            override fun run() {
                mRecyclerView.adapter?.notifyDataSetChanged()
            }
        }, 250L)
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
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

    fun getCurrentPos(): Int {
        var position = (mRecyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        if (position < 0) {
            position = 0
        }
        return position;
    }

    //===========================================

    protected inner class PDFRecyclerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        var pos: Int = 0
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var pageSize: APage? = null
            if (mPageSizes.size() > pos) {
                pageSize = mPageSizes.get(pos)
                if (pageSize.targetWidth <= 0) {
                    Logcat.d(String.format("create:%s", mRecyclerView.measuredWidth));
                    pageSize.targetWidth = parent.width;
                }
            }
            val view = APDFView(parent.context, mDocument, pageSize, bitmapManager)
            var lp: RecyclerView.LayoutParams? = view.layoutParams as RecyclerView.LayoutParams?
            var width: Int = ViewGroup.LayoutParams.MATCH_PARENT
            var height: Int = ViewGroup.LayoutParams.MATCH_PARENT
            pageSize?.let {
                width = it.effectivePagesWidth
                height = it.effectivePagesHeight
            }
            //Logcat.d("create width:" + width + "==>" + mRecyclerView.measuredWidth + "==>" + pageSize!!.targetWidth);
            if (null == lp) {
                lp = RecyclerView.LayoutParams(width, height)
                view.layoutParams = lp
            } else {
                lp.width = width;
                lp.height = height;
            }
            val holder = PdfHolder(view)
            return holder
        }

        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
            pos = viewHolder.adapterPosition
            val pdfHolder = viewHolder as PdfHolder

            pdfHolder.onBind(position)
        }

        override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
            super.onViewRecycled(holder)
            val pdfHolder = holder as PdfHolder?

            pdfHolder?.view?.recycle()
        }

        override fun getItemCount(): Int {
            return mDocument!!.countPages()
        }

        inner class PdfHolder(internal var view: APDFView) : RecyclerView.ViewHolder(view) {
            fun onBind(position: Int) {
                val pageSize = mPageSizes.get(position)
                //Logcat.d(String.format("bind:position:%s,width:%s,%s", position, pageSize.targetWidth, mRecyclerView.measuredWidth));
                if (pageSize.targetWidth != mRecyclerView.measuredWidth) {
                    pageSize.targetWidth = mRecyclerView.measuredWidth
                }
                if (pageSize.targetWidth <= 0) {
                    return
                }
                view.setPage(pageSize, zoomModel!!.zoom, autoCrop)

                //Logcat.d("onBindViewHolder:$pos, view:${view}")
            }
        }

    }

    companion object {

        private const val TAG = "MuPDFRecyclerViewActivity"
        public const val TYPE_TITLE = 0
        public const val TYPE_PROGRESS = 1
        public const val TYPE_ZOOM = 2
        public const val TYPE_CLOSE = 3
        public const val TYPE_FONT = 4
        public const val TYPE_SETTINGS = 5
    }

    open fun loadDoc() {
        progressDialog.setMessage(mPath)
        progressDialog.show()
        val start = SystemClock.uptimeMillis()
        doAsync {
            var result = false
            try {
                //var start = SystemClock.uptimeMillis();
                mDocument = Document.openDocument(mPath)
                val cp = mDocument!!.countPages();
                //Logcat.d(TAG, "open:" + (SystemClock.uptimeMillis() - start))

                //val loc = mDocument!!.layout(mLayoutW, mLayoutH, mLayoutEM)

                for (i in 0 until cp) {
                    val pointF = getPageSize(i)
                    mPageSizes.put(i, pointF)
                }
                result = true
                val mill = SystemClock.uptimeMillis() - start
                if (mill < 500L) {
                    Thread.sleep(500L - mill)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            uiThread {
                if (result) {
                    doLoadDoc()
                } else {
                    finish()
                }
            }
        }

        //toast("loading:$mPath")
    }

    fun getPageSize(pageNum: Int): APage {
        val p = mDocument?.loadPage(pageNum)
        val b = p!!.getBounds()
        val w = b.x1 - b.x0
        val h = b.y1 - b.y0
        val pointf = PointF(w, h)
        p.destroy()
        return APage(pageNum, pointf, zoomModel!!.zoom, 0)
    }
}

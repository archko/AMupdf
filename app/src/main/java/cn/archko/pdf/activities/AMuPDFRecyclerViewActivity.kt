package cn.archko.pdf.activities

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.mupdf.R
import cn.archko.pdf.adapters.MuPDFReflowAdapter
import cn.archko.pdf.colorpicker.ColorPickerDialog
import cn.archko.pdf.common.*
import cn.archko.pdf.entity.FontBean
import cn.archko.pdf.entity.MenuBean
import cn.archko.pdf.fragments.FontsFragment
import cn.archko.pdf.listeners.DataListener
import cn.archko.pdf.listeners.MenuListener
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.widgets.ViewerDividerItemDecoration
import org.vudroid.core.events.PageViewPresenter
import org.vudroid.core.models.ZoomModel
import org.vudroid.core.views.APageSeekBarControls
import org.vudroid.core.views.PageViewZoomControls

/**
 * @author: archko 2019/8/25 :12:43
 */
class AMuPDFRecyclerViewActivity : MuPDFRecyclerViewActivity(), OutlineListener {

    lateinit var mLeftDrawer: RecyclerView
    lateinit var mDrawerLayout: DrawerLayout
    lateinit var mControllerLayout: RelativeLayout

    private var mPageSeekBarControls: APageSeekBarControls? = null
    private var mReflow = false
    private val OUTLINE_REQUEST = 0
    private var pdfBookmarkManager: PDFBookmarkManager? = null
    private var outlineHelper: OutlineHelper? = null
    private var mZoomControls: PageViewZoomControls? = null
    private var mStyleControls: View? = null

    private var mMenuHelper: MenuHelper? = null

    private var mFontSeekBar: SeekBar? = null
    private var mFontSizeLabel: TextView? = null
    private var mFontFaceSelected: TextView? = null
    private var mFontFaceChange: TextView? = null
    private var mLineSpaceLabel: TextView? = null
    private var mLinespaceMinus: View? = null
    private var mLinespacePlus: View? = null
    private var mColorLabel: TextView? = null
    private var mBgSetting: View? = null
    private var mFgSetting: View? = null
    private var colorPickerDialog: ColorPickerDialog? = null

    private var mStyleHelper: StyleHelper? = null
    private val START_PROGRESS = 15

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (TextUtils.isEmpty(mPath)) {
            return
        }
        mPageSeekBarControls?.updateTitle(mPath)

        autoCrop = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PdfOptionsActivity.PREF_AUTOCROP, true)
    }

    override fun doLoadDoc() {
        try {
            progressDialog.setMessage("Loading menu")
            bitmapManager = BitmapManager()
            ImageDecoder.getInstance().setBitmapManager(bitmapManager)

            mRecyclerView.adapter = PDFRecyclerAdapter()
            addGesture()

            isDocLoaded = true
            pdfBookmarkManager = PDFBookmarkManager()
            var ac = 0;
            if (!autoCrop) {
                ac = 1;
            }
            pdfBookmarkManager!!.setStartBookmark(mPath, ac)
            var pos = pdfBookmarkManager?.restoreBookmark(mDocument!!.countPages())!!
            val progress = pdfBookmarkManager?.bookmarkToRestore;
            progress?.let {
                autoCrop = it.autoCrop == 0;
                mReflow = it.reflow == 1
            }

            if (mReflow) {
                if (null == mStyleHelper) {
                    mStyleHelper = StyleHelper()
                }
                mRecyclerView.adapter = MuPDFReflowAdapter(this, mDocument, mStyleHelper)
                mPageSeekBarControls?.reflowButton!!.setColorFilter(Color.argb(0xFF, 172, 114, 37))

            } else {
                mRecyclerView.adapter = PDFRecyclerAdapter()
                mPageSeekBarControls?.reflowButton!!.setColorFilter(Color.argb(0xFF, 255, 255, 255))
            }
            if (pos > 0) {
                mRecyclerView.scrollToPosition(pos)
            }
            addGesture()
            autoCropModeSet(autoCrop)

            mPageSeekBarControls?.showReflow(true)
            if (null != pdfBookmarkManager!!.getBookmarkToRestore()) {
                zoomModel?.setZoom(pdfBookmarkManager!!.getBookmarkToRestore().zoomLevel / 1000f)
            }
            outlineHelper = OutlineHelper(mDocument, this);

            mMenuHelper = MenuHelper(mLeftDrawer, outlineHelper, supportFragmentManager)
            mMenuHelper?.setupMenu(mPath, this@AMuPDFRecyclerViewActivity, menuListener)
            mMenuHelper?.setupOutline(pos)

            isDocLoaded = true

            val sp = getSharedPreferences(PREF_READER, Context.MODE_PRIVATE)
            val isFirst = sp.getBoolean(PREF_READER_KEY_FIRST, true)
            if (isFirst) {
                mDrawerLayout.openDrawer(mLeftDrawer)
                showOutline()

                sp.edit()
                        .putBoolean(PREF_READER_KEY_FIRST, false)
                        .apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        } finally {
            progressDialog.dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ImageDecoder.getInstance().setBitmapManager(null)
        bitmapManager?.clear()
    }

    override fun initView() {
        super.initView()
        mLeftDrawer = findViewById(R.id.left_drawer)
        mDrawerLayout = findViewById(R.id.drawerLayout)

        mControllerLayout = findViewById(R.id.layout)

        mZoomControls = createZoomControls(zoomModel!!)
        zoomModel?.addEventListener(this)

        mPageSeekBarControls = createSeekControls()

        var lp = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP)
        mControllerLayout.addView(mPageSeekBarControls, lp)

        lp = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        mControllerLayout.addView(mZoomControls, lp)

        mPageSeekBarControls?.autoCropButton!!.visibility = View.VISIBLE

        with(mLeftDrawer) {
            layoutManager = LinearLayoutManager(this@AMuPDFRecyclerViewActivity, LinearLayoutManager.VERTICAL, false)
            addItemDecoration(ViewerDividerItemDecoration(this@AMuPDFRecyclerViewActivity, LinearLayoutManager.VERTICAL))
        }
    }

    override fun addGesture() {
        mRecyclerView.setOnTouchListener { v, event ->
            gestureDetector!!.onTouchEvent(event)
            if (!mReflow) {
                if (multiTouchZoom != null) {
                    if (multiTouchZoom!!.onTouchEvent(event)) {
                        return@setOnTouchListener true
                    }

                    if (multiTouchZoom!!.isResetLastPointAfterZoom()) {
                        //setLastPosition(ev)
                        multiTouchZoom!!.setResetLastPointAfterZoom(false)
                    }
                }
            }
            false
        }
    }

    private fun toggleReflow() {
        reflowModeSet(!mReflow)
        Toast.makeText(this, if (mReflow) getString(R.string.entering_reflow_mode) else getString(R.string.leaving_reflow_mode), Toast.LENGTH_SHORT).show()
    }

    private fun reflowModeSet(reflow: Boolean) {
        val pos = getCurrentPos();
        mReflow = reflow
        if (mReflow) {
            if (null == mStyleHelper) {
                mStyleHelper = StyleHelper()
            }
            mRecyclerView.adapter = MuPDFReflowAdapter(this, mDocument, mStyleHelper)
            mPageSeekBarControls?.reflowButton!!.setColorFilter(Color.argb(0xFF, 172, 114, 37))

            addGesture()
        } else {
            addGesture()

            mRecyclerView.adapter = PDFRecyclerAdapter()
            mPageSeekBarControls?.reflowButton!!.setColorFilter(Color.argb(0xFF, 255, 255, 255))
        }
        if (pos > 0) {
            mRecyclerView.scrollToPosition(pos)
        }
    }

    private fun createZoomControls(zoomModel: ZoomModel): PageViewZoomControls {
        val controls = PageViewZoomControls(this, zoomModel)
        controls.gravity = Gravity.RIGHT or Gravity.BOTTOM
        zoomModel.addEventListener(controls)
        return controls
    }

    override fun onSingleTap() {
        if (mPageSeekBarControls?.visibility == View.VISIBLE) {
            mPageSeekBarControls?.hide()
            return
        }
        showReflowConfigMenu()
    }

    override fun onDoubleTap() {
        super.onDoubleTap()
        if (!isDocLoaded) {
            return
        }
        mPageSeekBarControls?.hide()
        mZoomControls?.visibility = View.GONE
        mStyleControls?.visibility = View.GONE
        if (!mDrawerLayout.isDrawerOpen(mLeftDrawer)) {
            mDrawerLayout.openDrawer(mLeftDrawer)
        } else {
            mDrawerLayout.closeDrawer(mLeftDrawer)
        }
        showOutline()
    }

    private fun createSeekControls(): APageSeekBarControls {
        mPageSeekBarControls = APageSeekBarControls(this, object : PageViewPresenter {
            override fun reflow() {
                toggleReflow()
            }

            override fun getPageCount(): Int {
                return mDocument!!.countPages()
            }

            override fun getCurrentPageIndex(): Int {
                return getCurrentPos();
            }

            override fun goToPageIndex(page: Int) {
                mRecyclerView.layoutManager?.scrollToPosition(page)
            }

            override fun showOutline() {
                //outlineHelper?.openOutline(getCurrentPos(), OUTLINE_REQUEST)
                this@AMuPDFRecyclerViewActivity.showOutline()
            }

            override fun back() {
                //this@MuPDFRecyclerViewActivity.finish()
                mPageSeekBarControls?.hide()
            }

            override fun getTitle(): String {
                return mPath!!
            }

            override fun autoCrop() {
                toggleAutoCrop();
            }
        })
        return mPageSeekBarControls!!
    }

    private fun showOutline() {
        outlineHelper?.let {
            if (it.hasOutline()) {
                val frameLayout = mPageSeekBarControls?.getLayoutOutline()

                if (frameLayout?.visibility == View.GONE) {
                    frameLayout?.visibility = View.VISIBLE
                    mMenuHelper?.updateSelection(getCurrentPos())
                } else {
                    frameLayout?.visibility = View.GONE
                }
            } else {
                mPageSeekBarControls?.getLayoutOutline()?.visibility = View.GONE
            }
        }
    }

    private fun toggleAutoCrop() {
        var flag = autoCropModeSet(!autoCrop)
        if (flag) {
            autoCrop = !autoCrop;
        }
    }

    private fun autoCropModeSet(autoCrop: Boolean): Boolean {
        if (mReflow) {
            mPageSeekBarControls?.autoCropButton!!.setColorFilter(Color.argb(0xFF, 255, 255, 255))
            return false
        } else {
            if (autoCrop) {
                mPageSeekBarControls?.autoCropButton!!.setColorFilter(Color.argb(0xFF, 172, 114, 37))
            } else {
                mPageSeekBarControls?.autoCropButton!!.setColorFilter(Color.argb(0xFF, 255, 255, 255))
            }
            mRecyclerView.adapter?.notifyDataSetChanged()
            return true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            OUTLINE_REQUEST -> {
                onSelectedOutline(resultCode)
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onSelectedOutline(resultCode: Int) {
        mRecyclerView.layoutManager?.scrollToPosition(resultCode - RESULT_FIRST_USER)
        updateProgress(resultCode - RESULT_FIRST_USER)
    }

    override fun commitZoom() {
        bitmapManager?.clear()
        mRecyclerView.adapter?.notifyItemChanged(getCurrentPos())
    }

    override fun updateProgress(index: Int) {
        if (isDocLoaded && mPageSeekBarControls?.visibility == View.VISIBLE) {
            mPageSeekBarControls?.updatePageProgress(index)
        }
    }
    //--------------------------------------

    override fun onResume() {
        super.onResume()

        mPageSeekBarControls?.hide()
        mZoomControls?.hide()
        mStyleControls?.visibility = View.GONE
        mDrawerLayout.closeDrawers()
    }

    override fun onPause() {
        super.onPause()
        if (autoCrop) {
            pdfBookmarkManager?.bookmarkToRestore?.autoCrop = 0
        } else {
            pdfBookmarkManager?.bookmarkToRestore?.autoCrop = 1
        }
        if (mReflow) {
            pdfBookmarkManager?.bookmarkToRestore?.reflow = 1
        } else {
            pdfBookmarkManager?.bookmarkToRestore?.reflow = 0
        }
        val position = getCurrentPos()
        pdfBookmarkManager?.saveCurrentPage(mPath, mDocument!!.countPages(), position, zoomModel!!.zoom * 1000.0f, -1, 0)
        if (null != mRecyclerView.adapter && mRecyclerView.adapter is MuPDFReflowAdapter) {
            (mRecyclerView.adapter as MuPDFReflowAdapter).clearCacheViews()
        }
    }

    //===========================================

    private fun showReflowConfigMenu() {
        if (mReflow) {
            if (null == mStyleControls) {
                initStyleControls()
            } else {
                if (mStyleControls?.visibility == View.VISIBLE) {
                    mStyleControls?.visibility = View.GONE
                    super.onSingleTap()
                } else {
                    showStyleFragment()
                }
            }
        } else {
            super.onSingleTap()
        }
    }

    private fun initStyleControls() {
        mPageSeekBarControls?.hide()
        if (null == mStyleControls) {
            mStyleControls = layoutInflater.inflate(R.layout.text_style, mDrawerLayout, false)

            val lp = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            mControllerLayout.addView(mStyleControls, lp)
        }
        mStyleControls?.visibility = View.VISIBLE

        mFontSeekBar = mStyleControls?.findViewById(R.id.font_seek_bar)
        mFontSizeLabel = mStyleControls?.findViewById(R.id.font_size_label)
        mFontFaceSelected = mStyleControls?.findViewById(R.id.font_face_selected)
        mFontFaceChange = mStyleControls?.findViewById(R.id.font_face_change)
        mLineSpaceLabel = mStyleControls?.findViewById(R.id.line_space_label)
        mLinespaceMinus = mStyleControls?.findViewById(R.id.linespace_minus)
        mLinespacePlus = mStyleControls?.findViewById(R.id.linespace_plus)
        mColorLabel = mStyleControls?.findViewById(R.id.color_label)
        mBgSetting = mStyleControls?.findViewById(R.id.bg_setting)
        mFgSetting = mStyleControls?.findViewById(R.id.fg_setting)

        mStyleHelper?.let {
            val progress = (it.styleBean?.textSize!! - START_PROGRESS).toInt()
            mFontSeekBar?.progress = progress
            mFontSizeLabel?.text = String.format("%s", progress + START_PROGRESS)
            mFontSeekBar?.max = 10
            mFontSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val index = (progress + START_PROGRESS)
                    mFontSizeLabel?.text = String.format("%s", index)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    it.styleBean?.textSize = (seekBar?.progress!! + START_PROGRESS).toFloat()
                    it.saveStyleToSP(it.styleBean)
                    updateReflowAdapter()
                }
            });
            mFontFaceSelected?.text = it.fontHelper?.fontBean?.fontName

            mLineSpaceLabel?.text = String.format("%s倍", it.styleBean?.lineSpacingMult)
            mColorLabel?.setBackgroundColor(it.styleBean?.bgColor!!)
            mColorLabel?.setTextColor(it.styleBean?.fgColor!!)
        }

        mFontFaceChange?.setOnClickListener {
            FontsFragment.showFontsDialog(this, mStyleHelper,
                    object : DataListener {
                        override fun onSuccess(vararg args: Any?) {
                            updateReflowAdapter()
                            val fBean = args[0] as FontBean
                            mFontFaceSelected?.text = fBean.fontName
                        }

                        override fun onFailed(vararg args: Any?) {
                        }
                    })
        }

        mLinespaceMinus?.setOnClickListener {
            var old = mStyleHelper?.styleBean?.lineSpacingMult
            if (old!! < 0.8f) {
                return@setOnClickListener
            }
            old = old.minus(0.1f)
            applyLineSpace(old)
        }
        mLinespacePlus?.setOnClickListener {
            var old = mStyleHelper?.styleBean?.lineSpacingMult
            if (old!! > 2.2f) {
                return@setOnClickListener
            }
            old = old?.plus(0.1f)
            applyLineSpace(old)
        }
        mBgSetting?.setOnClickListener {
            pickerColor(mStyleHelper?.styleBean?.bgColor!!, ColorPickerDialog.OnColorSelectedListener { color ->
                mColorLabel?.setBackgroundColor(color)
                mStyleHelper?.styleBean?.bgColor = color
                mStyleHelper?.saveStyleToSP(mStyleHelper?.styleBean)
                updateReflowAdapter()
            })
        }
        mFgSetting?.setOnClickListener {
            pickerColor(mStyleHelper?.styleBean?.fgColor!!, ColorPickerDialog.OnColorSelectedListener { color ->
                mColorLabel?.setTextColor(color)
                mStyleHelper?.styleBean?.fgColor = color
                mStyleHelper?.saveStyleToSP(mStyleHelper?.styleBean)
                updateReflowAdapter()
            })
        }
    }

    private fun updateReflowAdapter() {
        mRecyclerView.adapter?.run {
            this.notifyDataSetChanged()
        }
    }

    private fun applyLineSpace(old: Float?) {
        mLineSpaceLabel?.text = String.format("%s倍", old)
        mStyleHelper?.styleBean?.lineSpacingMult = old!!
        mStyleHelper?.saveStyleToSP(mStyleHelper?.styleBean)
        updateReflowAdapter()
    }

    private fun pickerColor(initialColor: Int, selectedListener: ColorPickerDialog.OnColorSelectedListener) {
        if (null == colorPickerDialog) {
            colorPickerDialog = ColorPickerDialog(this, initialColor, selectedListener);
        } else {
            colorPickerDialog?.updateColor(initialColor)
            colorPickerDialog?.setOnColorSelectedListener(selectedListener)
        }
        colorPickerDialog?.show();
    }

    private fun showStyleFragment() {
        mZoomControls?.hide()
        mStyleControls?.visibility = View.VISIBLE
    }

    //===========================================

    companion object {

        private const val TAG = "AMuPDFRecyclerViewActivity"
        public const val TYPE_TITLE = 0
        const val PREF_READER = "pref_reader"
        const val PREF_READER_KEY_FIRST = "pref_reader_key_first"
    }

    //===========================================

    private var menuListener = object : MenuListener {

        override fun onMenuSelected(data: MenuBean?, position: Int) {
            when (data?.type) {
                TYPE_PROGRESS -> {
                    mDrawerLayout.closeDrawer(mLeftDrawer)
                    mPageSeekBarControls?.show()
                }
                TYPE_ZOOM -> {
                    mDrawerLayout.closeDrawer(mLeftDrawer)
                    mZoomControls?.show()
                    mStyleControls?.visibility = View.VISIBLE
                }
                TYPE_CLOSE -> {
                    this@AMuPDFRecyclerViewActivity.finish()
                }
                TYPE_SETTINGS -> {
                    PdfOptionsActivity.start(this@AMuPDFRecyclerViewActivity)
                }
                else -> {
                }
            }
        }

    }
}

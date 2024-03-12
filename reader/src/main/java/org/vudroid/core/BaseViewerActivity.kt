package org.vudroid.core

import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Gravity
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import cn.archko.pdf.activities.PdfOptionsActivity
import cn.archko.pdf.common.SensorHelper
import cn.archko.pdf.common.StatusBarHelper
import cn.archko.pdf.listeners.SimpleGestureListener
import cn.archko.pdf.presenter.PageViewPresenter
import cn.archko.pdf.viewmodel.PDFViewModel
import cn.archko.pdf.widgets.APageSeekBarControls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.vudroid.core.events.CurrentPageListener
import org.vudroid.core.events.DecodingProgressListener
import org.vudroid.core.models.CurrentPageModel
import org.vudroid.core.models.DecodingProgressModel
import org.vudroid.core.models.ZoomModel
import org.vudroid.core.views.PageViewZoomControls

abstract class BaseViewerActivity : FragmentActivity(), DecodingProgressListener,
    CurrentPageListener {
    var decodeService: DecodeService? = null
        private set
    var documentView: DocumentView? = null
        private set
    private var pageNumberToast: Toast? = null
    private var currentPageModel: CurrentPageModel? = null
    var pageControls: PageViewZoomControls? = null

    //private CurrentPageModel mPageModel;
    var pageSeekBarControls: APageSeekBarControls? = null
    var pdfViewModel: PDFViewModel? = null
    var sensorHelper: SensorHelper? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatusBarHelper.hideSystemUI(this)
        StatusBarHelper.setImmerseBarAppearance(window, true)
        initDecodeService()
        val zoomModel = ZoomModel()
        pdfViewModel = PDFViewModel()
        sensorHelper = SensorHelper(this)
        val uri = intent.data
        val absolutePath = Uri.decode(uri!!.encodedPath)

        val progressModel = DecodingProgressModel()
        progressModel.addEventListener(this)
        currentPageModel = CurrentPageModel()
        currentPageModel!!.addEventListener(this)
        documentView =
            DocumentView(this, zoomModel, progressModel, currentPageModel, simpleGestureListener)
        zoomModel.addEventListener(documentView)
        documentView!!.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        decodeService!!.setContainerView(documentView)
        documentView!!.setDecodeService(decodeService)
        decodeService!!.open(absolutePath)
        val frameLayout = createMainContainer()
        frameLayout.addView(documentView)
        pageControls = createZoomControls(zoomModel)
        frameLayout.addView(pageControls)
        setContentView(frameLayout)

        documentView!!.showDocument()
        lifecycleScope.launch {
            val bookProgress = withContext(Dispatchers.IO) {
                return@withContext pdfViewModel!!.loadBookProgressByPath(absolutePath)
            }
            if (null != bookProgress) {
                val currentPage = bookProgress.page
                zoomModel.setZoom(bookProgress.zoomLevel / 1000)
                val scrollX = bookProgress.offsetX
                val scrollY = bookProgress.offsetY
                if (0 < currentPage) {
                    documentView!!.goToPage(currentPage, scrollX, scrollY)
                }
            }
        }

        pageSeekBarControls = APageSeekBarControls(this, object : PageViewPresenter {
            override fun getPageCount(): Int {
                return decodeService!!.getPageCount()
            }

            override fun getCurrentPageIndex(): Int {
                return documentView!!.getCurrentPage()
            }

            override fun goToPageIndex(page: Int) {
                documentView!!.goToPage(page)
            }

            override fun showOutline() {
                openOutline()
            }

            override fun back() {
                finish()
            }

            override fun getTitle(): String {
                val uri = intent.data
                return Uri.decode(uri!!.encodedPath)
            }

            override fun reflow() {}
            override fun autoCrop() {}
        })
        frameLayout.addView(pageSeekBarControls)
        pageSeekBarControls!!.hide()
        pageSeekBarControls!!.showReflow(true)
        pageSeekBarControls!!.updateTitle(absolutePath)
    }

    override fun decodingProgressChanged(currentlyDecoding: Int) {
    }

    override fun currentPageChanged(pageIndex: Int) {
        showPageIndex(pageIndex)
        documentView!!.goToPage(pageIndex)
    }

    private fun showPageIndex(pageIndex: Int) {
        val pageText = (pageIndex + 1).toString() + "/" + decodeService!!.getPageCount()
        if (pageNumberToast != null) {
            pageNumberToast!!.setText(pageText)
        } else {
            pageNumberToast = Toast.makeText(this, pageText, Toast.LENGTH_SHORT)
        }
        pageNumberToast!!.setGravity(Gravity.BOTTOM or Gravity.LEFT, 30, 0)
        pageNumberToast!!.show()
        //saveCurrentPage();
    }

    private fun setWindowTitle() {
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        setWindowTitle()
    }

    private fun createZoomControls(zoomModel: ZoomModel): PageViewZoomControls {
        val controls = PageViewZoomControls(this, zoomModel)
        controls.gravity = Gravity.RIGHT or Gravity.BOTTOM
        zoomModel.addEventListener(controls)
        return controls
    }

    private fun createMainContainer(): FrameLayout {
        return FrameLayout(this)
    }

    private fun initDecodeService() {
        if (decodeService == null) {
            decodeService = createDecodeService()
        }
    }

    protected abstract fun createDecodeService(): DecodeService?
    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        decodeService!!.recycle()
        decodeService = null
        super.onDestroy()
    }

    open fun openOutline() {}

    //--------------------------------------
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onResume() {
        super.onResume()
        sensorHelper!!.onResume()
        val options = PreferenceManager.getDefaultSharedPreferences(this)
        if (options.getBoolean(PdfOptionsActivity.PREF_KEEP_ON, false)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        if (options.getBoolean(PdfOptionsActivity.PREF_FULLSCREEN, true)) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        pageControls!!.hide()
        var height = documentView!!.height
        height = if (height <= 0) {
            ViewConfiguration().scaledTouchSlop * 2
        } else {
            (height * 0.03).toInt()
        }
        documentView!!.setScrollMargin(height)
        documentView!!.setDecodePage(1 /*options.getBoolean(PdfOptionsActivity.PREF_RENDER_AHEAD, true) ? 1 : 0*/)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE)
        }
    }

    override fun onPause() {
        super.onPause()
        val uri = intent.data
        val filePath = Uri.decode(uri!!.encodedPath)
        pdfViewModel!!.saveBookProgress(
            filePath,
            decodeService?.pageCount ?: 1,
            documentView!!.getCurrentPage() + 1,
            pdfViewModel!!.bookProgress!!.zoomLevel * 1000f,
            documentView!!.scrollX,
            documentView!!.scrollY
        )
        sensorHelper!!.onPause()
    }

    //--------------------------------

    private var simpleGestureListener: SimpleGestureListener = object : SimpleGestureListener {
        override fun onSingleTapConfirmed(currentPage: Int) {
            currentPageChanged(currentPage)
        }

        override fun onDoubleTapEvent(currentPage: Int) {
            pageSeekBarControls!!.toggleSeekControls()
            pageControls!!.toggleZoomControls()
        }
    }

    protected fun currentPage(): Int {
        return documentView!!.getCurrentPage()
    }

    companion object {
        private const val TAG = "BaseViewer"
    }
}

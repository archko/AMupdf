package org.vudroid.core;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.vudroid.core.events.CurrentPageListener;
import org.vudroid.core.events.DecodingProgressListener;
import org.vudroid.core.models.CurrentPageModel;
import org.vudroid.core.models.DecodingProgressModel;
import org.vudroid.core.models.ZoomModel;
import org.vudroid.core.views.PageViewZoomControls;

import androidx.fragment.app.FragmentActivity;
import cn.archko.pdf.activities.PdfOptionsActivity;
import cn.archko.pdf.common.PDFBookmarkManager;
import cn.archko.pdf.common.SensorHelper;
import cn.archko.pdf.common.StatusBarHelper;
import cn.archko.pdf.listeners.SimpleGestureListener;
import cn.archko.pdf.presenter.PageViewPresenter;
import cn.archko.pdf.widgets.APageSeekBarControls;

public abstract class BaseViewerActivity extends FragmentActivity implements DecodingProgressListener, CurrentPageListener {
    private static final String TAG = "BaseViewer";
    private DecodeService decodeService;
    private DocumentView documentView;
    private Toast pageNumberToast;
    private CurrentPageModel currentPageModel;
    PageViewZoomControls mPageControls;
    //private CurrentPageModel mPageModel;
    APageSeekBarControls mPageSeekBarControls;

    PDFBookmarkManager pdfBookmarkManager;
    SensorHelper sensorHelper;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StatusBarHelper.hideSystemUI(this);
        StatusBarHelper.setImmerseBarAppearance(getWindow(), true);

        initDecodeService();
        final ZoomModel zoomModel = new ZoomModel();
        pdfBookmarkManager = new PDFBookmarkManager();
        sensorHelper = new SensorHelper(this);

        Uri uri = getIntent().getData();
        String absolutePath = Uri.decode(uri.getEncodedPath());
        pdfBookmarkManager.setStartBookmark(absolutePath, 0);
        if (null != pdfBookmarkManager.getBookmarkToRestore()) {
            zoomModel.setZoom(pdfBookmarkManager.getBookmarkToRestore().zoomLevel / 1000);
        }
        final DecodingProgressModel progressModel = new DecodingProgressModel();
        progressModel.addEventListener(this);
        currentPageModel = new CurrentPageModel();
        currentPageModel.addEventListener(this);
        documentView = new DocumentView(this, zoomModel, progressModel, currentPageModel, simpleGestureListener);
        zoomModel.addEventListener(documentView);
        documentView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        decodeService.setContainerView(documentView);
        documentView.setDecodeService(decodeService);
        decodeService.open(absolutePath);

        final FrameLayout frameLayout = createMainContainer();
        frameLayout.addView(documentView);
        mPageControls = createZoomControls(zoomModel);
        frameLayout.addView(mPageControls);

        setContentView(frameLayout);

        int currentPage = pdfBookmarkManager.restoreBookmark(decodeService.getPageCount());
        if (0 < currentPage) {
            documentView.goToPage(currentPage, pdfBookmarkManager.getBookmarkToRestore().offsetX, pdfBookmarkManager.getBookmarkToRestore().offsetY);
        }
        documentView.showDocument();

        mPageSeekBarControls = new APageSeekBarControls(this, new PageViewPresenter() {
            @Override
            public int getPageCount() {
                return decodeService.getPageCount();
            }

            @Override
            public int getCurrentPageIndex() {
                return documentView.getCurrentPage();
            }

            @Override
            public void goToPageIndex(int page) {
                documentView.goToPage(page);
            }

            @Override
            public void showOutline() {
                openOutline();
            }

            @Override
            public void back() {
                BaseViewerActivity.this.finish();
            }

            @Override
            public String getTitle() {
                Uri uri = getIntent().getData();
                String filePath = Uri.decode(uri.getEncodedPath());
                return filePath;
            }

            @Override
            public void reflow() {

            }

            @Override
            public void autoCrop() {

            }
        });
        frameLayout.addView(mPageSeekBarControls);
        mPageSeekBarControls.hide();
        mPageSeekBarControls.showReflow(true);
        mPageSeekBarControls.updateTitle(absolutePath);
    }

    public void decodingProgressChanged(final int currentlyDecoding) {
        //runOnUiThread(() -> getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS, currentlyDecoding == 0 ? 10000 : currentlyDecoding));
    }

    public void currentPageChanged(int pageIndex) {
        showPageIndex(pageIndex);
        documentView.goToPage(pageIndex);
    }

    private void showPageIndex(int pageIndex) {
        final String pageText = (pageIndex + 1) + "/" + decodeService.getPageCount();
        if (pageNumberToast != null) {
            pageNumberToast.setText(pageText);
        } else {
            pageNumberToast = Toast.makeText(this, pageText, Toast.LENGTH_SHORT);
        }
        pageNumberToast.setGravity(Gravity.BOTTOM | Gravity.LEFT, 30, 0);
        pageNumberToast.show();
        //saveCurrentPage();
    }

    private void setWindowTitle() {
        //final String name = getIntent().getData().getLastPathSegment();
        //getWindow().setTitle(name);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setWindowTitle();
    }

    private PageViewZoomControls createZoomControls(ZoomModel zoomModel) {
        final PageViewZoomControls controls = new PageViewZoomControls(this, zoomModel);
        controls.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        zoomModel.addEventListener(controls);
        return controls;
    }

    private FrameLayout createMainContainer() {
        return new FrameLayout(this);
    }

    private void initDecodeService() {
        if (decodeService == null) {
            decodeService = createDecodeService();
        }
    }

    protected abstract DecodeService createDecodeService();

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        decodeService.recycle();
        decodeService = null;
        super.onDestroy();
    }

    public void openOutline() {
    }

    public DecodeService getDecodeService() {
        return decodeService;
    }

    public DocumentView getDocumentView() {
        return documentView;
    }

    public APageSeekBarControls getPageSeekBarControls() {
        return mPageSeekBarControls;
    }

    public PageViewZoomControls getPageControls() {
        return mPageControls;
    }

    //--------------------------------------

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    protected void onResume() {
        super.onResume();

        sensorHelper.onResume();
        SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(this);

        if (options.getBoolean(PdfOptionsActivity.PREF_KEEP_ON, false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        if (options.getBoolean(PdfOptionsActivity.PREF_FULLSCREEN, true)) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        mPageControls.hide();
        int height = documentView.getHeight();
        if (height <= 0) {
            height = new ViewConfiguration().getScaledTouchSlop() * 2;
        } else {
            height = (int) (height * 0.03);
        }
        documentView.setScrollMargin(height);
        documentView.setDecodePage(1/*options.getBoolean(PdfOptionsActivity.PREF_RENDER_AHEAD, true) ? 1 : 0*/);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Uri uri = getIntent().getData();
        String filePath = Uri.decode(uri.getEncodedPath());
        pdfBookmarkManager.saveCurrentPage(filePath, decodeService.getPageCount(), documentView.getCurrentPage(),
                documentView.getZoomModel().getZoom() * 1000f, documentView.getScrollX(), documentView.getScrollY());

        sensorHelper.onPause();
    }

    //--------------------------------

    protected final int OUTLINE_REQUEST = 0;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case OUTLINE_REQUEST:
                if (resultCode >= 0)
                    documentView.goToPage(resultCode);
                mPageSeekBarControls.hide();
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    SimpleGestureListener simpleGestureListener = new SimpleGestureListener() {
        @Override
        public void onSingleTapConfirmed(int currentPage) {
            currentPageChanged(currentPage);
        }

        @Override
        public void onDoubleTapEvent(int currentPage) {
            mPageSeekBarControls.toggleSeekControls();
            mPageControls.toggleZoomControls();
        }
    };

    protected int currentPage() {
        return documentView.getCurrentPage();
    }
}

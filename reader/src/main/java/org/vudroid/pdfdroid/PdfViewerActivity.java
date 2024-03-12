package org.vudroid.pdfdroid;

import android.view.View;

import com.jeremyliao.liveeventbus.LiveEventBus;

import org.vudroid.core.BaseViewerActivity;
import org.vudroid.core.DecodeService;
import org.vudroid.core.DecodeServiceBase;
import org.vudroid.pdfdroid.codec.PdfContext;
import org.vudroid.pdfdroid.codec.PdfDocument;

import cn.archko.pdf.common.Event;
import cn.archko.pdf.common.MenuHelper;
import cn.archko.pdf.common.OutlineHelper;
import cn.archko.pdf.listeners.OutlineListener;
import cn.archko.pdf.mupdf.MupdfDocument;

public class PdfViewerActivity extends BaseViewerActivity implements OutlineListener {

    @Override
    protected DecodeService createDecodeService() {
        return new DecodeServiceBase(new PdfContext());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LiveEventBus
                .get(Event.ACTION_STOPPED)
                .post(null);
    }

    private OutlineHelper mOutlineHelper;
    private MenuHelper mMenuHelper;

    public void openOutline() {
        DecodeServiceBase service = (DecodeServiceBase) getDecodeService();
        PdfDocument document = (PdfDocument) service.getDocument();
        if (null == mOutlineHelper) {
            MupdfDocument mupdfDocument = new MupdfDocument(this);
            mupdfDocument.setDocument(document.getCore());
            mOutlineHelper = new OutlineHelper(mupdfDocument, this);
        }
        //mOutlineHelper.openOutline(getDocumentView().getCurrentPage(), OUTLINE_REQUEST);
        if (mMenuHelper == null) {
            mMenuHelper = new MenuHelper(null, mOutlineHelper, getSupportFragmentManager());
            mMenuHelper.setupOutline(currentPage());
        }

        mMenuHelper.updateSelection(currentPage());

        if (mOutlineHelper.hasOutline()) {
            if (getPageSeekBarControls().getLayoutOutline().getVisibility() == View.GONE) {
                getPageSeekBarControls().getLayoutOutline().setVisibility(View.VISIBLE);
            } else {
                getPageSeekBarControls().getLayoutOutline().setVisibility(View.GONE);
            }
        } else {
            getPageSeekBarControls().getLayoutOutline().setVisibility(View.GONE);
        }
    }
    

    @Override
    public void onSelectedOutline(int index) {
        if (index >= 0) {
            getDocumentView().goToPage(index - RESULT_FIRST_USER);
        }
        getPageSeekBarControls().hide();
        getPageControls().hide();
    }
}

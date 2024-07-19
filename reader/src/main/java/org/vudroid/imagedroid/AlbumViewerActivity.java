package org.vudroid.imagedroid;

import android.app.ProgressDialog;
import android.widget.Toast;

import org.vudroid.core.BaseViewerActivity;
import org.vudroid.core.DecodeService;
import org.vudroid.core.DecodeServiceBase;
import org.vudroid.core.codec.CodecDocument;
import org.vudroid.imagedroid.codec.AlbumContext;

import cn.archko.pdf.AppExecutors;

public class AlbumViewerActivity extends BaseViewerActivity {

    protected ProgressDialog progressDialog;

    @Override
    protected DecodeService createDecodeService() {
        return new DecodeServiceBase(new AlbumContext());
    }

    public void loadDocument(String path, boolean crop) {
        boolean autoCrop = false;
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading");
        progressDialog.show();

        AppExecutors.Companion.getInstance().diskIO().execute(() -> {
            CodecDocument document = getDecodeService().open(path, autoCrop, false);
            AppExecutors.Companion.getInstance().mainThread().execute(() -> {
                progressDialog.dismiss();
                if (null == document) {
                    Toast.makeText(this, "Open Failed", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                setDocLoaded(true);
                getDocumentView().showDocument(autoCrop);
            });
        });
    }
}

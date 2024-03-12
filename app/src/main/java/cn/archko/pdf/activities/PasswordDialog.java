package cn.archko.pdf.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import cn.archko.mupdf.R;

public class PasswordDialog {

    public static void show(final Activity activity, final PasswordDialogListener listener) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(activity);

        LayoutInflater li = LayoutInflater.from(activity);
        View mainView = li.inflate(R.layout.password_dialog, null);
        dialog.setView(mainView);
        dialog.setTitle(activity.getResources().getString(R.string.password_for_document));
        final EditText et = mainView.findViewById(R.id.editTextDialogUserInput);

        dialog.setPositiveButton(activity.getResources().getString(R.string.ok),
                (dialog1, which) -> {
                    //PDFUtilities.hideKeyboard(activity);
                    String content = et.getText().toString();
                    if (TextUtils.isEmpty(content)) {
                        Toast.makeText(activity, R.string.password_for_document, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    dialog1.dismiss();
                    if (listener != null) {
                        listener.onOK(content);
                    }
                });

        dialog.setNegativeButton(activity.getResources().getString(R.string.cancel),
                (dialog12, which) -> {
                    //PDFUtilities.hideKeyboard(activity);
                    dialog12.dismiss();
                    if (listener != null) {
                        listener.onCancel();
                    }
                });

        dialog.create().show();
    }

    public interface PasswordDialogListener {
        void onOK(String content);

        void onCancel();
    }
}
package org.vudroid.core;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;

import com.artifex.mupdf.fitz.Outline;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import cn.archko.pdf.R;
import cn.archko.pdf.databinding.DialogOutlineBinding;

public class OutlineDialog extends Dialog {

    public interface OutlineListener {
        void selected(int page, boolean dismiss);

        void orientation(int ori);

        void setCrop(boolean crop);
    }

    public static class Item {
        public String title;
        public int page;

        public Item(String title, int page) {
            this.title = title;
            this.page = page;
        }

        public String toString() {
            return String.format("%s - %s", page, title);
        }
    }

    protected ArrayAdapter<Item> adapter;
    private final List<Item> items = new ArrayList<>();
    private boolean initOutline = false;
    private int currentPage;
    private int oriention = DocumentView.VERTICAL;
    private int total;
    private DialogOutlineBinding binding;
    private boolean crop = true;
    private boolean showCrop = true;
    private OutlineListener outlineListener;
    private static final String pattern_str = "(#page=)(\\d+)(&)";
    private final Pattern pattern = Pattern.compile(pattern_str);

    public OutlineDialog(@NonNull Context context) {
        super(context, R.style.Dialog_Bottom);

        binding = DialogOutlineBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.closeBtn.setOnClickListener(v -> toggleTocList());
        binding.orientionBtn.setOnClickListener(v -> toggleOrientation());
        binding.cropBtn.setOnClickListener(v -> toggleCropButton());

        adapter = new ArrayAdapter<>(context, R.layout.dialog_outline_item);
        binding.listview.setAdapter(adapter);
        binding.listview.setOnItemClickListener((parent, view, position, id) -> onListItemClick(position, id));

        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //Log.d("PDFSDK", String.format("onProgressChanged:%s-%s-%s", progress, currentPage, fromUser));
                if (fromUser) {
                    currentPage = progress;
                    updateProgress();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (null != outlineListener) {
                    outlineListener.selected(seekBar.getProgress(), false);
                }
            }
        });

        //resize();

        setCanceledOnTouchOutside(true);
    }

    private void toggleOrientation() {
        if (oriention == DocumentView.VERTICAL) {
            oriention = DocumentView.HORIZONTAL;
        } else {
            oriention = DocumentView.VERTICAL;
        }
        setOrientation(oriention);
        if (null != outlineListener) {
            outlineListener.orientation(oriention);
        }
    }

    private void toggleCropButton() {
        crop = !crop;
        updateCropButton();
        outlineListener.setCrop(crop);
    }

    private void updateCropButton() {
        if (crop) {
            binding.cropBtn.setColorFilter(Color.argb(0xFF, 172, 114, 37));
        } else {
            binding.cropBtn.setColorFilter(Color.argb(0xFF, 255, 255, 255));
        }
    }

    public void setOrientation(int oriention) {
        this.oriention = oriention;
        if (oriention == DocumentView.VERTICAL) {
            binding.orientionBtn.setImageResource(R.drawable.viewer_menu_viewmode_vscroll);
        } else {
            binding.orientionBtn.setImageResource(R.drawable.viewer_menu_viewmode_hscroll);
        }
    }

    int dp2px(float dp) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return Math.round(px);
    }

    private void resize() {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        if ((binding.listview.getVisibility() == View.GONE)) {
            params.height = dp2px(44);
        } else {
            if (adapter.getCount() == 0) {
                params.height = dp2px(44);
            } else {
                DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
                params.height = (int) (displayMetrics.heightPixels * 0.75);
            }
        }
        getWindow().setAttributes(params);
        getWindow().setGravity(Gravity.BOTTOM);
    }

    private void toggleTocList() {
        if (binding.listview.getVisibility() == View.VISIBLE) {
            binding.listview.setVisibility(View.GONE);
        } else {
            binding.listview.setVisibility(View.VISIBLE);
        }
        resize();
    }

    public void clear() {
        items.clear();
        outlineListener = null;
        currentPage = -1;
        total = 1;
        initOutline = false;
    }

    //item.uri:#page=2&zoom=nan,0,0
    public void initOutlinesIfNeed(boolean showCrop, Outline[] outlines, int total, OutlineListener outlineListener) {
        this.showCrop = showCrop;
        if (null != binding) {
            binding.cropBtn.setVisibility(showCrop ? View.VISIBLE : View.GONE);
        }
        if (initOutline) {
            return;
        }

        this.total = total - 1;
        this.outlineListener = outlineListener;
        items.clear();
        adapter.clear();
        adapter.notifyDataSetChanged();

        initOutline = true;
        if (null == outlines || outlines.length == 0) {
            return;
        }
        processOutline(outlines);
        adapter.clear();
        adapter.addAll(items);
        adapter.notifyDataSetChanged();
    }

    private void updateProgress() {
        Log.d("PDFSDK", "updateProgress:" + total + "-" + currentPage);
        binding.seekBar.setMax(total);
        binding.progressTxt.setText(String.format("%s/%s", currentPage, total));
    }

    private void processOutline(Outline[] outlines) {
        if (outlines != null && outlines.length > 0) {
            for (int i = 0; i < outlines.length; i++) {
                Outline outline = outlines[i];
                Matcher matcher = pattern.matcher(outline.uri);
                if (matcher.find()) {
                    int page = Integer.parseInt(matcher.group(0).replace("#page=", "").replace("&", ""));
                    Item item = new Item(outline.title, page);
                    items.add(item);
                } else {
                    if (!TextUtils.isEmpty(outline.uri)) {
                        try {
                            int page = Integer.parseInt(outline.uri);
                            Item item = new Item(outline.title, page);
                            items.add(item);
                        } catch (NumberFormatException e) {
                        }
                    }
                }

                if (outline.down != null) {
                    processOutline(outline.down);
                }
            }
        }
    }

    /**
     * @param currentPage
     */
    public void setCurrPage(int currentPage) {
        this.currentPage = currentPage;
        binding.seekBar.setProgress(currentPage);
        int found = -1;
        for (int i = 0; i < items.size(); ++i) {
            Item item = items.get(i);
            if (found < 0 && item.page >= currentPage) {
                found = i;
            }
        }
        if (found >= 0) {
            Log.d("PDFSDK", "found:" + found);
            binding.listview.setSelection(found);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void show() {
        super.show();
        binding.cropBtn.setVisibility(showCrop ? View.VISIBLE : View.GONE);
        updateCropButton();

        resize();
        updateProgress();
    }

    protected void onListItemClick(int position, long id) {
        Item item = adapter.getItem(position);
        if (null != outlineListener) {
            outlineListener.selected(item.page, true);
        }
    }
}

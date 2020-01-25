package cn.archko.pdf.common;

import android.text.TextUtils;
import android.util.SparseArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import cn.archko.pdf.entity.APage;
import cn.archko.pdf.utils.StreamUtils;

/**
 * @author: archko 2020/1/1 :9:04 下午
 */
public class APageSizeLoader {

    public static final int PAGE_COUNT = 320;

    public static PageSizeBean loadPageSizeFromFile(int targetWidth, File file) {
        PageSizeBean pageSizeBean = null;
        try {
            String content = StreamUtils.readStringFromFile(file);
            if (!TextUtils.isEmpty(content)) {
                pageSizeBean = fromJson(targetWidth, new JSONObject(content));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pageSizeBean;
    }

    public static void savePageSizeToFile(boolean crop, SparseArray<APage> sparseArray, File file) {
        String content = toJson(crop, sparseArray);
        StreamUtils.saveStringToFile(content, file);
    }

    public static PageSizeBean fromJson(int targetWidth, JSONObject jo) {
        PageSizeBean pageSizeBean = new PageSizeBean();
        SparseArray<APage> sparseArray = fromJson(targetWidth, jo.optJSONArray("pagesize"));
        pageSizeBean.sparseArray = sparseArray;
        pageSizeBean.crop = jo.optBoolean("crop");
        return pageSizeBean;
    }

    public static SparseArray<APage> fromJson(int targetWidth, JSONArray ja) {
        SparseArray<APage> sparseArray = new SparseArray<>();
        for (int i = 0; i < ja.length(); i++) {
            sparseArray.put(i, APage.fromJson(targetWidth, ja.optJSONObject(i)));
        }

        return sparseArray;
    }

    public static String toJson(boolean crop, SparseArray<APage> sparseArray) {
        JSONObject jo = new JSONObject();
        try {
            jo.put("crop", crop);
            jo.put("pagesize", toJson(sparseArray));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jo.toString();
    }

    public static JSONArray toJson(SparseArray<APage> sparseArray) {
        JSONArray jsonArray = new JSONArray();
        APage aPage;
        for (int i = 0; i < sparseArray.size(); i++) {
            aPage = sparseArray.valueAt(i);
            jsonArray.put(aPage.toJson());
        }
        return jsonArray;
    }

    public static class PageSizeBean {
        public SparseArray<APage> sparseArray;
        public boolean crop;
    }
}

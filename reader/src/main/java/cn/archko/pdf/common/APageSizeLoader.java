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

    public static final int PAGE_COUNT = 250;

    public static PageSizeBean loadPageSizeFromFile(int targetWidth, int pageCount, long fileSize, File file) {
        PageSizeBean pageSizeBean = null;
        try {
            String content = StreamUtils.readStringFromFile(file);
            if (!TextUtils.isEmpty(content)) {
                pageSizeBean = fromJson(targetWidth, pageCount, fileSize, new JSONObject(content));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pageSizeBean;
    }

    public static void savePageSizeToFile(boolean crop, long fileSize, SparseArray<APage> sparseArray, File file) {
        String content = toJson(crop, fileSize, sparseArray);
        StreamUtils.saveStringToFile(content, file);
    }

    public static PageSizeBean fromJson(int targetWidth, int pageCount, long fileSize, JSONObject jo) {
        JSONArray ja = jo.optJSONArray("pagesize");
        if (ja.length() != pageCount) {
            Logcat.d("new pagecount:" + pageCount);
            return null;
        }
        if (fileSize != jo.optLong("filesize")) {
            Logcat.d("new filesize:" + fileSize);
            return null;
        }
        PageSizeBean pageSizeBean = new PageSizeBean();
        SparseArray<APage> sparseArray = fromJson(targetWidth, ja);
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

    public static String toJson(boolean crop, long fileSize, SparseArray<APage> sparseArray) {
        JSONObject jo = new JSONObject();
        try {
            jo.put("crop", crop);
            jo.put("filesize", fileSize);
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
        public int fileSize;
    }
}

package cn.archko.pdf.common;

import android.util.SparseArray;

import org.json.JSONArray;
import org.vudroid.core.codec.CodecPage;

import cn.archko.pdf.entity.APage;
import cn.archko.pdf.utils.StreamUtils;

/**
 * @author: archko 2020/1/1 :9:04 下午
 */
public class APageSizeLoader {

    public static SparseArray<APage> loadPageSizeFromFile(int targetWidth, String name) {
        SparseArray<APage> sparseArray = null;
        try {
            String content = StreamUtils.readStringFromFile(name);
            sparseArray = fromJson(targetWidth, new JSONArray(content));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sparseArray;
    }

    public static void savePageSizeToFile(JSONArray ja, String name) {
        StreamUtils.saveStringToFile(ja.toString(), name);
    }

    public static void savePageSizeToFile(SparseArray<APage> sparseArray, String name) {
        String content = toJson(sparseArray).toString();
        StreamUtils.saveStringToFile(content, name);
    }

    public static SparseArray<APage> fromJson(int targetWidth, JSONArray ja) {
        SparseArray<APage> sparseArray = new SparseArray<>();
        for (int i = 0; i < ja.length(); i++) {
            sparseArray.put(i, APage.fromJson(targetWidth, ja.optJSONObject(i)));
        }

        return sparseArray;
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
}

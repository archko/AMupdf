package cn.archko.pdf.common;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;

import cn.archko.pdf.entity.BookProgress;

/**
 * 存储最近阅读的记录
 *
 * @author: archko 2014/4/17 :15:05
 */
public class BookProgressParser {

    public static void addProgressToJson(BookProgress progress, JSONArray ja) {
        JSONObject tmp = new JSONObject();
        try {
            tmp.put("index", progress.index);
            tmp.put("path", URLEncoder.encode(progress.path));
            tmp.put("name", progress.name);
            tmp.put("ext", progress.ext);
            tmp.put("md5", progress.md5);
            tmp.put("pageCount", progress.pageCount);
            tmp.put("size", progress.size);
            tmp.put("firstTimestampe", progress.firstTimestampe);
            tmp.put("lastTimestampe", progress.lastTimestampe);
            tmp.put("readTimes", progress.readTimes);

            tmp.put("progress", progress.progress);
            tmp.put("page", progress.page);
            tmp.put("zoomLevel", progress.zoomLevel);
            tmp.put("rotation", progress.rotation);
            tmp.put("offsetX", progress.offsetX);
            tmp.put("offsetY", progress.offsetY);
            tmp.put("autoCrop", progress.autoCrop);
            tmp.put("reflow", progress.reflow);
            tmp.put("isFavorited", progress.isFavorited);
            tmp.put("inRecent", progress.inRecent);
            ja.put(tmp);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static BookProgress parseProgress(JSONObject jsonobject) throws Exception {
        if (null == jsonobject) {
            return null;
        }
        BookProgress bean = new BookProgress();
        try {
            bean.index = jsonobject.optInt("index");
            bean.path = URLDecoder.decode(jsonobject.optString("path"));
            bean.name = jsonobject.optString("name");
            bean.ext = jsonobject.optString("ext");
            bean.md5 = jsonobject.optString("md5");
            bean.pageCount = jsonobject.optInt("pageCount");
            bean.size = jsonobject.optLong("size");

            bean.firstTimestampe = jsonobject.optLong("firstTimestampe");
            bean.lastTimestampe = jsonobject.optLong("lastTimestampe");
            bean.readTimes = jsonobject.optInt("readTimes");

            bean.progress = jsonobject.optInt("progress");
            bean.page = jsonobject.optInt("page");
            bean.zoomLevel = jsonobject.optInt("zoomLevel");
            bean.rotation = jsonobject.optInt("rotation");
            bean.offsetX = jsonobject.optInt("offsetX");
            bean.offsetY = jsonobject.optInt("offsetY");
            bean.autoCrop = jsonobject.optInt("autoCrop");
            bean.reflow = jsonobject.optInt("reflow");
            bean.isFavorited = jsonobject.optInt("isFavorited");
            bean.inRecent = jsonobject.optInt("inRecent");
        } catch (Exception jsonexception) {
            throw new Exception(jsonexception.getMessage() + ":" + jsonobject, jsonexception);
        }
        return bean;
    }

    /**
     * @return
     * @throws WeiboException
     */
    public static ArrayList<BookProgress> parseProgresses(String jo) {
        ArrayList<BookProgress> arraylist = new ArrayList();
        int i = 0;

        try {
            JSONObject json = new JSONObject(jo);
            JSONArray jsonarray = json.optJSONArray("root");
            int len = jsonarray.length();
            BookProgress bean = null;
            for (; i < len; i++) {
                bean = parseProgress(jsonarray.optJSONObject(i));
                arraylist.add(bean);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arraylist;
    }
}

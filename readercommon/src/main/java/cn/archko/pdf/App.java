package cn.archko.pdf;

import android.app.Application;
import android.util.DisplayMetrics;

import com.jeremyliao.liveeventbus.LiveEventBus;
import com.umeng.commonsdk.UMConfigure;

import cn.archko.pdf.common.CrashHandler;
import cn.archko.pdf.common.RecentManager;

public class App extends Application {

    private final String appkey = "5c15f639f1f556978b0009c8";
    private static App mInstance = null;

    public int screenHeight = 720;
    public int screenWidth = 1080;
    public static Thread uiThread;

    public static App getInstance() {
        return mInstance;
    }

    public void onCreate() {
        super.onCreate();
        mInstance = this;
        uiThread = Thread.currentThread();
        RecentManager.getInstance().getRecentTableManager().open();
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler());

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        screenHeight = displayMetrics.heightPixels;
        screenWidth = displayMetrics.widthPixels;

        UMConfigure.init(this, appkey, "archko", UMConfigure.DEVICE_TYPE_PHONE, null);

        LiveEventBus
                .config()
                .supportBroadcast(this)
                .lifecycleObserverAlwaysActive(true)
                .autoClear(false);
    }

}

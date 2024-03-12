package cn.archko.pdf.common;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Insets;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * @author: archko 2024/3/12 :10:40
 */
public class StatusBarHelper {

    /**
     * 显示/隐藏 顶部状态栏
     *
     * @param window
     * @param fullScreen
     */
    public static void setFullScreenFlag(Window window, boolean fullScreen) {
        if (fullScreen) {
            hideStatusBar(window);
        } else {
            showStatusBar(window);
        }
    }

    public static void hideStatusBar(Window window) {
        WindowManager.LayoutParams attrs = window.getAttributes();
        attrs.flags = attrs.flags & WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
        attrs.flags = attrs.flags | WindowManager.LayoutParams.FLAG_FULLSCREEN;
        window.setAttributes(attrs);
        // NavigatiobBar透明切盖在内容上面
//            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }

    public static void showStatusBar(Window window) {
        WindowManager.LayoutParams attrs = window.getAttributes();
        attrs.flags = attrs.flags & WindowManager.LayoutParams.FLAG_FULLSCREEN;
        attrs.flags = attrs.flags | WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
        window.setAttributes(attrs);
    }

    /**
     * 获取状态栏高度
     */
    public static int getStatusBarHeight(Context ctx) {
        int h = getStatusBarHeight1(ctx);
        if (h == 0) {
            h = getStatusBarHeight2(ctx);
            //println(String.format("h2:%s", h))
        }
        if (h == 0) {
            h = getStatusBarHeight3(ctx);
            //println(String.format("h3:%s", h))
        }
        if (h == 0) {
            h = getStatusBarHeight4(ctx);
            //println(String.format("h4:%s", h))
        }
        return h;
    }

    private static int getStatusBarHeight1(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            WindowMetrics windowMetrics = wm.getCurrentWindowMetrics();
            WindowInsets windowInsets = windowMetrics.getWindowInsets();
            Insets insets =
                    windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars() | WindowInsets.Type.displayCutout());
            return insets.top;
        }
        return 0;
    }

    private static int getStatusBarHeight2(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    @SuppressLint("PrivateApi")
    private static int getStatusBarHeight3(Context ctx) {
        int statusBarHeight = 0;
        try {
            Class<?> clazz = Class.forName("com.android.internal.R$dimen");
            Object object = clazz.newInstance();
            int height = Integer.parseInt(clazz.getField("status_bar_height").get(object).toString());
            return ctx.getResources().getDimensionPixelSize(height);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusBarHeight;
    }

    private static int getStatusBarHeight4(Context context) {
        int statusBarHeight = (int) Math.ceil((38 * context.getResources().getDisplayMetrics().density));
        return statusBarHeight;
    }

    public static void hideSystemUI(Activity activity) {
        Window window = activity.getWindow();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {   //不加这句,systemBars()调用会导致顶部有一块黑的
            window.getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        WindowCompat.setDecorFitsSystemWindows(window, false);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    public static void showSystemUI(Activity activity) {
        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, true);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        controller.show(WindowInsetsCompat.Type.systemBars());
    }

    /**
     * @param activity
     * @param statusBarColor 状态栏的颜色
     */
    public static void setBackgroundColor(Activity activity, @ColorInt int statusBarColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { //5.0及以上
            int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            View decorView = activity.getWindow().getDecorView();
            decorView.setSystemUiVisibility(option);
            activity.getWindow().setStatusBarColor(statusBarColor);
        }
    }

    public static void setBackgroundColor(Window window, @ColorInt int statusBarColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { //5.0及以上
            int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            View decorView = window.getDecorView();
            decorView.setSystemUiVisibility(option);
            window.setStatusBarColor(statusBarColor);
        }
    }

    public static void setColorRes(Activity activity, @ColorRes int statusBarColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().setStatusBarColor(activity.getResources().getColor(statusBarColor));
        }
    }

    /**
     * 设置顶部状态栏的颜色
     *
     * @param activity    当前的页面
     * @param statusColor 状态栏的颜色值
     */
    public static void setStatusBarColor(Activity activity, int statusColor) {
        Window window = activity.getWindow();
        //取消状态栏透明
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        //添加Flag把状态栏设为可绘制模式
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        //设置状态栏颜色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(statusColor);
        }
        // 如果亮色，设置状态栏文字为黑色
        if (isLightColor(statusColor)) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } else {
            //设置系统状态栏处于可见状态
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
        //让view不根据系统窗口来调整自己的布局
        ViewGroup mContentView = window.findViewById(Window.ID_ANDROID_CONTENT);
        View mChildView = mContentView.getChildAt(0);
        if (mChildView != null) {
            mChildView.setFitsSystemWindows(false);
            ViewCompat.requestApplyInsets(mChildView);
        }
    }

    /**
     * 判断当前的状态栏是不是亮色
     *
     * @param color 当前的颜色
     * @return
     */
    public static boolean isLightColor(@ColorInt int color) {
        return ColorUtils.calculateLuminance(color) >= 0.5;
    }

    public static void setTextAndIconDark(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    public static void setTextAndIconLight(Activity activity) {
        activity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    /**
     * 设置沉浸式状态栏
     *
     * @param window
     */
    public static void setStatusBarImmerse(Window window) {
        WindowCompat.setDecorFitsSystemWindows(window, false);

        //设置专栏栏和导航栏的底色，透明
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.setNavigationBarDividerColor(Color.TRANSPARENT);
        }
    }

    /**
     * 设置沉浸后状态栏和导航字体的颜色
     *
     * @param window
     * @param isLight true,则字体是黑色的,false则白色
     */
    public static void setImmerseBarAppearance(Window window, boolean isLight) {
        WindowInsetsControllerCompat controller = ViewCompat.getWindowInsetsController(window.getDecorView());
        if (null != controller) {
            controller.setAppearanceLightStatusBars(isLight);
            controller.setAppearanceLightNavigationBars(isLight);
        }
    }
}
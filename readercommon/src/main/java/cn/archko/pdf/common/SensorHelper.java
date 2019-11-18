package cn.archko.pdf.common;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.preference.PreferenceManager;

/**
 * @author: archko 2018/7/22 :13:03
 */
public class SensorHelper {

    public final static String PREF_ORIENTATION = "orientation";
    public final static String PREF_PREV_ORIENTATION = "prevOrientation";
    private Activity activity;
    private SensorEventListener sensorEventListener = new SensorEventListener() {

        /**
         * Called when accuracy changes.
         * This method is empty, but it's required by relevant interface.
         */
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            gravity[0] = 0.8f * gravity[0] + 0.2f * event.values[0];
            gravity[1] = 0.8f * gravity[1] + 0.2f * event.values[1];
            gravity[2] = 0.8f * gravity[2] + 0.2f * event.values[2];

            float sq0 = gravity[0] * gravity[0];
            float sq1 = gravity[1] * gravity[1];
            float sq2 = gravity[2] * gravity[2];

            gravityAge++;

            if (gravityAge < 4) {
                // ignore initial hiccups
                return;
            }

            if (sq1 > 3 * (sq0 + sq2)) {
                if (gravity[1] > 4)
                    setOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                else if (gravity[1] < -4 && Integer.parseInt(Build.VERSION.SDK) >= 9)
                    setOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
            } else if (sq0 > 3 * (sq1 + sq2)) {
                if (gravity[0] > 4)
                    setOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                else if (gravity[0] < -4 && Integer.parseInt(Build.VERSION.SDK) >= 9)
                    setOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            }
        }
    };

    private android.hardware.SensorManager sensorManager;
    private float[] gravity = {0f, -9.81f, 0f};
    private long gravityAge = 0;
    private int prevOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

    public SensorHelper(Activity activity) {
        this.activity = activity;
    }

    private void setOrientation(int orientation) {
        if (orientation != prevOrientation) {
            activity.setRequestedOrientation(orientation);
            prevOrientation = orientation;
        }
    }

    public void onPause() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(sensorEventListener);
            sensorManager = null;
            SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(activity).edit();
            edit.putInt(PREF_PREV_ORIENTATION, prevOrientation);
            edit.apply();
        }
    }

    public void onResume() {
        SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(activity);

        if (null == sensorManager) {
            sensorManager = (SensorManager) activity.getSystemService(Activity.SENSOR_SERVICE);
        }
        if (setOrientation(activity)) {
            if (sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() > 0) {
                gravity[0] = 0f;
                gravity[1] = -9.81f;
                gravity[2] = 0f;
                gravityAge = 0;
                sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                        SensorManager.SENSOR_DELAY_NORMAL);
                prevOrientation = options.getInt(PREF_PREV_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                activity.setRequestedOrientation(prevOrientation);
            } else {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }
    }

    public static boolean setOrientation(Activity activity) {
        SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(activity);
        int orientation = Integer.parseInt(options.getString(PREF_ORIENTATION, "7"));
        switch (orientation) {
            case 0:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                break;
            case 1:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case 2:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case 3:
                int prev = options.getInt(SensorHelper.PREF_PREV_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                //Logcat.v(TAG, "restoring orientation: " + prev);
                activity.setRequestedOrientation(prev);
                return true;
            case 4:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                break;
            case 5:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                break;
            case 6:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                break;
            case 7:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                break;
            default:
                break;
        }
        return false;
    }
}

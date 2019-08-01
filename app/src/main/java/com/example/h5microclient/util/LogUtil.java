package com.example.h5microclient.util;

import android.util.Log;

public class LogUtil {
    private static String  LOG_TAG = "H5Client";

    public static void d(String log) {
        Log.d(LOG_TAG, log);
    }
    public static void d(String log, boolean show) {
        if (show){
            d(log);
        }
    }
}

package com.example.h5microclient;

import android.app.Application;

import com.example.h5microclient.util.LogUtil;

import ren.yale.android.cachewebviewlib.WebViewCacheInterceptor;
import ren.yale.android.cachewebviewlib.WebViewCacheInterceptorInst;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        WebViewCacheInterceptorInst.getInstance().init(new WebViewCacheInterceptor.Builder(this));
    }
}

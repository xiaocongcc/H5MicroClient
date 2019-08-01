package com.example.h5microclient;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import com.example.h5microclient.util.LogUtil;

import ren.yale.android.cachewebviewlib.WebViewCacheInterceptorInst;

public class MainActivity extends AppCompatActivity implements NetStateReceiver.NetworkObserver {

    WebView mWebView;
	final String GAME_URL = "https://www.baidu.com";
    final String INDEX_URL = "file:///android_asset/index.html";
    final String ERROR_URL = "file:///android_asset/error.html";

    private boolean isShowErrorPage = false;

	private NetStateReceiver receiver = new NetStateReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        receiver.setNetworkObserver(this);

        // 动态创建webview，为防止内存泄露，不在xml定义
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mWebView = new WebView(getApplicationContext());
        mWebView.setLayoutParams(params);
        ConstraintLayout mLayout = (ConstraintLayout)findViewById(R.id.root_layout);
        mLayout.addView(mWebView);

        // 设置webview支持JS
        mWebView.getSettings().setJavaScriptEnabled(true);
        // 开启DOM缓存
        mWebView.getSettings().setDomStorageEnabled(true);

        // 加载首页
        WebViewCacheInterceptorInst.getInstance().loadUrl(mWebView, INDEX_URL);

        mWebView.setWebViewClient(new WebViewClient(){
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                WebViewCacheInterceptorInst.getInstance().loadUrl(mWebView,request.getUrl().toString());
                return true;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                WebViewCacheInterceptorInst.getInstance().loadUrl(mWebView,url);
                return true;
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return  WebViewCacheInterceptorInst.getInstance().interceptRequest(request);
            }

            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                return  WebViewCacheInterceptorInst.getInstance().interceptRequest(url);
            }

            @Override
            public void  onPageStarted(WebView view, String url, Bitmap favicon) {
                //设定加载开始的操作
                LogUtil.d("onPageStarted, url = " + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                //设定加载结束的操作
                LogUtil.d("onPageFinished, url = " + url);

                // 首页加载完毕才开启网络监听
                if (url.length()>0 && url.contains("index.html")){
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
                            registerReceiver(receiver, filter);
                        }
                    }, 1500);
                }
            }

            // 新版本，只会在Android6.0及以上调用
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()){  // 是否是为 main frame创建
                    LogUtil.d("onReceivedError (android version above 6.0), code = " + error.getErrorCode() + ", desc = " + error.getDescription());
                    showErrorPage();
                }
            }

            // 旧版本，会在新版本中也可能被调用，所以加上一个判断，防止重复显示
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    return;
                }
                LogUtil.d("onReceivedError (android version below 6.0), code = " + errorCode + ", desc = " + description);
                showErrorPage();
            }

            // Android6.0以上版本判断404或者500
            @TargetApi(android.os.Build.VERSION_CODES.M)
            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                // 这个方法在6.0才出现
                int statusCode = errorResponse.getStatusCode();
                if (404 == statusCode || 500 == statusCode) {
                    LogUtil.d("deal http error(android version above 6.0), statusCode = " + statusCode);
                    showErrorPage();
                }
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient(){
            @Override
            // Android6.0以下版本判断404或者500
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                // android 6.0 以下通过title获取
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    if (title.contains("404") || title.contains("500") || title.contains("Error") || title.contains("找不到网页") || title.contains("网页无法打开")) {
                        LogUtil.d("deal http error(android version below 6.0), title = " + title);
                        showErrorPage();
                    }
                }
            }
        });
    }

    // 处理webview访问错误的情况
    public void showErrorPage(){
        if (isShowErrorPage){
            return;
        }
        isShowErrorPage = true;
        LogUtil.d("--------- show error page");
        WebViewCacheInterceptorInst.getInstance().loadUrl(mWebView, ERROR_URL);
    }

    // 网络连接成功才访问游戏主页
    @Override
    public void networkSuccess(){
        unregisterReceiver(receiver);
        WebViewCacheInterceptorInst.getInstance().loadUrl(mWebView, GAME_URL);
    }

    @Override
    public void networkFail(){
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 在 Activity 销毁（ WebView ）的时候，先让 WebView 加载null内容，然后移除 WebView，再销毁 WebView，最后置空
        if (mWebView != null) {
            mWebView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            mWebView.clearHistory();

            ((ViewGroup) mWebView.getParent()).removeView(mWebView);
            mWebView.destroy();
            mWebView = null;
        }

        unregisterReceiver(receiver);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        switch (keyCode){
            case KeyEvent.KEYCODE_BACK:
                exitGame();
                break;
            default:
                    break;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void exitGame(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("退出游戏");
        builder.setMessage("确定退出游戏吗？");
        builder.setCancelable(false);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                finish();
            }
        });
        builder.setNegativeButton("继续游戏", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}

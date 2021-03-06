package com.linda.answerhelper.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.linda.answerhelper.event.BitmapEvent;
import com.linda.answerhelper.event.StartCaptureEvent;
import com.linda.answerhelper.util.Util;
import com.linda.tool.R;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * Created by dahaolin on 2018/1/12.
 */

public class AnswerService extends Service {

    private final static String ABSOLUTE_PATH = Environment.getExternalStorageDirectory().getAbsoluteFile().getAbsolutePath();
    private static final int MESSAGE_RECOGNISE_RESULT = 101;
    private static final String KEY_RESULT = "result";

    private TessBaseAPI mBaseAPI;

    private FrameLayout mWebViewContainer;
    private WindowManager.LayoutParams mWebViewContainerParams;

    private Handler mHandler;
    private boolean isShowWebView;

    private WindowManager mWindowManager;

    public static class WebViewHandler extends Handler {

        WeakReference<FrameLayout> webViewWeakReference;

        WebViewHandler(FrameLayout webViewContainer) {
            webViewWeakReference = new WeakReference<FrameLayout>(webViewContainer);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MESSAGE_RECOGNISE_RESULT:
                    Bundle bundle = msg.getData();
                    String result = bundle.getString(KEY_RESULT);
                    if (!TextUtils.isEmpty(result) && webViewWeakReference.get() != null) {
                        if (result.contains(".")) {
                            int index = result.indexOf(".");
                            result = result.substring(index + 1);
                        }
                        ((WebView)webViewWeakReference.get().findViewById(R.id.over_layer_webview)).loadUrl("https://wap.sogou.com/web/searchList.jsp?keyword=" + result);
                        webViewWeakReference.get().findViewById(R.id.over_layer_progress).setVisibility(View.GONE);
                    }
                    break;
            }
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        EventBus.getDefault().register(this);

        mBaseAPI = new TessBaseAPI();
        mBaseAPI.init(ABSOLUTE_PATH + File.separator, "chi_sim");

        initWebViewContainerParams();
        initTouchBtn();
    }

    private void initWebViewContainerParams() {
        mWebViewContainerParams = new WindowManager.LayoutParams();
        mWindowManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mWebViewContainerParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            mWebViewContainerParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        mWebViewContainerParams.format = PixelFormat.RGBA_8888;
        mWebViewContainerParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mWebViewContainerParams.gravity = Gravity.START | Gravity.BOTTOM;
        mWebViewContainerParams.x = 0;
        mWebViewContainerParams.y = 0;
        mWebViewContainerParams.width = Util.getScreenSize(this)[0];
        mWebViewContainerParams.height = dp2px(300);

        LayoutInflater inflater = LayoutInflater.from(getApplication());
        //获取浮动窗口视图所在布局.
        mWebViewContainer = (FrameLayout) inflater.inflate(R.layout.overlayer_layout, null);
        ImageButton mBtnRecognition = mWebViewContainer.findViewById(R.id.over_layer_button);
        mBtnRecognition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mWebViewContainer.findViewById(R.id.over_layer_progress).setVisibility(View.VISIBLE);
                EventBus.getDefault().post(new StartCaptureEvent());
            }
        });

        WebView mWebView = mWebViewContainer.findViewById(R.id.over_layer_webview);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setSupportMultipleWindows(true);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.setWebChromeClient(new WebChromeClient());

        mHandler = new WebViewHandler(mWebViewContainer);
    }

    private void initTouchBtn() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        mWindowManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        params.format = PixelFormat.RGBA_8888;
        //设置flags.不可聚焦及不可使用按钮对悬浮窗进行操控.
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        params.gravity = Gravity.START | Gravity.BOTTOM;
        params.x = 0;
        params.y = dp2px(300);
        params.width = 100;
        params.height = 100;

        LayoutInflater inflater = LayoutInflater.from(getApplication());
        //获取浮动窗口视图所在布局.
        View touchLayout = inflater.inflate(R.layout.toucher_btn_layout, null);
        final ImageButton imageButton = touchLayout.findViewById(R.id.touch_btn);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isShowWebView) {
                    imageButton.setBackgroundResource(R.drawable.rightsquare);
                    dismiss();
                } else {
                    imageButton.setBackgroundResource(R.drawable.leftsquare);
                    show();
                }
            }
        });
        mWindowManager.addView(touchLayout, params);

    }

    private void show() {
        isShowWebView = true;
        mWindowManager.addView(mWebViewContainer, mWebViewContainerParams);
    }

    private void dismiss() {
        isShowWebView = false;
        mWindowManager.removeViewImmediate(mWebViewContainer);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onGetBitmapEvent(BitmapEvent event) {
        recognise(event.getBitmap());
    }

    private void recognise(final Bitmap bitmap) {
        new Thread() {
            @Override
            public void run() {
                if (bitmap != null) {
                    mBaseAPI.setImage(bitmap);
                    String result = mBaseAPI.getUTF8Text();

                    Message message = new Message();
                    message.what = MESSAGE_RECOGNISE_RESULT;
                    Bundle bundle = new Bundle();
                    bundle.putString(KEY_RESULT, result);
                    message.setData(bundle);
                    mHandler.sendMessage(message);

                    bitmap.recycle();
                }
            }
        }.start();
    }

    private int dp2px(int dp){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getApplication().getResources().getDisplayMetrics());
    }

}

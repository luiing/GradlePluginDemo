package interprocess.uis.com.web_demo;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

import com.bailian.weblib.bljsbridge.BridgeWebView;
import com.bailian.weblib.bljsbridge.BridgeWebViewClient;
import com.bailian.weblib.bljsbridge.IJSCallFunction;
import com.bailian.weblib.bljsbridge.INativeCallBack;
import com.bailian.weblib.bljsbridge.JSEntity;
import com.tencent.sonic.sdk.SonicCacheInterceptor;
import com.tencent.sonic.sdk.SonicConfig;
import com.tencent.sonic.sdk.SonicConstants;
import com.tencent.sonic.sdk.SonicEngine;
import com.tencent.sonic.sdk.SonicSession;
import com.tencent.sonic.sdk.SonicSessionConfig;
import com.tencent.sonic.sdk.SonicSessionConnection;
import com.tencent.sonic.sdk.SonicSessionConnectionInterceptor;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import interprocess.uis.com.web_demo.vassonic.SonicJavaScriptInterface;
import interprocess.uis.com.web_demo.vassonic.SonicRuntimeImpl;
import interprocess.uis.com.web_demo.vassonic.SonicSessionClientImpl;

/**
 * @author uis on 2017/12/11.
 */

public class VasSonicWebViewActivity extends Activity{

    final static String TAG = "WebViewAct";

    public static String url = "file:///android_asset/demo.html";
    BridgeWebView webView;
    private SonicSession sonicSession;

    public <T extends View> T id(int id){
        return (T)findViewById(id);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        url = intent.getStringExtra("url");
        if(TextUtils.isEmpty(url)){
            finish();
            return;
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        // step 1: Initialize sonic engine if necessary, or maybe u can do this when application created
        if (!SonicEngine.isGetInstanceAllowed()) {
            SonicEngine.createInstance(new SonicRuntimeImpl(getApplication()), new SonicConfig.Builder().build());
        }
        SonicSessionClientImpl sonicSessionClient = null;

        // step 2: Create SonicSession
        sonicSession = SonicEngine.getInstance().createSession(url,
                new SonicSessionConfig.Builder().setSupportLocalServer(true).build());
        if (null != sonicSession) {
            sonicSession.bindClient(sonicSessionClient = new SonicSessionClientImpl());
        } else {
            // this only happen when a same sonic session is already running,
            // u can comment following codes to feedback as a default mode.
            //throw new UnknownError("create session fail!");
            Toast.makeText(this, "create sonic session fail!", Toast.LENGTH_LONG).show();
        }

        // step 3: BindWebView for sessionClient and bindClient for SonicSession
        // in the real world, the init flow may cost a long time as startup
        // runtime、init configs....
        setContentView(R.layout.activity_webview);
        Button btRefresh = id(R.id.bt_refresh);
        btRefresh.setVisibility(View.GONE);
        webView = id(R.id.webview);
        webView.setWebViewClient(new BridgeWebViewClient(webView){
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (sonicSession != null && sonicSession.getSessionClient()!=null) {
                    sonicSession.getSessionClient().pageFinish(url);
                }
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (sonicSession != null && sonicSession.getSessionClient()!=null) {
                    //step 6: Call sessionClient.requestResource when host allow the application
                    // to return the local data .
                    return (WebResourceResponse) sonicSession.getSessionClient().requestResource(url);
                }
                return null;//super.shouldInterceptRequest(view, url);
            }

            @TargetApi(21)
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return shouldInterceptRequest(view, request.getUrl().toString());
            }
        });
        //setting start------
        // step 4: bind javascript
        // note:if api level lower than 17(android 4.2), addJavascriptInterface has security
        // issue, please use x5 or see https://developer.android.com/reference/android/webkit/
        // WebView.html#addJavascriptInterface(java.lang.Object, java.lang.String)
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.removeJavascriptInterface("searchBoxJavaBridge_");
        //intent.putExtra(SonicJavaScriptInterface.PARAM_LOAD_URL_TIME, System.currentTimeMillis());
        //webView.addJavascriptInterface(new SonicJavaScriptInterface(sonicSessionClient, intent), "sonic");
        // init webview settings
        webSettings.setAllowContentAccess(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setSavePassword(false);
        webSettings.setSaveFormData(false);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        WebSettings settings = webView.getSettings();
        //settings.setJavaScriptEnabled(true);
        //settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT > 20) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        //settings.setDomStorageEnabled(true);
        //settings.setDatabaseEnabled(true);
        //setting end---------
        /**
         * 当js使用LoadMethod方法时，methodName需要定义为 tagName#methodName
         * 当js使用LoadAPI方法时,methodName直接为名称即可
         * entity.success()返回成功，entity.fail()返回失败
         */

        webView.registerFunction("tag#methodName", new INativeCallBack() {
            @Override
            public void onCall(String method, String data, String url, IJSCallFunction call) {
                Log.e(TAG,data);
                JSEntity entity = new JSEntity();
                entity.data = "{\"name\":\"lily\",\"function\":\"method\"}";
                entity.success();
                call.onCall(entity,url);
            }
        });
        webView.registerFunction("apiName", new INativeCallBack() {
            @Override
            public void onCall(String method, String data, String url, IJSCallFunction call) {
                Log.e(TAG,data);
                JSEntity entity = new JSEntity();
                entity.data = "{\"name\":\"lucy\",\"function\":\"api\"}";
                entity.success();
                call.onCall(entity,url);
            }
        });
        // step 5: webview is ready now, just tell session client to bind
        if (sonicSessionClient != null) {
            sonicSessionClient.bindWebView(webView);
            sonicSessionClient.clientReady();
        } else { // default mode
            webView.loadUrl(url);
        }
        btRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //webView.loadUrl(url);
                test();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (null != sonicSession) {
            sonicSession.destroy();
            sonicSession = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if(webView.canGoBack()){
            webView.goBack();
        }else {
            super.onBackPressed();
        }
    }

    private void test(){

    }
}

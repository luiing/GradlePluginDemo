package interprocess.uis.com.web_demo;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.widget.Button;

import com.bailian.weblib.bljsbridge.BridgeWebView;
import com.bailian.weblib.bljsbridge.IJSCallFunction;
import com.bailian.weblib.bljsbridge.INativeCallBack;
import com.bailian.weblib.bljsbridge.JSEntity;

/**
 * @author uis on 2017/12/11.
 */

public class WebViewActivity extends Activity{

    final static String TAG = "WebViewAct";

    public static String url = "file:///android_asset/demo.html";
    BridgeWebView webView;

    public <T extends View> T id(int id){
        return (T)findViewById(id);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        Intent intent = getIntent();
        url = intent.getStringExtra("url");
        if(TextUtils.isEmpty(url)){
            finish();
        }
        setContentView(R.layout.activity_webview);
        Button btRefresh = id(R.id.bt_refresh);
        btRefresh.setVisibility(View.GONE);
        webView = id(R.id.webview);
        //setting start------
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT > 20) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
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
        webView.loadUrl(url);
        btRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //webView.loadUrl(url);
                test();
            }
        });
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

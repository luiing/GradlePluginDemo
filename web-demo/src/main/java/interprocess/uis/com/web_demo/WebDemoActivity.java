package interprocess.uis.com.web_demo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.tencent.sonic.sdk.SonicConfig;
import com.tencent.sonic.sdk.SonicEngine;
import com.tencent.sonic.sdk.SonicSession;
import com.tencent.sonic.sdk.SonicSessionConfig;

import java.io.File;
import java.util.List;

import interprocess.uis.com.web_demo.vassonic.SonicRuntimeImpl;

/**
 * @author uis on 2018/6/20.
 */
public class WebDemoActivity extends Activity implements View.OnClickListener{

    String[] URLS = new String[]{
            "https://m.baidu.com",
            "https://m.bl.com",
            "https://m.jd.com",
            "https://sina.cn",
            "https://xw.qq.com",
    };
    Button btMode;
    boolean isVasSonic = false;
    boolean isFirst = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PermissionProxy.requestPermission(this, 100, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, new PermissionProxy.OnPermissionResult() {
            @Override
            public void onResult(boolean success, int requestCode, @NonNull List<String> allow, @NonNull List<String> deny) {
                if(success){
                    preLoaderPages();
                }
            }
        });

        setContentView(R.layout.activity_webdemo);
        getView(R.id.bt_baidu).setOnClickListener(this);
        getView(R.id.bt_bailian).setOnClickListener(this);
        getView(R.id.bt_jd).setOnClickListener(this);
        getView(R.id.bt_sina).setOnClickListener(this);
        getView(R.id.bt_qq).setOnClickListener(this);
        btMode = getView(R.id.bt_mode);
        btMode.setOnClickListener(this);
        change();

    }

    void change(){
        btMode.setText(isVasSonic ? "vasSonic":"webView");
        if(isFirst){
            isFirst = false;
            return;
        }
        try {
            // 来自相机
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // 下面这句指定调用相机拍照后的照片存储的路径
            //getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            File file = new File(Environment.getExternalStorageDirectory(),""+System.currentTimeMillis());
            Uri fileUri = Uri.fromFile(file);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                String sFileProvider = String.format("%s.fileprovider", getPackageName());
                fileUri = FileProvider.getUriForFile(this, sFileProvider, file);
                cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
            //startActivityForResult(cameraIntent, 100);// CAMERA_OK是用作判断返回结果的标识
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void preLoaderPages(){
        if (!SonicEngine.isGetInstanceAllowed()) {
            SonicEngine.createInstance(new SonicRuntimeImpl(getApplication()), new SonicConfig.Builder().build());
        }

        for(String url:URLS){
            SonicEngine.getInstance().preCreateSession(url, new SonicSessionConfig.Builder().setSupportLocalServer(true).build());
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        int type = 0;
        if(R.id.bt_baidu == id){

        }else if(R.id.bt_bailian == id){
            type = 1;
        }else if(R.id.bt_jd == id){
            type = 2;
        }else if(R.id.bt_sina == id){
            type = 3;
        }else if(R.id.bt_qq == id){
            type = 4;
        }else if(R.id.bt_mode == id){
            type = -1;
            isVasSonic = !isVasSonic;
            change();
        }
        if(type >= 0) {
            String url = URLS[type];
            if(isVasSonic) {
                SonicEngine.getInstance().preCreateSession(url, new SonicSessionConfig.Builder().setSupportLocalServer(true).build());
            }
            Intent intent = new Intent(this, isVasSonic ?  VasSonicWebViewActivity.class : WebViewActivity.class);
            intent.putExtra("url", url);
            startActivity(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    <T extends View> T getView(int id){
        return (T)findViewById(id);
    }
}

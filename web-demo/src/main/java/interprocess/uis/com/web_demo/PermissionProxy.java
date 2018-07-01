package interprocess.uis.com.web_demo;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.ArrayMap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author uis on 2018/6/22.
 */
public class PermissionProxy{

    public interface OnPermissionResult{
        void onResult(boolean success,int requestCode,@NonNull List<String> allow,@NonNull List<String> deny);
    }

    static ArrayMap<Activity,OnPermissionResult> callMap = new ArrayMap<>();


    static AtomicInteger code = new AtomicInteger(701);

    public static void removePermission(Activity act){
        callMap.remove(act);
    }

    public static void test(){

    }

    public static void resultPermission(Activity act,int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        OnPermissionResult result = callMap.get(act);
        if(result != null){
            List<String> allow = new ArrayList<>();
            List<String> deny = new ArrayList<>();
            for(int i=0,size=permissions.length;i<size;i++){
                if(-1 == grantResults[i]){//deny
                    deny.add(permissions[i]);
                }else{
                    allow.add(permissions[i]);
                }
            }
            result.onResult(0 == deny.size(),requestCode,allow,deny);
        }
        callMap.remove(act);
    }

    public static void requestPermission(Activity act,int requestCode, String[] permissions,OnPermissionResult result){
        if(result != null && permissions != null && permissions.length>0) {
            ArrayList<String> array = new ArrayList<>();
            for(String per : permissions) {
                if(-1 == ActivityCompat.checkSelfPermission(act, per)){//deny
                    array.add(per);
                }
            }
            if(array.size() > 0){
                callMap.put(act,result);
                String[] res = new String[array.size()];
                array.toArray(res);
                ActivityCompat.requestPermissions(act,res,requestCode);
            }else{//success
                List<String> allow = Arrays.asList(permissions);
                result.onResult(true,requestCode,allow,new ArrayList<String>());
            }
        }
    }
}

package interprocess.uis.com.lib_demo;

import android.view.View;

/**
 * @author uis on 2018/7/6.
 */
public class LibListenerImpl implements LibListener,View.OnClickListener {

    public void test(){
        System.out.print("test impl");
    }

    public void testEmpty(){

    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onLib() {

    }
}

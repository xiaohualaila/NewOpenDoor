package ug.newopendoor;


import android.app.Application;
import android.content.Context;

import com.cmm.rkgpiocontrol.rkGpioControlNative;

import ug.newopendoor.util.SharedPreferencesUtil;
import ug.newopendoor.util.SoundPoolUtil;

/**
 * Created by xyuxiao on 2016/9/23.
 */
public class MyApplication extends Application {
    private static Context mContext;
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        SoundPoolUtil.getInstance(this);
    }
    public static Context getContext() {
        return mContext;
    }

}

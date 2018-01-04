package ug.newopendoor.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.cmm.rkgpiocontrol.rkGpioControlNative;

import ug.newopendoor.activity.camera2.CameraActivity2;
import ug.newopendoor.util.CommonUtil;

/**
 * Created by Administrator on 2017/11/24.
 */

public class ScreenService extends Service {

    private final int TIME =1000;
    private boolean isAuto = true;
    private Thread thread;
    @Override
    public void onCreate() {
        super.onCreate();
        thread = new Thread(runnable);
        thread.start();
    }

    Runnable runnable=new Runnable() {
        @Override
        public void run() {
            while (isAuto) {{
                // TODO: 2018/1/4 添加哪个IO口
                int val = rkGpioControlNative.ReadGpio(1);
                Log.i("sss",">>>>" + val);
                if(val == 1){
                    boolean isTop = CommonUtil.isForeground(ScreenService.this, CameraActivity2.class.getName());
                    if(!isTop){
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_LAUNCHER);
                        intent.setClass(ScreenService.this,CameraActivity2.class);
                        intent.setFlags(intent.FLAG_ACTIVITY_CLEAR_TOP|intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                }
                try {
                    Thread.sleep(TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }}
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        isAuto = false;
        thread.stop();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}

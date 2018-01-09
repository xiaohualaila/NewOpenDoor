package ug.newopendoor.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.cmm.rkadcreader.adcNative;
import com.cmm.rkgpiocontrol.rkGpioControlNative;

import ug.newopendoor.R;
import ug.newopendoor.activity.camera2.CameraActivity2;
import ug.newopendoor.rx.RxBus;
import ug.newopendoor.service.ScreenService;
import ug.newopendoor.util.CommonUtil;
import ug.newopendoor.util.MyMessage;


/**
 * Created by Administrator on 2018/1/4.
 */

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startService(new Intent(this, ScreenService.class));
        RxBus.getDefault().toObserverable(MyMessage.class).subscribe(myMessage -> {
           int num = myMessage.getNum();
           if(num == 0){
            startActivity(new Intent(this, CameraActivity2.class));
            finish();
           }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService( new Intent(this, ScreenService.class));

    }
}

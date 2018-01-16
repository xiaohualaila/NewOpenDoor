package ug.newopendoor.broadcastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ug.newopendoor.activity.SetupActivity;
import ug.newopendoor.activity.camera.CameraActivity;
import ug.newopendoor.activity.camera2.CameraActivity2;
import ug.newopendoor.activity.camera3.FragmentActivity;


/**
 * Created by admin on 2017/11/2.
 */

public class BootBroadcastReceiver extends BroadcastReceiver {
    public static final String ACTION = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION)) {
            //Intent in = new Intent(context, CameraActivity.class);
            Intent in = new Intent(context, FragmentActivity.class);
            in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(in);
        }
    }
}

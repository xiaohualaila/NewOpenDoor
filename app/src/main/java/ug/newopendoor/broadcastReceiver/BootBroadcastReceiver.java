package ug.newopendoor.broadcastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ug.newopendoor.activity.camera.CameraActivity;
import ug.newopendoor.activity.camera2.CameraActivity2;
import ug.newopendoor.activity.camera3.FragmentActivity;
import ug.newopendoor.activity.camera4.FragmentActivity2;
import ug.newopendoor.activity.camera5.CameraActivity5;
import ug.newopendoor.activity.camera6.CameraActivity6;
import ug.newopendoor.activity.setup.SetupActivity;


/**
 * Created by admin on 2017/11/2.
 */

public class BootBroadcastReceiver extends BroadcastReceiver {
    public static final String ACTION = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION)) {
            Intent in = new Intent(context, SetupActivity.class);
            // Intent in = new Intent(context, FragmentActivity.class);
            in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(in);
        }
    }
}

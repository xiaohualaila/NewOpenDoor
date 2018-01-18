package ug.newopendoor.broadcastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ug.newopendoor.activity.camera4.FragmentActivity2;


/**
 * Created by admin on 2017/11/2.
 */

public class BootBroadcastReceiver extends BroadcastReceiver {
    public static final String ACTION = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION)) {
         //   Intent in = new Intent(context, CameraActivity.class);
            Intent in = new Intent(context, FragmentActivity2.class);
            in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(in);
        }
    }
}

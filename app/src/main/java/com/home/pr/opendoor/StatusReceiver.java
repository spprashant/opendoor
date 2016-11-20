package com.home.pr.opendoor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Pr on 19/11/2016.
 */
public class StatusReceiver extends BroadcastReceiver {
    public static int REQUEST_CODE = 12345;

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context,AdafruitDoorStatus.class);
        context.startService(i);
    }
}

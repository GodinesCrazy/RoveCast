package com.ivanmarty.radiola.ui.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

    @androidx.media3.common.util.UnstableApi
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            AlarmScheduler scheduler = new AlarmScheduler(context);
            AlarmInfo alarmInfo = scheduler.load();

            if (alarmInfo.isEnabled()) {
                scheduler.schedule(alarmInfo);
            }
        }
    }
}

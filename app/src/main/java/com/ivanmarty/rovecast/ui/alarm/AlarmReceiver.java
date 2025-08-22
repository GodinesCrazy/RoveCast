package com.ivanmarty.rovecast.ui.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import com.ivanmarty.rovecast.player.PlaybackService;

public class AlarmReceiver extends BroadcastReceiver {

    @androidx.media3.common.util.UnstableApi
    @Override
    public void onReceive(Context context, Intent intent) {
        // Cuando se dispara la alarma, se envía un comando para iniciar el PlaybackService.
        Intent serviceIntent = new Intent(context, PlaybackService.class);
        serviceIntent.setAction(PlaybackService.ACTION_PLAY);

        // Pasa los extras de la emisora desde el intent original al nuevo intent del servicio
        if (intent.getExtras() != null) {
            serviceIntent.putExtras(intent.getExtras());
        }

        ContextCompat.startForegroundService(context, serviceIntent);
    }
}

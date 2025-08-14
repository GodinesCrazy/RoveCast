package com.ivanmarty.radiola.ui.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ivanmarty.radiola.ui.PlayerActivity;

public class AlarmReceiver extends BroadcastReceiver {

    @androidx.media3.common.util.UnstableApi
    @Override
    public void onReceive(Context context, Intent intent) {
        // Cuando se dispara la alarma, lanza la PlayerActivity con los datos de la emisora
        Intent playerIntent = new Intent(context, PlayerActivity.class);
        playerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Pasa los extras de la emisora desde el intent original al nuevo
        if (intent.getExtras() != null) {
            playerIntent.putExtras(intent.getExtras());
        }

        context.startActivity(playerIntent);
    }
}

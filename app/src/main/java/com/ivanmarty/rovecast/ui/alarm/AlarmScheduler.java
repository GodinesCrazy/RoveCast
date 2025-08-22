package com.ivanmarty.rovecast.ui.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.ivanmarty.rovecast.model.Station;
import com.ivanmarty.rovecast.player.PlaybackService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AlarmScheduler {

    private static final String PREFS_NAME = "RoveCastAlarm";
    private static final String KEY_ENABLED = "alarm_enabled";
    private static final String KEY_HOUR = "alarm_hour";
    private static final String KEY_MINUTE = "alarm_minute";
    private static final String KEY_STATION_NAME = "alarm_station_name";
    private static final String KEY_STATION_URL = "alarm_station_url";
    private static final String KEY_STATION_LOGO = "alarm_station_logo";
    private static final String KEY_DAYS = "alarm_days";

    private final Context context;
    private final SharedPreferences prefs;

    public AlarmScheduler(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @androidx.media3.common.util.UnstableApi
    public void schedule(AlarmInfo alarm) {
        save(alarm);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = createPendingIntent(alarm);

        if (alarm.isEnabled()) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, alarm.getHour());
            calendar.set(Calendar.MINUTE, alarm.getMinute());
            calendar.set(Calendar.SECOND, 0);

            // Si la hora ya pasó hoy, programarla para mañana
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                } else {
                    // Opcional: redirigir al usuario a la configuración para conceder el permiso
                    Log.w("AlarmScheduler", "No se pueden programar alarmas exactas. El permiso no ha sido concedido.");
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
        } else {
            alarmManager.cancel(pendingIntent);
        }
    }

    @androidx.media3.common.util.UnstableApi
    public void cancel() {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = createPendingIntent(load()); // Cargar para obtener los datos correctos
        alarmManager.cancel(pendingIntent);
    }

    public AlarmInfo load() {
        boolean enabled = prefs.getBoolean(KEY_ENABLED, false);
        int hour = prefs.getInt(KEY_HOUR, 7); // 7 AM por defecto
        int minute = prefs.getInt(KEY_MINUTE, 0);
        String stationName = prefs.getString(KEY_STATION_NAME, null);
        String stationUrl = prefs.getString(KEY_STATION_URL, null);
        String stationLogo = prefs.getString(KEY_STATION_LOGO, null);
        String daysStr = prefs.getString(KEY_DAYS, "");
        List<Integer> days = new ArrayList<>();
        if (!daysStr.isEmpty()) {
            String[] daysArray = daysStr.split(",");
            for (String day : daysArray) {
                days.add(Integer.parseInt(day));
            }
        }
        return new AlarmInfo(enabled, hour, minute, stationName, stationUrl, stationLogo, days);
    }

    private void save(AlarmInfo alarm) {
        StringBuilder daysStr = new StringBuilder();
        if (alarm.getDays() != null) {
            for (int i = 0; i < alarm.getDays().size(); i++) {
                daysStr.append(alarm.getDays().get(i));
                if (i < alarm.getDays().size() - 1) {
                    daysStr.append(",");
                }
            }
        }

        prefs.edit()
                .putBoolean(KEY_ENABLED, alarm.isEnabled())
                .putInt(KEY_HOUR, alarm.getHour())
                .putInt(KEY_MINUTE, alarm.getMinute())
                .putString(KEY_STATION_NAME, alarm.getStationName())
                .putString(KEY_STATION_URL, alarm.getStationUrl())
                .putString(KEY_STATION_LOGO, alarm.getStationLogo())
                .putString(KEY_DAYS, daysStr.toString())
                .apply();
    }

    @androidx.media3.common.util.UnstableApi
    private PendingIntent createPendingIntent(AlarmInfo alarm) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        // Ahora usamos los extras del PlaybackService
        intent.putExtra(PlaybackService.EXTRA_URL, alarm.getStationUrl());
        intent.putExtra(PlaybackService.EXTRA_NAME, alarm.getStationName());
        intent.putExtra(PlaybackService.EXTRA_LOGO, alarm.getStationLogo());
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
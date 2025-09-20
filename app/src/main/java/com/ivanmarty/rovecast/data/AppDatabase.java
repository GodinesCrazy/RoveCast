package com.ivanmarty.rovecast.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.ivanmarty.rovecast.model.Station;

@Database(entities = {FavoriteStation.class, Station.class, SleepTimerPreset.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract FavoriteDao favoriteDao();
    public abstract StationDao stationDao();
    public abstract SleepTimerPresetDao sleepTimerPresetDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase get(Context ctx) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    ctx.getApplicationContext(),
                                    AppDatabase.class,
                                    "rovecast.db")
                            .fallbackToDestructiveMigration() // Permite la actualización de versión sin migraciones complejas
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

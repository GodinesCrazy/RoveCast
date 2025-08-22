package com.ivanmarty.rovecast.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sleep_timer_presets")
public class SleepTimerPreset {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;

    public long durationMinutes;
}

package com.ivanmarty.rovecast.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SleepTimerPresetDao {

    @Query("SELECT * FROM sleep_timer_presets ORDER BY durationMinutes ASC")
    LiveData<List<SleepTimerPreset>> getAll();

    @Insert
    void insert(SleepTimerPreset preset);

    @Update
    void update(SleepTimerPreset preset);

    @Delete
    void delete(SleepTimerPreset preset);
}

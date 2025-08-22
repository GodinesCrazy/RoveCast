package com.ivanmarty.rovecast.data;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SleepTimerPresetRepository {

    private final SleepTimerPresetDao dao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    public SleepTimerPresetRepository(Context ctx) {
        this.dao = AppDatabase.get(ctx).sleepTimerPresetDao();
    }

    public LiveData<List<SleepTimerPreset>> getAll() {
        return dao.getAll();
    }

    public void insert(SleepTimerPreset preset) {
        io.submit(() -> dao.insert(preset));
    }

    public void update(SleepTimerPreset preset) {
        io.submit(() -> dao.update(preset));
    }

    public void delete(SleepTimerPreset preset) {
        io.submit(() -> dao.delete(preset));
    }
}

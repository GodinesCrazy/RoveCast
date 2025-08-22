package com.ivanmarty.rovecast.data;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.ivanmarty.rovecast.model.Station;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FavoriteRepository {

    private final FavoriteDao dao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    public FavoriteRepository(Context ctx) {
        this.dao = AppDatabase.get(ctx).favoriteDao();
    }

    public LiveData<List<FavoriteStation>> getFavorites() { return dao.getAll(); }

    public void toggleFavorite(Station s, boolean makeFav) {
        io.submit(() -> {
            if (makeFav) {
                FavoriteStation f = new FavoriteStation();
                f.stationuuid = s.stationuuid != null ? s.stationuuid : s.getUrl(); // fallback
                f.name = s.name;
                f.url = s.getUrl();
                f.favicon = s.favicon;
                f.country = s.country;
                dao.upsert(f);
            } else {
                String id = (s.stationuuid != null ? s.stationuuid : s.getUrl());
                dao.deleteById(id);
            }
        });
    }

    public boolean isFavoriteNow(String uuid) {
        try {
            return io.submit(() -> dao.isFavoriteNow(uuid)).get();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

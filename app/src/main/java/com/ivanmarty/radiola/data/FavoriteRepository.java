package com.ivanmarty.radiola.data;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.ivanmarty.radiola.model.Station;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FavoriteRepository {

    private final FavoriteDao dao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    public FavoriteRepository(Context ctx) {
        this.dao = AppDatabase.get(ctx).favorites();
    }

    public LiveData<List<FavoriteStation>> getFavorites() { return dao.getAll(); }

    public void toggleFavorite(Station s, boolean makeFav) {
        io.submit(() -> {
            if (makeFav) {
                FavoriteStation f = new FavoriteStation();
                f.stationuuid = s.stationuuid != null ? s.stationuuid : s.url; // fallback
                f.name = s.name;
                f.url = s.url;
                f.favicon = s.favicon;
                f.country = s.country;
                dao.upsert(f);
            } else {
                String id = (s.stationuuid != null ? s.stationuuid : s.url);
                dao.deleteById(id);
            }
        });
    }
}

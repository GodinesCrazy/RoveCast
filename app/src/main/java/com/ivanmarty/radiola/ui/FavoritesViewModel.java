package com.ivanmarty.radiola.ui;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.ivanmarty.radiola.data.FavoriteStation;
import com.ivanmarty.radiola.repo.StationRepository;
import java.util.List;

public class FavoritesViewModel extends AndroidViewModel {
    private final StationRepository repo;
    private final LiveData<List<FavoriteStation>> favorites;

    public FavoritesViewModel(@NonNull Application app) {
        super(app);
        repo = new StationRepository(app);
        favorites = repo.getFavorites();
    }

    public LiveData<List<FavoriteStation>> getFavorites() { return favorites; }
    public StationRepository getRepo() { return repo; }
}

package com.ivanmarty.rovecast.ui;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.ivanmarty.rovecast.data.FavoriteRepository;
import com.ivanmarty.rovecast.repo.StationRepository;
import com.ivanmarty.rovecast.model.Station;

import java.util.List;

public class SearchViewModel extends AndroidViewModel {
    private final FavoriteRepository favRepo;
    private final StationRepository stationRepo;

    public SearchViewModel(@NonNull Application app) {
        super(app);
        favRepo = new FavoriteRepository(app);
        stationRepo = new StationRepository(app);
    }

    public LiveData<List<Station>> search(String q) {
        return stationRepo.searchByName(q);
    }

    public FavoriteRepository getRepo() { return favRepo; }
}

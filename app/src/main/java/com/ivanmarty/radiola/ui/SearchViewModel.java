package com.ivanmarty.radiola.ui;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.ivanmarty.radiola.data.FavoriteRepository;
import com.ivanmarty.radiola.data.StationsRepository;
import com.ivanmarty.radiola.model.Station;
import com.ivanmarty.radiola.util.ServiceLocator;

import java.util.List;

public class SearchViewModel extends AndroidViewModel {
    private final FavoriteRepository favRepo;
    private final StationsRepository netRepo;

    public SearchViewModel(@NonNull Application app) {
        super(app);
        favRepo = ServiceLocator.favorites(app);
        netRepo = new StationsRepository(app);
    }

    public LiveData<List<Station>> search(String q) {
        return netRepo.search(q);
    }

    public FavoriteRepository getRepo() { return favRepo; }
}

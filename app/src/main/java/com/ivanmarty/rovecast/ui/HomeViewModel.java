package com.ivanmarty.rovecast.ui;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.ivanmarty.rovecast.repo.StationRepository;
import com.ivanmarty.rovecast.model.Station;
import com.ivanmarty.rovecast.data.FavoriteRepository; // RUTA CORREGIDA

import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final FavoriteRepository favRepo;
    private final StationRepository stationRepo;
    private final LiveData<List<Station>> stations;

    public HomeViewModel(@NonNull Application app) {
        super(app);
        favRepo = new FavoriteRepository(app);
        stationRepo = new StationRepository(app);
        stations = stationRepo.getCachedStations();
    }

    /** Lanza la actualización de estaciones desde la red. */
    public void refreshStations() {
        stationRepo.refreshStations();
    }

    /** Devuelve las estaciones (cacheadas) para que la UI las observe. */
    public LiveData<List<Station>> getStations() { return stations; }

    public FavoriteRepository getFavRepo() { return favRepo; }
}

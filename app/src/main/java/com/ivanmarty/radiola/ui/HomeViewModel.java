package com.ivanmarty.radiola.ui;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.ivanmarty.radiola.data.FavoriteRepository;
import com.ivanmarty.radiola.data.StationsRepository;
import com.ivanmarty.radiola.model.Station;
import com.ivanmarty.radiola.util.ServiceLocator;
import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final FavoriteRepository favRepo;
    private final StationsRepository netRepo;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private LiveData<List<Station>> stations = new MutableLiveData<>();

    public HomeViewModel(@NonNull Application app) {
        super(app);
        favRepo = ServiceLocator.favorites(app);
        netRepo = new StationsRepository(app);
    }

    public void loadLocalStations() {
        loading.setValue(true);
        stations = netRepo.topNearUser(50); // LiveData de Retrofit
    }

    public LiveData<List<Station>> getStations() { return stations; }
    public FavoriteRepository getRepo() { return favRepo; }
    public LiveData<Boolean> getLoading() { return loading; }
}

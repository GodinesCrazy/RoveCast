package com.ivanmarty.radiola.repo;

import android.content.Context;
import android.telephony.TelephonyManager;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.ivanmarty.radiola.api.ApiClient;
import com.ivanmarty.radiola.api.RadioBrowserService;
import com.ivanmarty.radiola.data.AppDatabase;
import com.ivanmarty.radiola.data.FavoriteDao;
import com.ivanmarty.radiola.data.FavoriteStation;
import com.ivanmarty.radiola.model.Station;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StationRepository {
    private final RadioBrowserService api = ApiClient.get();
    private final FavoriteDao favoriteDao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Context app;

    public StationRepository(Context ctx) {
        this.app = ctx.getApplicationContext();
        this.favoriteDao = AppDatabase.get(app).favoriteDao();
    }

    public LiveData<List<FavoriteStation>> getFavorites() {
        return favoriteDao.getAll();
    }

    public LiveData<Boolean> isFavorite(String uuid) {
        return favoriteDao.isFavorite(uuid);
    }

    public void toggleFavorite(Station s, boolean makeFav) {
        io.execute(() -> {
            if (makeFav) {
                favoriteDao.insert(new FavoriteStation(
                        s.stationuuid, s.name, s.url, s.favicon));
            } else {
                favoriteDao.delete(new FavoriteStation(
                        s.stationuuid, s.name, s.url, s.favicon));
            }
        });
    }

    /** Determina ISO2 de país vía red/locale (sin permisos). */
    public String getUserCountryIso2() {
        try {
            TelephonyManager tm = (TelephonyManager) app.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                String net = tm.getNetworkCountryIso();
                if (net != null && net.length() == 2) return net.toUpperCase(Locale.US);
            }
        } catch (Exception ignored) {}
        String loc = Locale.getDefault().getCountry();
        if (loc != null && loc.length() == 2) return loc.toUpperCase(Locale.US);
        return "US"; // fallback
    }

    /** Carga estaciones por país (orden por clicks, descendente). */
    public LiveData<List<Station>> loadByCountry(String iso2) {
        MutableLiveData<List<Station>> live = new MutableLiveData<>();
        api.stationsByCountry(iso2, true, "clickcount", true, 100)
                .enqueue(new Callback<List<Station>>() {
                    @Override public void onResponse(Call<List<Station>> call, Response<List<Station>> resp) {
                        if (resp.isSuccessful()) live.postValue(resp.body());
                        else loadTopFallback(live);
                    }
                    @Override public void onFailure(Call<List<Station>> call, Throwable t) { loadTopFallback(live); }
                });
        return live;
    }

    private void loadTopFallback(MutableLiveData<List<Station>> live) {
        api.topClick(100).enqueue(new Callback<List<Station>>() {
            @Override public void onResponse(Call<List<Station>> call, Response<List<Station>> resp) {
                if (resp.isSuccessful()) live.postValue(resp.body()); else live.postValue(null);
            }
            @Override public void onFailure(Call<List<Station>> call, Throwable t) { live.postValue(null); }
        });
    }

    public LiveData<List<Station>> searchByName(String q) {
        MutableLiveData<List<Station>> live = new MutableLiveData<>();
        api.searchByName(q, true, 100)
                .enqueue(new Callback<List<Station>>() {
                    @Override public void onResponse(Call<List<Station>> call, Response<List<Station>> resp) {
                        if (resp.isSuccessful()) live.postValue(resp.body()); else live.postValue(null);
                    }
                    @Override public void onFailure(Call<List<Station>> call, Throwable t) { live.postValue(null); }
                });
        return live;
    }
}

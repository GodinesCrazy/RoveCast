package com.ivanmarty.rovecast.repo;

import android.content.Context;
import android.telephony.TelephonyManager;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.ivanmarty.rovecast.api.ApiClient;
import com.ivanmarty.rovecast.api.RadioBrowserService;
import com.ivanmarty.rovecast.data.AppDatabase;
import com.ivanmarty.rovecast.data.FavoriteDao;
import com.ivanmarty.rovecast.data.FavoriteStation;
import com.ivanmarty.rovecast.data.StationDao;
import com.ivanmarty.rovecast.model.Station;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StationRepository {
    private final RadioBrowserService api = ApiClient.get();
    private final StationDao stationDao;
    private final FavoriteDao favoriteDao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Context app;

    public StationRepository(Context ctx) {
        this.app = ctx.getApplicationContext();
        AppDatabase db = AppDatabase.get(app);
        this.stationDao = db.stationDao();
        this.favoriteDao = db.favoriteDao();
    }

    // --- FAVORITES ---

    public LiveData<List<FavoriteStation>> getFavorites() {
        return favoriteDao.getAll();
    }

    public LiveData<Boolean> isFavorite(String uuid) {
        return favoriteDao.isFavorite(uuid);
    }

    public void toggleFavorite(Station s, boolean makeFav) {
        io.execute(() -> {
            // VALIDACIÓN MEJORADA: Prevenir crash si la estación o su UUID son nulos/vacíos.
            if (s == null || s.stationuuid == null || s.stationuuid.trim().isEmpty()) {
                android.util.Log.w("StationRepository", "Intento de guardar estación inválida o con UUID nulo/vacío. Se ignora.");
                return; // No continuar con la operación de base de datos.
            }

            if (makeFav) {
                favoriteDao.insert(new FavoriteStation(s.stationuuid, s.name, s.getUrl(), s.favicon));
            } else {
                favoriteDao.delete(new FavoriteStation(s.stationuuid, s.name, s.getUrl(), s.favicon));
            }
        });
    }

    // --- STATIONS (CACHE + NETWORK) ---

    /**
     * Devuelve las estaciones cacheadas en la BD. La UI observa este LiveData.
     */
    public LiveData<List<Station>> getCachedStations() {
        return stationDao.getStations();
    }

    /**
     * Lanza una petición a la red para refrescar el contenido de la caché.
     * La UI se actualizará automáticamente gracias al LiveData de getCachedStations().
     */
    public void refreshStations() {
        String countryIso = getUserCountryIso2();
        api.stationsByCountry(countryIso, true, "clickcount", true, 100)
                .enqueue(new Callback<List<Station>>() {
                    @Override
                    public void onResponse(Call<List<Station>> call, Response<List<Station>> resp) {
                        if (resp.isSuccessful() && resp.body() != null) {
                            io.execute(() -> {
                                List<Station> validStations = new java.util.ArrayList<>();
                                for (Station s : resp.body()) {
                                    if (s != null && s.stationuuid != null && !s.stationuuid.trim().isEmpty()) {
                                        if ((s.url_resolved == null || s.url_resolved.trim().isEmpty()) && s.url != null) {
                                            s.url_resolved = s.url;
                                        }
                                        validStations.add(s);
                                    }
                                }
                                if (validStations.isEmpty()) {
                                    loadTopFallback();
                                } else {
                                    // SOLUCIÓN DEFINITIVA: Solo borrar si la nueva lista es sustancial.
                                    // Esto previene que una respuesta mala de la API borre la caché.
                                    if (validStations.size() > 20) {
                                        stationDao.deleteAll();
                                        stationDao.insertAll(validStations);
                                    }
                                }
                            });
                        } else {
                            loadTopFallback(); // Intenta con el fallback si la llamada por país falla
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Station>> call, Throwable t) {
                        loadTopFallback(); // Intenta con el fallback en caso de error de red
                    }
                });
    }

    private void loadTopFallback() {
        io.execute(() -> {
            if (stationDao.getStationCount() > 0) {
                return; // No-op si ya hay estaciones en la caché
            }

            api.topClick(100).enqueue(new Callback<List<Station>>() {
                @Override
                public void onResponse(Call<List<Station>> call, Response<List<Station>> resp) {
                    if (resp.isSuccessful() && resp.body() != null) {
                        io.execute(() -> {
                            List<Station> validStations = new java.util.ArrayList<>();
                            for (Station s : resp.body()) {
                                if (s.stationuuid != null && !s.stationuuid.trim().isEmpty()) {
                                    validStations.add(s);
                                }
                            }
                            // No es necesario borrar, la caché ya está vacía en este punto.
                            stationDao.insertAll(validStations);
                        });
                    }
                }
                @Override
                public void onFailure(Call<List<Station>> call, Throwable t) { /* No-op en el fallback final */ }
            });
        });
    }

    /** La búsqueda no usa caché, es una operación directa que devuelve un LiveData temporal. */
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

    private String getUserCountryIso2() {
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
}

package com.ivanmarty.radiola.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ivanmarty.radiola.api.RadioBrowserApi;
import com.ivanmarty.radiola.api.RestClient;
import com.ivanmarty.radiola.model.Station;
import com.ivanmarty.radiola.util.GeoUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Capa de datos para estaciones desde Radio Browser API. */
public class StationsRepository {

    private final Context app;
    private final RadioBrowserApi api;

    public StationsRepository(Context app) {
        this.app = app.getApplicationContext();
        this.api = RestClient.get();
    }

    /** Top de estaciones para el país del usuario (por ISO2), fallback a lista vacía. */
    public LiveData<List<Station>> topNearUser(int limit) {
        MutableLiveData<List<Station>> ld = new MutableLiveData<>();
        String country = GeoUtils.countryCode(app);
        if (country == null || country.isEmpty()) {
            ld.postValue(new ArrayList<>());
            return ld;
        }
        api.topByCountry(country, true, "clickcount", true, limit)
                .enqueue(new Callback<List<Station>>() {
                    @Override public void onResponse(@NonNull Call<List<Station>> call, @NonNull Response<List<Station>> res) {
                        if (res.isSuccessful() && res.body() != null) ld.postValue(res.body());
                        else ld.postValue(new ArrayList<>());
                    }
                    @Override public void onFailure(@NonNull Call<List<Station>> call, @NonNull Throwable t) {
                        Log.w("StationsRepository", "topNearUser failed", t);
                        ld.postValue(new ArrayList<>());
                    }
                });
        return ld;
    }

    /** Búsqueda por nombre (orden por relevancia) */
    public LiveData<List<Station>> search(String query) {
        MutableLiveData<List<Station>> ld = new MutableLiveData<>();
        Log.d("SearchRepo", "Searching for: " + query);
        api.searchByName(query, true, "relevance", true, 100)
                .enqueue(new Callback<List<Station>>() {
                    @Override public void onResponse(@NonNull Call<List<Station>> call, @NonNull Response<List<Station>> res) {
                        if (res.isSuccessful() && res.body() != null) {
                            Log.d("SearchRepo", "Found " + res.body().size() + " stations for query: " + query);
                            ld.postValue(res.body());
                        } else {
                            Log.w("SearchRepo", "Search failed for query: " + query + ", response code: " + res.code());
                            ld.postValue(new ArrayList<>());
                        }
                    }
                    @Override public void onFailure(@NonNull Call<List<Station>> call, @NonNull Throwable t) {
                        Log.e("SearchRepo", "Search failed for query: " + query, t);
                        ld.postValue(new ArrayList<>());
                    }
                });
        return ld;
    }
}

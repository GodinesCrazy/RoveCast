package com.ivanmarty.radiola.api;

import com.ivanmarty.radiola.model.Station;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/** Endpoints de Radio Browser vía /json/stations/search */
public interface RadioBrowserApi {

    // Top por país (orden por clickcount, evita rotos)
    @GET("json/stations/search")
    Call<List<Station>> topByCountry(
            @Query("countrycode") String countryCode,  // CL, AR, ES, etc.
            @Query("hidebroken") boolean hideBroken,   // true
            @Query("order") String order,              // "clickcount"
            @Query("reverse") boolean reverse,         // true
            @Query("limit") int limit                  // p.ej. 100
    );

    // Búsqueda por nombre (global)
    @GET("json/stations/search")
    Call<List<Station>> searchByName(
            @Query("name") String name,
            @Query("hidebroken") boolean hideBroken,
            @Query("order") String order,              // "relevance" o "clickcount"
            @Query("reverse") boolean reverse,
            @Query("limit") int limit
    );
}

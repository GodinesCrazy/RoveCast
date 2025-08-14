package com.ivanmarty.radiola.api;

import com.ivanmarty.radiola.model.Station;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Endpoints alternativos (bycountrycodeexact + topclick) */
public interface RadioBrowserService {

    @GET("json/stations/bycountrycodeexact/{code}")
    Call<List<Station>> stationsByCountry(
            @Path("code") String iso2,
            @Query("hidebroken") boolean hideBroken,
            @Query("order") String order,      // "clickcount"
            @Query("reverse") boolean reverse, // true
            @Query("limit") int limit
    );

    @GET("json/stations/search")
    Call<List<Station>> searchByName(
            @Query("name") String name,
            @Query("hidebroken") boolean hideBroken,
            @Query("limit") int limit
    );

    @GET("json/stations/topclick")
    Call<List<Station>> topClick(@Query("limit") int limit);
}

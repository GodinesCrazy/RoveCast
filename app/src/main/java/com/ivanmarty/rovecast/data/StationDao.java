package com.ivanmarty.rovecast.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.ivanmarty.rovecast.model.Station;

import java.util.List;

@Dao
public interface StationDao {

    @Query("SELECT * FROM stations ORDER BY RANDOM()") // Orden aleatorio para que la lista no sea siempre igual
    LiveData<List<Station>> getStations();

    @Query("SELECT COUNT(*) FROM stations")
    int getStationCount();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Station> stations);

    @Query("DELETE FROM stations")
    void deleteAll();
}

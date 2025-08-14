package com.ivanmarty.radiola.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY name COLLATE NOCASE ASC")
    LiveData<List<FavoriteStation>> getAll();

    // Para StationRepository: necesita LiveData<Boolean>
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE stationuuid = :uuid)")
    LiveData<Boolean> isFavorite(String uuid);

    // Para FavoriteRepository: "upsert"
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(FavoriteStation s);

    // Para StationRepository: "insert" explícito
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FavoriteStation s);

    // Para StationRepository: "delete" con entidad
    @Delete
    void delete(FavoriteStation s);

    // Para FavoriteRepository: "deleteById"
    @Query("DELETE FROM favorites WHERE stationuuid = :uuid")
    void deleteById(String uuid);
}

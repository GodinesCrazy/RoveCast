package com.ivanmarty.rovecast.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorites")
public class FavoriteStation {

    @PrimaryKey @NonNull
    public String stationuuid;

    public String name;
    public String url;
    public String favicon;
    public String country; // opcional, pero útil para mostrar meta

    // Constructor que Room usará (sin args)
    public FavoriteStation() { }

    // Constructor de conveniencia para tu StationRepository (4 args)
    // Marcado con @Ignore para que Room NO lo considere.
    @Ignore
    public FavoriteStation(@NonNull String stationuuid, String name, String url, String favicon) {
        this.stationuuid = stationuuid;
        this.name = name;
        this.url = url;
        this.favicon = favicon;
    }
}

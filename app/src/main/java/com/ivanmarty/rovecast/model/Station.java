package com.ivanmarty.rovecast.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;

@Entity(tableName = "stations")
public class Station {
    @PrimaryKey
    @NonNull
    @SerializedName("stationuuid") public String stationuuid;

    @SerializedName("name")        public String name;
    @SerializedName("url_resolved") public String url_resolved;
    @SerializedName("favicon")     public String favicon;
    @SerializedName("country")     public String country;
    @SerializedName("language")    public String language;
    @SerializedName("bitrate")     public int bitrate;
    @SerializedName("codec")       public String codec;
    @SerializedName("tags")        public String tags;

    // El campo 'url' de la API a veces no es el bueno, 'url_resolved' sí.
    // Para mantener compatibilidad con el código existente que usa 's.url', lo asignamos aquí.
    public String getUrl() {
        return url_resolved;
    }
}


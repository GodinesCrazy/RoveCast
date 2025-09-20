package com.ivanmarty.rovecast.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;
import java.util.Objects;

@Entity(tableName = "stations")
public class Station {
    @PrimaryKey
    @NonNull
    @SerializedName("stationuuid") public String stationuuid;

    @SerializedName("name")        public String name;
    @SerializedName("url_resolved") public String url_resolved;
    @SerializedName("url")         public String url; // fallback si url_resolved viene vacío
    @SerializedName("favicon")     public String favicon;
    @SerializedName("country")     public String country;
    @SerializedName("language")    public String language;
    @SerializedName("bitrate")     public int bitrate;
    @SerializedName("codec")       public String codec;
    @SerializedName("tags")        public String tags;

    // Campo ignorado por Room para gestionar el estado de favorito en la UI.
    @Ignore
    public boolean isFavorite = false;

    // El campo 'url' de la API a veces no es el bueno, 'url_resolved' sí.
    // Para mantener compatibilidad con el código existente que usa 's.url', lo asignamos aquí.
    public String getUrl() { return (url_resolved != null && !url_resolved.isEmpty()) ? url_resolved : url; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Station station = (Station) o;
        return isFavorite == station.isFavorite &&
                bitrate == station.bitrate &&
                stationuuid.equals(station.stationuuid) &&
                Objects.equals(name, station.name) &&
                Objects.equals(url_resolved, station.url_resolved) &&
                Objects.equals(favicon, station.favicon) &&
                Objects.equals(country, station.country) &&
                Objects.equals(language, station.language) &&
                Objects.equals(codec, station.codec) &&
                Objects.equals(tags, station.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stationuuid, name, url_resolved, favicon, country, language, bitrate, codec, tags, isFavorite);
    }
}

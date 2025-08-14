package com.ivanmarty.radiola.model;

import com.google.gson.annotations.SerializedName;

public class Station {
    @SerializedName("stationuuid") public String stationuuid;
    @SerializedName("name")        public String name;
    @SerializedName("url")         public String url;
    @SerializedName("favicon")     public String favicon;
    @SerializedName("country")     public String country;
    @SerializedName("language")    public String language;  // a veces "es;en"
    @SerializedName("bitrate")     public int bitrate;
    @SerializedName("codec")       public String codec;
    @SerializedName("tags")        public String tags;
}

package com.ivanmarty.radiola.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RestClient {
    private static final String BASE_URL = "https://de1.api.radio-browser.info/";
    private static RadioBrowserApi rb;

    public static RadioBrowserApi get() {
        if (rb == null) {
            OkHttpClient ok = new OkHttpClient.Builder().build();
            Gson gson = new GsonBuilder().create();
            Retrofit r = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(ok)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
            rb = r.create(RadioBrowserApi.class);
        }
        return rb;
    }

    private RestClient() {}
}

package com.ivanmarty.radiola.api;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {
    private static final String BASE = "https://de1.api.radio-browser.info/";
    private static RadioBrowserService service;

    public static RadioBrowserService get() {
        if (service == null) {
            OkHttpClient client = new OkHttpClient.Builder().build();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
            service = retrofit.create(RadioBrowserService.class);
        }
        return service;
    }

    private ApiClient() {}
}

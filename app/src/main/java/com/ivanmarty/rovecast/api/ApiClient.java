package com.ivanmarty.rovecast.api;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {
    // Usar el balanceador "all" para alta disponibilidad entre mirrors.
    private static final String BASE = "https://all.api.radio-browser.info/";
    private static RadioBrowserService service;

    public static RadioBrowserService get() {
        if (service == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .retryOnConnectionFailure(true)
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .callTimeout(30, TimeUnit.SECONDS)
                    .connectionSpecs(java.util.Arrays.asList(
                            okhttp3.ConnectionSpec.MODERN_TLS,
                            okhttp3.ConnectionSpec.COMPATIBLE_TLS
                    ))
                    .addInterceptor(chain -> {
                        okhttp3.Request original = chain.request();
                        okhttp3.Request req = original.newBuilder()
                                .header("User-Agent", "RoveCast/1.0 (Android)")
                                .header("Accept", "application/json")
                                .build();
                        return chain.proceed(req);
                    })
                    .build();
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

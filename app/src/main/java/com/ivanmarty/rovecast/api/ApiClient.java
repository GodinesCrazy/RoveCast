package com.ivanmarty.rovecast.api;

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
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .readTimeout(java.time.Duration.ofSeconds(20))
                    .callTimeout(java.time.Duration.ofSeconds(30))
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

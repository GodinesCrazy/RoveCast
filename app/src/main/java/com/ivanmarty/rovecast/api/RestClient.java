package com.ivanmarty.rovecast.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RestClient {
    private static final String BASE_URL = "https://all.api.radio-browser.info/";
    private static RadioBrowserApi rb;

    public static RadioBrowserApi get() {
        if (rb == null) {
            OkHttpClient ok = new OkHttpClient.Builder()
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

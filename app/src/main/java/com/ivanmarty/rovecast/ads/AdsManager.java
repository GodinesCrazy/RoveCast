package com.ivanmarty.rovecast.ads;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.ivanmarty.rovecast.billing.PremiumManager;


public final class AdsManager {

    private static final String TAG = "AdsManager";
    private static volatile AdsManager INSTANCE;

    @Nullable
    public static AdsManager get() { return INSTANCE; }

    public static void init(@NonNull Context ctx) {
        if (INSTANCE == null) {
            synchronized (AdsManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AdsManager(ctx.getApplicationContext());
                }
            }
        }
    }

    private final Context app;
    private InterstitialAd interstitial;
    private long lastShownMs = 0L;

    // --- Configuración --- //
    private static final String AD_ID_INTERSTITIAL = "ca-app-pub-2918417880001381/8618426192";
    private static final long MIN_INTERVAL_MS = 300_000L; // 5 minutos
    private static final int MAX_PER_DAY = 8;

    private AdsManager(@NonNull Context app) {
        this.app = app;
        preload();
    }

    private void preload() {
        if (PremiumManager.isPremium(app)) {
            Log.d(TAG, "Usuario Premium, no se precargan anuncios.");
            interstitial = null;
            return;
        }
        Log.d(TAG, "Precargando anuncio intersticial...");
        AdRequest req = new AdRequest.Builder().build();
        InterstitialAd.load(app, AD_ID_INTERSTITIAL, req,
                new InterstitialAdLoadCallback() {
                    @Override public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitial = ad;
                        Log.d(TAG, "Anuncio intersticial cargado con éxito.");
                    }
                    @Override public void onAdFailedToLoad(@NonNull com.google.android.gms.ads.LoadAdError loadAdError) {
                        interstitial = null;
                        Log.e(TAG, "Error al cargar el anuncio intersticial: " + loadAdError.getMessage());
                    }
                });
    }

    public void runWithMaybeAd(@NonNull Activity act, @Nullable Runnable onAdClosed) {
        if (PremiumManager.isPremium(app)) {
            Log.d(TAG, "No se muestra anuncio: Usuario Premium.");
            if (onAdClosed != null) onAdClosed.run();
            return;
        }
        long now = System.currentTimeMillis();
        if (interstitial == null) {
            Log.d(TAG, "No se muestra anuncio: No hay un anuncio cargado.");
            if (onAdClosed != null) onAdClosed.run();
            preload(); // Intentar cargar el siguiente
            return;
        }
        if ((now - lastShownMs) < MIN_INTERVAL_MS) {
            Log.d(TAG, "No se muestra anuncio: No ha pasado el tiempo mínimo de espera.");
            if (onAdClosed != null) onAdClosed.run();
            return;
        }
        if (getShownToday() >= MAX_PER_DAY) {
            Log.d(TAG, "No se muestra anuncio: Se ha alcanzado el límite diario.");
            if (onAdClosed != null) onAdClosed.run();
            return;
        }

        Log.d(TAG, "Mostrando anuncio intersticial.");
        interstitial.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override public void onAdDismissedFullScreenContent() {
                setShownToday(getShownToday() + 1);
                lastShownMs = System.currentTimeMillis();
                interstitial = null;
                preload();
                if (onAdClosed != null) onAdClosed.run();
                Log.d(TAG, "Anuncio cerrado.");
            }
            @Override public void onAdFailedToShowFullScreenContent(AdError adError) {
                if (onAdClosed != null) onAdClosed.run();
                interstitial = null;
                preload();
                Log.e(TAG, "Error al mostrar el anuncio: " + adError.getMessage());
            }
        });
        interstitial.show(act);
    }

    private int getShownToday() {
        final SharedPreferences sp = app.getSharedPreferences("ads_prefs", Context.MODE_PRIVATE);
        final long day = sp.getLong("day", 0L);
        final long today = System.currentTimeMillis() / (24L*60*60*1000);
        if (day != today) {
            sp.edit().putLong("day", today).putInt("count", 0).apply();
            return 0;
        }
        return sp.getInt("count", 0);
    }

    private void setShownToday(int c) {
        final SharedPreferences sp = app.getSharedPreferences("ads_prefs", Context.MODE_PRIVATE);
        final long today = System.currentTimeMillis() / (24L*60*60*1000);
        sp.edit().putLong("day", today).putInt("count", c).apply();
    }
}
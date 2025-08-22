package com.ivanmarty.rovecast.ads;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.ivanmarty.rovecast.billing.PremiumManager;

public final class AdsManager {

    private static AdsManager INSTANCE;

    public static AdsManager get() { return INSTANCE; }
    public static void init(Context ctx) { if (INSTANCE == null) INSTANCE = new AdsManager(ctx.getApplicationContext()); }

    private final Context app;
    private InterstitialAd interstitial;
    private long lastShownMs = 0L;

    // --- Configuración --- //
    private static final String AD_ID_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712";
    private long minIntervalMs = 300_000L; // 5 minutos
    private int maxPerDay = 8;

    private AdsManager(Context app) {
        this.app = app;
        preload();
    }

    private void preload() {
        if (PremiumManager.isPremium(app)) { interstitial = null; return; }
        AdRequest req = new AdRequest.Builder().build();
        InterstitialAd.load(app, AD_ID_INTERSTITIAL, req,
                new InterstitialAdLoadCallback() {
                    @Override public void onAdLoaded(@NonNull InterstitialAd ad) { interstitial = ad; }
                    @Override public void onAdFailedToLoad(@NonNull com.google.android.gms.ads.LoadAdError loadAdError) { interstitial = null; }
                });
    }

    public void runWithMaybeAd(Activity act, Runnable onAdClosed) {
        if (PremiumManager.isPremium(app)) {
            if (onAdClosed != null) onAdClosed.run();
            return;
        }
        long now = System.currentTimeMillis();
        if (interstitial == null || (now - lastShownMs) < minIntervalMs || getShownToday() >= maxPerDay) {
            if (onAdClosed != null) onAdClosed.run();
            if (interstitial == null) preload();
            return;
        }

        interstitial.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override public void onAdDismissedFullScreenContent() {
                setShownToday(getShownToday() + 1);
                lastShownMs = System.currentTimeMillis();
                interstitial = null;
                preload();
                if (onAdClosed != null) onAdClosed.run();
            }
            @Override public void onAdFailedToShowFullScreenContent(AdError adError) {
                if (onAdClosed != null) onAdClosed.run();
                interstitial = null;
                preload();
            }
        });
        interstitial.show(act);
    }

    private int getShownToday() {
        SharedPreferences sp = app.getSharedPreferences("ads_prefs", Context.MODE_PRIVATE);
        long day = sp.getLong("day", 0L);
        long today = System.currentTimeMillis() / (24L*60*60*1000);
        if (day != today) { sp.edit().putLong("day", today).putInt("count", 0).apply(); return 0; }
        return sp.getInt("count", 0);
    }

    private void setShownToday(int c) {
        SharedPreferences sp = app.getSharedPreferences("ads_prefs", Context.MODE_PRIVATE);
        long today = System.currentTimeMillis() / (24L*60*60*1000);
        sp.edit().putLong("day", today).putInt("count", c).apply();
    }
}
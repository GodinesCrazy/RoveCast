package com.ivanmarty.radiola.ads;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.ivanmarty.radiola.billing.PremiumManager;

public final class AdsManager {

    private static AdsManager INSTANCE;

    public static AdsManager get() { return INSTANCE; }
    public static void init(Context ctx) { if (INSTANCE == null) INSTANCE = new AdsManager(ctx.getApplicationContext()); }

    private final Context app;
    private InterstitialAd interstitial;
    private long lastShownMs = 0L;
    private int changesSinceLast = 0;
    private boolean firstPlayDone = false;

    // --- Configuración --- //
    private static final String AD_ID_INTERSTITIAL = "ca-app-pub-2918417880001381/3218147476";
    private long cooldownMs = 90_000L; // 1.5 minutos
    private long minIntervalMs = 90_000L; // 1.5 minutos
    private int minChangesBeforeAd = 2;
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

    public void markFirstPlayDone() { firstPlayDone = true; }

    public void onStationChangeTrigger() { changesSinceLast++; }

    public void runWithMaybeAd(Activity act, Runnable action) {
        if (PremiumManager.isPremium(app)) { action.run(); return; }
        if (!firstPlayDone) {
            markFirstPlayDone();
            action.run();
            return;
        }
        long now = System.currentTimeMillis();
        if (interstitial == null ||
                changesSinceLast < minChangesBeforeAd ||
                (now - lastShownMs) < minIntervalMs ||
                (now - lastShownMs) < cooldownMs ||
                getShownToday() >= maxPerDay) {
            action.run();
            if (interstitial == null) preload();
            return;
        }

        interstitial.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override public void onAdDismissedFullScreenContent() {
                setShownToday(getShownToday() + 1);
                lastShownMs = System.currentTimeMillis();
                changesSinceLast = 0;
                interstitial = null;
                preload();
                action.run();
            }
            @Override public void onAdFailedToShowFullScreenContent(AdError adError) {
                action.run();
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

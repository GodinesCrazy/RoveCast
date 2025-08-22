package com.ivanmarty.rovecast.billing;

import android.content.Context;
import android.content.SharedPreferences;

/** Flag local para Premium. (Persiste y se consulta desde Ads/Fragments) */
public final class PremiumManager {

    private static final String PREF = "premium_prefs";
    private static final String KEY = "is_premium";

    public static boolean isPremium(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(KEY, false);
    }

    public static void setPremium(Context ctx, boolean value) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean(KEY, value).apply();
    }
}

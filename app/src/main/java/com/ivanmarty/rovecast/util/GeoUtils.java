package com.ivanmarty.rovecast.util;

import android.content.Context;
import android.telephony.TelephonyManager;

import java.util.Locale;

/** Utilidad simple para país ISO del usuario. */
public final class GeoUtils {

    public static String countryCode(Context ctx) {
        try {
            TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                String net = tm.getNetworkCountryIso();
                if (net != null && !net.trim().isEmpty()) return net.toUpperCase(Locale.US);
            }
        } catch (SecurityException ignored) {}
        String local = Locale.getDefault().getCountry();
        return (local == null || local.trim().isEmpty()) ? "US" : local.toUpperCase(Locale.US);
    }
}

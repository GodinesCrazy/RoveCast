package com.ivanmarty.rovecast.util;

import android.content.Context;
import android.content.SharedPreferences;

public class FirstLaunchManager {

    private static final String PREFS_NAME = "RoveCastPrefs";
    private static final String IS_FIRST_LAUNCH = "IsFirstLaunch";

    private final SharedPreferences preferences;

    public FirstLaunchManager(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isFirstLaunch() {
        return preferences.getBoolean(IS_FIRST_LAUNCH, true);
    }

    public void setFirstLaunch(boolean isFirst) {
        preferences.edit().putBoolean(IS_FIRST_LAUNCH, isFirst).apply();
    }
}

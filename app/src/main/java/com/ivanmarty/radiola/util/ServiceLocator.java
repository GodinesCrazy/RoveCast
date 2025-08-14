package com.ivanmarty.radiola.util;

import android.content.Context;

import com.ivanmarty.radiola.data.FavoriteRepository;

public final class ServiceLocator {

    private static FavoriteRepository fav;

    public static FavoriteRepository favorites(Context ctx) {
        if (fav == null) fav = new FavoriteRepository(ctx.getApplicationContext());
        return fav;
    }

    private ServiceLocator() {}
}

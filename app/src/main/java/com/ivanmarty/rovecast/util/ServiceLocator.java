package com.ivanmarty.rovecast.util;

import android.content.Context;

import com.ivanmarty.rovecast.data.FavoriteRepository;

public final class ServiceLocator {

    private static FavoriteRepository fav;

    public static FavoriteRepository favorites(Context ctx) {
        if (fav == null) fav = new FavoriteRepository(ctx.getApplicationContext());
        return fav;
    }

    private ServiceLocator() {}
}

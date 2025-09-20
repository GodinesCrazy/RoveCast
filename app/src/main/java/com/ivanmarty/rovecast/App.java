package com.ivanmarty.rovecast;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import com.ivanmarty.rovecast.ads.AdsManager;
import com.ivanmarty.rovecast.cast.CastManager;

import java.util.List;

/**
 * App.java - Clase Application del proyecto RoveCast.
 * - Inicializa AdMob.
 * - Inicializa el AdsManager (interstitial con capping/cooldown).
 * - Configura las opciones de Google Cast.
 */
public class App extends Application implements OptionsProvider {

    @Override
    public void onCreate() {
        super.onCreate();

        // Inicializa AdMob (IDs de prueba hasta publicación)
        MobileAds.initialize(this, initializationStatus -> { /* no-op */ });

        

        // Inicializa los managers de la app que requieren contexto.
        // Al hacerlo aquí, nos aseguramos de que usan el contexto de la aplicación
        // y evitamos fugas de memoria.
        AdsManager.init(this);
        CastManager.init(this);

        // (Opcional futuro): inicializar UMP (consentimiento) y analytics/crashlytics.
    }

    @NonNull
    @Override
    public CastOptions getCastOptions(@NonNull Context context) {
        return new CastOptions.Builder()
                .setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
                .build();
    }

    @Override
    public List<SessionProvider> getAdditionalSessionProviders(@NonNull Context context) {
        return null;
    }
}

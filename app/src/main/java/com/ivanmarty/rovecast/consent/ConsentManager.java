package com.ivanmarty.rovecast.consent;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.MobileAds;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Gestor de consentimiento (GDPR/CCPA) con UMP. Centraliza toda la lógica de consentimiento.
 * Es un Singleton para asegurar una única instancia en toda la app.
 */
public final class ConsentManager {

    private static final String TAG = "ConsentManager";
    private static volatile ConsentManager INSTANCE;

    private final ConsentInformation consentInformation;
    private final AtomicBoolean isMobileAdsInitialized = new AtomicBoolean(false);

    // Callback para notificar a la UI cuando el proceso de consentimiento termina.
    public interface OnConsentCompleteListener {
        void onConsentComplete();
    }

    private ConsentManager(Context context) {
        this.consentInformation = UserMessagingPlatform.getConsentInformation(context);
    }

    public static ConsentManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ConsentManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ConsentManager(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Inicia el proceso de consentimiento. Llama a este método desde la MainActivity.
     * @param activity La actividad que presenta el formulario.
     * @param listener El callback que se ejecuta cuando el proceso termina.
     */
    public void gatherConsent(Activity activity, OnConsentCompleteListener listener) {
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();

        consentInformation.requestConsentInfoUpdate(activity, params, () -> {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, formError -> {
                if (formError != null) {
                    Log.w(TAG, String.format("%s: %s", formError.getErrorCode(), formError.getMessage()));
                }
                // El consentimiento se ha obtenido o no era necesario. Inicializar anuncios.
                initializeMobileAdsIfNeeded(activity);
                listener.onConsentComplete();
            });
        }, requestConsentError -> {
            Log.w(TAG, String.format("%s: %s", requestConsentError.getErrorCode(), requestConsentError.getMessage()));
            // Aunque haya un error, intentamos inicializar por si acaso.
            initializeMobileAdsIfNeeded(activity);
            listener.onConsentComplete();
        });
    }

    /**
     * Muestra el formulario de privacidad para que el usuario pueda cambiar su consentimiento.
     * @param activity La actividad que presenta el formulario.
     */
    public void showPrivacyOptionsForm(Activity activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, formError -> {
            if (formError != null) {
                Log.w(TAG, String.format("%s: %s", formError.getErrorCode(), formError.getMessage()));
            }
        });
    }

    /**
     * Inicializa el SDK de Mobile Ads si el consentimiento ha sido otorgado y aún no ha sido inicializado.
     */
    private void initializeMobileAdsIfNeeded(Context context) {
        if (canRequestAds() && isMobileAdsInitialized.compareAndSet(false, true)) {
            MobileAds.initialize(context);
        }
    }

    /**
     * Verifica si se pueden solicitar anuncios.
     * @return true si se pueden solicitar anuncios, false en caso contrario.
     */
    public boolean canRequestAds() {
        return consentInformation.canRequestAds();
    }
}
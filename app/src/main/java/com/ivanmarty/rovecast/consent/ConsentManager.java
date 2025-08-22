package com.ivanmarty.rovecast.consent;

import android.app.Activity;
import android.app.Application;

import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

/** Gestor de consentimiento (GDPR/CCPA) con UMP. */
public final class ConsentManager {

    private static ConsentInformation consentInfo;

    /** Opcional: llamarlo en Application.onCreate() */
    public static void init(Application app) {
        consentInfo = UserMessagingPlatform.getConsentInformation(app);
    }

    /** Llamar al abrir MainActivity para mostrar formulario si corresponde. */
    public static void requestIfNeeded(Activity activity) {
        if (consentInfo == null) {
            consentInfo = UserMessagingPlatform.getConsentInformation(activity.getApplication());
        }

        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();

        consentInfo.requestConsentInfoUpdate(
                activity,
                params,
                () -> {
                    if (consentInfo.isConsentFormAvailable()) {
                        UserMessagingPlatform.loadConsentForm(
                                activity,
                                form -> maybeShowForm(activity, form),
                                formError -> { /* opcional: log */ }
                        );
                    }
                },
                requestError -> { /* opcional: log */ }
        );
    }

    private static void maybeShowForm(Activity activity, ConsentForm form) {
        if (consentInfo.getConsentStatus() == ConsentInformation.ConsentStatus.REQUIRED) {
            form.show(activity, dismissError -> { /* opcional: log */ });
        }
    }

    private ConsentManager() {}
}

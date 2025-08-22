package com.ivanmarty.rovecast.billing;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Compra única no consumible: premium_no_ads */
public class BillingManager implements PurchasesUpdatedListener {

    public interface Listener { void onPremiumActivated(); void onError(String msg); }

    private final Activity activity;
    private final Listener listener;
    private BillingClient client;
    private final AtomicReference<ProductDetails> premiumDetails = new AtomicReference<>(null);

    public static final String PRODUCT_ID = "premium_no_ads";

    public BillingManager(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
        client = BillingClient.newBuilder(activity)
                .enablePendingPurchases()
                .setListener(this)
                .build();
    }

    public void start() {
        client.startConnection(new BillingClientStateListener() {
            @Override public void onBillingServiceDisconnected() { /* retry next time */ }
            @Override public void onBillingSetupFinished(@NonNull BillingResult br) {
                if (br.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryProduct();
                    // Revisa compras previas (restaurar)
                    client.queryPurchasesAsync(BillingClient.ProductType.INAPP, (result, list) -> {
                        if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                            for (Purchase p : list) handlePurchase(p);
                        }
                    });
                } else {
                    if (listener != null) listener.onError("Billing setup: " + br.getDebugMessage());
                }
            }
        });
    }

    private void queryProduct() {
        List<QueryProductDetailsParams.Product> products = new ArrayList<>();
        products.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build());

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(products).build();

        client.queryProductDetailsAsync(params, (br, list) -> {
            if (br.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null && !list.isEmpty()) {
                premiumDetails.set(list.get(0));
            } else if (listener != null) {
                listener.onError("Producto no encontrado en Play Console: " + PRODUCT_ID);
            }
        });
    }

    public void launchPurchase() {
        ProductDetails pd = premiumDetails.get();
        if (pd == null) { if (listener != null) listener.onError("Producto no cargado"); return; }

        BillingFlowParams.ProductDetailsParams pdp = BillingFlowParams.ProductDetailsParams
                .newBuilder().setProductDetails(pd).build();

        BillingFlowParams flow = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(List.of(pdp))
                .build();

        client.launchBillingFlow(activity, flow);
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult br, List<Purchase> purchases) {
        if (br.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase p : purchases) handlePurchase(p);
        } else if (br.getResponseCode() != BillingClient.BillingResponseCode.USER_CANCELED) {
            if (listener != null) listener.onError("Compra fallida: " + br.getDebugMessage());
        }
    }

    private void handlePurchase(Purchase p) {
        if (p.getProducts().contains(PRODUCT_ID)) {
            // Reconocimiento (acknowledge) si hace falta:
            if (!p.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(p.getPurchaseToken())
                        .build();
                client.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {});
            }
            PremiumManager.setPremium(activity, true);
            if (listener != null) listener.onPremiumActivated();
        }
    }
}

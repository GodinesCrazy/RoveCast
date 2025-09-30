package com.ivanmarty.rovecast.ui;

import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.ivanmarty.rovecast.R;
import com.ivanmarty.rovecast.consent.ConsentManager;

public class PrivacyPolicyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        // Configurar la barra de herramientas
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.privacy_policy_title);
        }

        // Configurar WebView
        WebView webView = findViewById(R.id.privacy_policy_webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("file:///android_asset/privacy_policy_en.html");

        // Configurar botón para gestionar consentimiento
        Button btnManageConsent = findViewById(R.id.btn_manage_consent);
        btnManageConsent.setOnClickListener(v -> {
            ConsentManager.getInstance(this).showPrivacyOptionsForm(this);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
package com.ivanmarty.radiola.ui;

import android.os.Bundle;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ivanmarty.radiola.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class PrivacyPolicyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        WebView webView = findViewById(R.id.privacy_policy_webview);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("privacy_policy_en.html")));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            webView.loadData(sb.toString(), "text/html", "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
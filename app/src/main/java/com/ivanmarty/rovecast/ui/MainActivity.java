package com.ivanmarty.rovecast.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ivanmarty.rovecast.R;
import com.ivanmarty.rovecast.billing.BillingManager;
import com.ivanmarty.rovecast.billing.PremiumManager;
import com.ivanmarty.rovecast.consent.ConsentManager;
import com.ivanmarty.rovecast.ui.alarm.AlarmFragment;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;

public class MainActivity extends AppCompatActivity {

    private BillingManager billing;
    private MaterialToolbar top;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        ConsentManager.requestIfNeeded(this);

        // El contexto de Cast se inicializa en la clase App
        // Añadimos un listener para depurar el estado
        CastContext.getSharedInstance(this).addCastStateListener(state -> {
            Log.d("CastState", "State changed: " + state);
        });

        top = findViewById(R.id.topBar);
        setSupportActionBar(top);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        top.setNavigationIcon(R.drawable.ic_toolbar_logo_padded);

        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.setOnItemSelectedListener(item -> {
            Fragment f;
            int id = item.getItemId();
            if (id == R.id.menu_home) f = new HomeFragment();
            else if (id == R.id.menu_fav) f = new FavoritesFragment();
            else f = new SearchFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, f)
                    .commit();
            return true;
        });
        nav.setSelectedItemId(R.id.menu_home);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_top, menu);
        
        // Oculta/muestra el botón premium según el estado
        menu.findItem(R.id.menu_premium).setVisible(!PremiumManager.isPremium(this));

        // Asigna el botón de Cast al menú
        

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_premium) {
            if (billing == null) {
                billing = new BillingManager(this, new BillingManager.Listener() {
                    @Override public void onPremiumActivated() {
                        Toast.makeText(MainActivity.this, "Premium activado", Toast.LENGTH_SHORT).show();
                        invalidateOptionsMenu(); // Vuelve a dibujar el menú para ocultar el botón
                    }
                    @Override public void onError(String msg) {
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                    }
                });
                billing.start();
            }
            // pequeño delay para asegurar queryProduct() antes de lanzar
            top.postDelayed(() -> billing.launchPurchase(), 400);
            return true;
        } else if (itemId == R.id.menu_alarm) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new AlarmFragment())
                    .addToBackStack(null) // Permite volver al fragment anterior
                    .commit();
            return true;
        } else if (itemId == R.id.menu_privacy) {
            Intent intent = new Intent(this, PrivacyPolicyActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

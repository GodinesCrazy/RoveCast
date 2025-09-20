package com.ivanmarty.rovecast.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.bumptech.glide.Glide;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.ivanmarty.rovecast.R;
import com.ivanmarty.rovecast.billing.BillingManager;
import com.ivanmarty.rovecast.billing.PremiumManager;
import com.ivanmarty.rovecast.consent.ConsentManager;
import com.ivanmarty.rovecast.player.PlaybackService;
import com.ivanmarty.rovecast.ui.alarm.AlarmFragment;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements Player.Listener {

    private BillingManager billing;
    private MaterialToolbar top;

    private ConstraintLayout miniPlayerContainer;
    private ImageView miniPlayerLogo;
    private TextView miniPlayerTitle;
    private TextView miniPlayerStatus;
    private ImageButton miniPlayerPlayPause;

    private ListenableFuture<MediaController> mediaControllerFuture;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        // SOLUCIÓN: Habilita el modo edge-to-edge para que la app se dibuje detrás de las barras de sistema.
        // Esto es necesario porque el tema usa una barra de estado transparente.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_main);

        com.ivanmarty.rovecast.util.FirstLaunchManager firstLaunchManager = new com.ivanmarty.rovecast.util.FirstLaunchManager(this);
        if (firstLaunchManager.isFirstLaunch()) {
            Toast.makeText(this, "Welcome! It might take a moment to load nearby stations and images for the first time.", Toast.LENGTH_LONG).show();
            firstLaunchManager.setFirstLaunch(false);
        }

        ConsentManager.requestIfNeeded(this);

        CastContext.getSharedInstance(this).addCastStateListener(state -> {
            Log.d("CastState", "State changed: " + state);
        });

        top = findViewById(R.id.topBar);

        // SOLUCIÓN: Aplica un listener a la barra de herramientas para ajustar su margen superior
        // dinámicamente según el tamaño de la barra de estado del sistema.
        ViewCompat.setOnApplyWindowInsetsListener(top, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Se obtiene la altura de la barra de estado y se aplica como margen superior a la Toolbar.
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.topMargin = insets.top;
            v.setLayoutParams(mlp);
            // Se devuelven los insets para que otras vistas (como el teclado) puedan usarlos.
            return windowInsets;
        });

        setSupportActionBar(top);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        top.setNavigationIcon(R.drawable.ic_toolbar_logo_padded);

        // Ajuste de insets para el contenedor inferior (mini player + navegación) para evitar solapamientos
        View bottomContainer = findViewById(R.id.bottomContainer);
        if (bottomContainer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomContainer, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), insets.bottom);
                return windowInsets;
            });
        }

        // Asegura que el contenido principal nunca quede oculto ni por la Toolbar ni por el contenedor inferior
        View fragmentContainer = findViewById(R.id.fragmentContainer);
        if (fragmentContainer != null && bottomContainer != null) {
            Runnable applySafePadding = () -> {
                int topPadding = top != null ? top.getBottom() : 0; // incluye altura + margen/top inset
                int bottomPadding = bottomContainer.getHeight();     // incluye altura + padding por insets
                fragmentContainer.setPadding(
                        fragmentContainer.getPaddingLeft(),
                        topPadding,
                        fragmentContainer.getPaddingRight(),
                        bottomPadding
                );
            };
            // Aplicar al inicializar y cada vez que cambie la altura del contenedor inferior
            fragmentContainer.post(applySafePadding);
            bottomContainer.getViewTreeObserver().addOnGlobalLayoutListener(applySafePadding::run);
            top.getViewTreeObserver().addOnGlobalLayoutListener(applySafePadding::run);
        }

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

        setupMiniPlayer();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializeMediaController();
    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseMediaController();
    }

    private void setupMiniPlayer() {
        // Obtén directamente el contenedor del mini player
        View container = findViewById(R.id.miniPlayerContainer);
        if (!(container instanceof ConstraintLayout)) {
            Log.e("MainActivity", "miniPlayerContainer not found or wrong type");
            return;
        }
        miniPlayerContainer = (ConstraintLayout) container;

        // Inicializa sub-vistas de forma segura
        miniPlayerLogo = miniPlayerContainer.findViewById(R.id.miniPlayerLogo);
        miniPlayerTitle = miniPlayerContainer.findViewById(R.id.miniPlayerTitle);
        miniPlayerStatus = miniPlayerContainer.findViewById(R.id.miniPlayerStatus);
        miniPlayerPlayPause = miniPlayerContainer.findViewById(R.id.miniPlayerPlayPause);

        if (miniPlayerPlayPause != null) {
            miniPlayerPlayPause.setOnClickListener(v -> {
                try {
                    if (mediaControllerFuture != null) {
                        MediaController mediaController = mediaControllerFuture.get();
                        if (mediaController != null) {
                            if (mediaController.isPlaying()) {
                                mediaController.pause();
                            } else {
                                mediaController.play();
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("MainActivity", "Error in play/pause click handler", e);
                }
            });
        }

        miniPlayerContainer.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(this, PlayerActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e("MainActivity", "Error starting PlayerActivity", e);
            }
        });
    }

    private void initializeMediaController() {
        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, PlaybackService.class));
        mediaControllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        mediaControllerFuture.addListener(() -> {
            try {
                MediaController mediaController = mediaControllerFuture.get();
                mediaController.addListener(this);
                updateUiWithCurrentState(mediaController);
            } catch (InterruptedException | ExecutionException e) {
                Log.e("MainActivity", "Error al conectar con MediaController", e);
            }
        }, MoreExecutors.directExecutor());
    }

    private void releaseMediaController() {
        try {
            mediaControllerFuture.get().removeListener(this);
        } catch (Exception e) { /* Ignored */ }
        MediaController.releaseFuture(mediaControllerFuture);
    }

    @SuppressWarnings("unused")
    public void onMediaItemChanged(@Nullable MediaItem mediaItem, int reason) {
        updateUiWithCurrentState(null); // Pass null to force re-evaluation
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        updateMiniPlayerState(isPlaying);
    }

    private void updateUiWithCurrentState(MediaController mediaController) {
        // If controller is null, try to get it from the future
        if (mediaController == null) {
            try {
                mediaController = mediaControllerFuture.get();
            } catch (Exception e) {
                setMiniPlayerVisibility(false);
                return;
            }
        }

        boolean shouldBeVisible = mediaController != null && mediaController.getCurrentMediaItem() != null;
        setMiniPlayerVisibility(shouldBeVisible);

        if (shouldBeVisible) {
            updateMiniPlayerMetadata(mediaController.getCurrentMediaItem());
            updateMiniPlayerState(mediaController.isPlaying());
        }
    }

    private void updateMiniPlayerMetadata(MediaItem item) {
        if (item != null && item.mediaMetadata != null) {
            miniPlayerTitle.setText(item.mediaMetadata.title);
            Glide.with(this)
                    .load(item.mediaMetadata.artworkUri)
                    .placeholder(R.drawable.ic_radio_placeholder)
                    .error(R.drawable.ic_radio_placeholder)
                    .into(miniPlayerLogo);
        }
    }

    private void updateMiniPlayerState(boolean isPlaying) {
        if (isPlaying) {
            miniPlayerStatus.setText(R.string.status_playing);
            miniPlayerPlayPause.setImageResource(R.drawable.ic_pause);
            try {
                int accent = androidx.core.content.ContextCompat.getColor(this, R.color.accent);
                miniPlayerPlayPause.setColorFilter(accent);
            } catch (Exception ignored) {}
        } else {
            miniPlayerStatus.setText(R.string.status_paused);
            miniPlayerPlayPause.setImageResource(R.drawable.ic_play_arrow);
            try {
                int onDark = androidx.core.content.ContextCompat.getColor(this, R.color.on_dark);
                miniPlayerPlayPause.setColorFilter(onDark);
            } catch (Exception ignored) {}
        }
    }

    private void setMiniPlayerVisibility(boolean shouldBeVisible) {
        if (miniPlayerContainer == null) return;

        boolean isVisible = miniPlayerContainer.getVisibility() == View.VISIBLE;
        if (isVisible == shouldBeVisible) {
            return;
        }

        if (shouldBeVisible) {
            miniPlayerContainer.setVisibility(View.VISIBLE);
            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            miniPlayerContainer.startAnimation(slideUp);
        } else {
            Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
            slideDown.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    miniPlayerContainer.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            miniPlayerContainer.startAnimation(slideDown);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_top, menu);
        try {
            com.google.android.gms.cast.framework.CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu, R.id.media_route_menu_item);
        } catch (Throwable ignored) {}
        menu.findItem(R.id.menu_premium).setVisible(!PremiumManager.isPremium(this));
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
                        invalidateOptionsMenu();
                    }
                    @Override public void onError(String msg) {
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                    }
                });
                billing.start();
            }
            top.postDelayed(() -> billing.launchPurchase(), 400);
            return true;
        } else if (itemId == R.id.menu_alarm) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new AlarmFragment())
                    .addToBackStack(null)
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

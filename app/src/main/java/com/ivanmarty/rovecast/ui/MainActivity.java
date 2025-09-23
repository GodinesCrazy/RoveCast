package com.ivanmarty.rovecast.ui;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import android.content.res.ColorStateList;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.media3.ui.PlayerControlView;

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
import com.ivanmarty.rovecast.data.FavoriteRepository;
import com.ivanmarty.rovecast.model.Station;
import com.ivanmarty.rovecast.player.PlaybackService;
import com.ivanmarty.rovecast.ui.alarm.AlarmFragment;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private BillingManager billing;
    private MaterialToolbar top;

    private CardView miniPlayerContainer;
    private TextView miniPlayerTitle, miniPlayerStatus;
    private ImageView miniPlayerLogo;
    private ImageButton miniPlayerFavorite;
    private ImageButton miniPlayerPlayPause;
    private ProgressBar miniPlayerProgress;
    private androidx.mediarouter.app.MediaRouteButton miniPlayerCast;

    private MediaItem currentMediaItem;

    private ListenableFuture<MediaController> controllerFuture;
    private MediaController controller;
    private Player.Listener playerListener;
    private FavoriteRepository favoriteRepository;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

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

        ViewCompat.setOnApplyWindowInsetsListener(top, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.topMargin = insets.top;
            v.setLayoutParams(mlp);
            return windowInsets;
        });

        setSupportActionBar(top);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        top.setNavigationIcon(R.drawable.ic_toolbar_logo_padded);

        View bottomContainer = findViewById(R.id.bottomContainer);
        if (bottomContainer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomContainer, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), insets.bottom);
                return windowInsets;
            });
        }

        View fragmentContainer = findViewById(R.id.fragmentContainer);
        if (fragmentContainer != null) {
            Runnable applySafePadding = () -> {
                // SOLUCIÓN COMPLETA: Eliminar TODOS los paddings para expansión total
                // Sin topPadding = RecyclerView comienza desde arriba (elimina espacio negro)
                // Sin bottomPadding = RecyclerView se extiende hacia abajo (más radios visibles)
                
                fragmentContainer.setPadding(
                        fragmentContainer.getPaddingLeft(),
                        0, // Sin padding top - permite que la lista comience arriba
                        fragmentContainer.getPaddingRight(),
                        0 // Sin padding bottom - permite expansión completa
                );
            };
            fragmentContainer.post(applySafePadding);
            // Mantener solo el listener del top para cambios de sistema UI
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

        favoriteRepository = new FavoriteRepository(this);

        miniPlayerContainer = findViewById(R.id.miniPlayerContainer);
        miniPlayerTitle = findViewById(R.id.miniPlayerTitle);
        miniPlayerStatus = findViewById(R.id.miniPlayerStatus);
        miniPlayerLogo = findViewById(R.id.miniPlayerLogo);
        miniPlayerFavorite = findViewById(R.id.miniPlayerFavorite);
        miniPlayerPlayPause = findViewById(R.id.miniPlayerPlayPause);
        miniPlayerProgress = findViewById(R.id.miniPlayerProgress);
        miniPlayerCast = findViewById(R.id.miniPlayerCast);
        
        // Configurar el botón Cast del mini-player
        try {
            com.google.android.gms.cast.framework.CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), miniPlayerCast);
        } catch (Throwable ignored) {}

        miniPlayerContainer.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(this, PlayerActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e("MainActivity", "Error starting PlayerActivity", e);
            }
        });

        miniPlayerFavorite.setOnClickListener(v -> {
            if (currentMediaItem != null && currentMediaItem.mediaMetadata != null) {
                try {
                    Station station = createStationFromMediaItem(currentMediaItem);
                    String stationId = extractStationId(currentMediaItem);

                    if (station != null && stationId != null) {
                        boolean isCurrentlyFavorite = favoriteRepository.isFavoriteNow(stationId);
                        favoriteRepository.toggleFavorite(station, !isCurrentlyFavorite);

                        String message = !isCurrentlyFavorite ? "Added to favorites" : "Removed from favorites";
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();

                        // CRÍTICO: Actualizar UI inmediatamente después de cambiar favorito
                        updateUiWithCurrentState();

                        Animation pulse = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
                        miniPlayerFavorite.startAnimation(pulse);
                    } else {
                        Toast.makeText(this, "Unable to add to favorites", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e("MainActivity", "Error handling favorite button", e);
                    Toast.makeText(this, "Error updating favorites", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // FIX CRÍTICO: Mostrar mini-player desde el inicio para consistencia visual
        updateUiWithCurrentState();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializeMediaController();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (controller != null && playerListener != null) {
            controller.removeListener(playerListener);
        }
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
        }
    }

    private void initializeMediaController() {
        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, PlaybackService.class));
        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                controller = controllerFuture.get();
                // Configurar OnClickListener para el botón play/pause
                setupPlayPauseButton();

                playerListener = new Player.Listener() {
                    @Override
                    public void onEvents(@NonNull Player player, @NonNull Player.Events events) {
                        updateUiWithCurrentState();
                    }
                };
                controller.addListener(playerListener);

                updateUiWithCurrentState();
            } catch (Exception e) {
                Log.e("MainActivity", "Error connecting to MediaController", e);
            }
        }, MoreExecutors.directExecutor());
    }

    private void setupPlayPauseButton() {
        if (miniPlayerPlayPause != null) {
            miniPlayerPlayPause.setOnClickListener(v -> {
                if (controller != null) {
                    if (controller.isPlaying()) {
                        controller.pause();
                    } else {
                        controller.play();
                    }
                    // Actualizar inmediatamente el icono
                    updatePlayPauseButton(controller.isPlaying());
                }
            });
        }
    }

    private void updatePlayPauseButton(boolean isPlaying) {
        if (miniPlayerPlayPause != null) {
            if (isPlaying) {
                miniPlayerPlayPause.setImageResource(R.drawable.ic_pause);
            } else {
                miniPlayerPlayPause.setImageResource(R.drawable.ic_play_arrow);
            }
        }
    }

    private void updateUiWithCurrentState() {
        // FIX CRÍTICO: Mini-player siempre visible para consistencia visual
        miniPlayerContainer.setVisibility(View.VISIBLE);
        
        if (controller == null || controller.getCurrentMediaItem() == null) {
            // Mostrar estado por defecto cuando no hay reproducción
            showDefaultMiniPlayerState();
            return;
        }
        MediaItem mediaItem = controller.getCurrentMediaItem();
        MediaMetadata metadata = mediaItem.mediaMetadata;
        currentMediaItem = mediaItem; // Actualizar referencia actual

        // Habilitar botones cuando hay reproducción activa
        miniPlayerPlayPause.setEnabled(true);
        miniPlayerFavorite.setEnabled(true);
        miniPlayerContainer.setClickable(true);

        // Actualizar el icono del botón play/pause según el estado actual
        updatePlayPauseButton(controller != null && controller.isPlaying());

        // Información de la estación
        miniPlayerTitle.setText(metadata.title);
        String status = controller.isPlaying() ? getString(R.string.playback_state_playing) : getString(R.string.playback_state_paused);
        miniPlayerStatus.setText(status);

        // Logo con carga elegante
        Glide.with(this)
                .load(metadata.artworkUri)
                .placeholder(R.drawable.ic_radio_placeholder)
                .into(miniPlayerLogo);

        // Estado de favorito
        boolean isFavorite = favoriteRepository.isFavoriteNow(mediaItem.mediaId);
        miniPlayerFavorite.setImageResource(isFavorite ? R.drawable.ic_heart : R.drawable.ic_heart_outline);
        // Cambiar color directamente según estado de favorito
        int color = isFavorite ? 
            ContextCompat.getColor(this, R.color.accent) : 
            ContextCompat.getColor(this, android.R.color.white);
        ImageViewCompat.setImageTintList(miniPlayerFavorite, ColorStateList.valueOf(color));

        // SPOTIFY-LIKE: Estados visuales elegantes
        float alpha = controller.isPlaying() ? 1.0f : 0.85f;
        miniPlayerContainer.setAlpha(alpha);

        // SPOTIFY-LIKE: Progreso de reproducción fluido (para streams en vivo, mostrar actividad)
        if (controller.isPlaying()) {
            miniPlayerProgress.setIndeterminate(false);
            // Para radio en vivo, simular progreso continuo
            miniPlayerProgress.setProgress(75); // Indicador visual de actividad
        } else {
            miniPlayerProgress.setProgress(0);
        }
    }

    /**
     * Muestra el estado por defecto del mini-player cuando no hay reproducción activa
     * Esto garantiza que el mini-player esté siempre visible para consistencia visual
     */
    private void showDefaultMiniPlayerState() {
        miniPlayerTitle.setText("RoveCast");
        miniPlayerStatus.setText("Selecciona una estación para comenzar");
        miniPlayerLogo.setImageResource(R.mipmap.ic_launcher); // Usar icono oficial de la app
        miniPlayerPlayPause.setImageResource(R.drawable.ic_play_arrow);
        miniPlayerFavorite.setImageResource(R.drawable.ic_heart_outline);
        // Establecer color blanco por defecto
        ImageViewCompat.setImageTintList(miniPlayerFavorite,
                ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.white)));
        miniPlayerProgress.setProgress(0);
        miniPlayerContainer.setAlpha(0.7f); // Opacidad reducida para indicar estado inactivo
        
        // Deshabilitar botones cuando no hay reproducción
        miniPlayerPlayPause.setEnabled(false);
        miniPlayerFavorite.setEnabled(false);
        miniPlayerContainer.setClickable(false);
    }

    private Station createStationFromMediaItem(MediaItem mediaItem) {
        if (mediaItem == null || mediaItem.mediaMetadata == null) {
            return null;
        }

        Station station = new Station();
        try {
            if (mediaItem.mediaMetadata.title != null) {
                station.name = mediaItem.mediaMetadata.title.toString();
            }

            if (mediaItem.requestMetadata != null && mediaItem.requestMetadata.mediaUri != null) {
                station.url = mediaItem.requestMetadata.mediaUri.toString();
            } else if (mediaItem.localConfiguration != null && mediaItem.localConfiguration.uri != null) {
                station.url = mediaItem.localConfiguration.uri.toString();
            }

            if (mediaItem.mediaMetadata.artworkUri != null) {
                station.favicon = mediaItem.mediaMetadata.artworkUri.toString();
            }

            if (mediaItem.mediaId != null) {
                station.stationuuid = mediaItem.mediaId;
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error creating Station from MediaItem", e);
        }

        return station;
    }

    private String extractStationId(MediaItem mediaItem) {
        if (mediaItem == null) {
            return null;
        }

        if (mediaItem.mediaId != null && !mediaItem.mediaId.isEmpty()) {
            return mediaItem.mediaId;
        }

        if (mediaItem.requestMetadata != null && mediaItem.requestMetadata.mediaUri != null) {
            return mediaItem.requestMetadata.mediaUri.toString();
        } else if (mediaItem.localConfiguration != null && mediaItem.localConfiguration.uri != null) {
            return mediaItem.localConfiguration.uri.toString();
        }

        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_top, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_privacy) {
            Intent intent = new Intent(this, PrivacyPolicyActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

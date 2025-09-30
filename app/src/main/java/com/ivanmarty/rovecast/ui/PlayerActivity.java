package com.ivanmarty.rovecast.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.ivanmarty.rovecast.R;
import com.ivanmarty.rovecast.ads.AdsManager;
import com.ivanmarty.rovecast.billing.PremiumManager;
import com.ivanmarty.rovecast.cast.CastManager;
import com.ivanmarty.rovecast.data.FavoriteRepository;
import com.ivanmarty.rovecast.data.SleepTimerPreset;
import com.ivanmarty.rovecast.data.SleepTimerPresetRepository;
import com.ivanmarty.rovecast.model.Station;
import com.ivanmarty.rovecast.ui.alarm.AlarmFragmentSimplified;
import com.ivanmarty.rovecast.ui.alarm.AlarmScheduler;
import com.ivanmarty.rovecast.player.PlaybackService;
import com.ivanmarty.rovecast.player.SleepTimerManager;
import com.ivanmarty.rovecast.ui.adapter.SleepTimerPresetAdapter;

import java.util.concurrent.TimeUnit;

import jp.wasabeef.glide.transformations.BlurTransformation;

@androidx.media3.common.util.UnstableApi
public class PlayerActivity extends AppCompatActivity {

    private ListenableFuture<MediaController> controllerFuture;
    private MediaController controller;
    private PlayerView playerView;
    private ImageView ivLogo;
    private ImageView ivBackground; // Para el fondo difuso
    private com.facebook.shimmer.ShimmerFrameLayout shimmerArt;
    private ProgressBar bufferingProgress;
    private TextView tvTitle, tvMeta;
    private ImageButton btnPlayPause, btnNext, btnPrevious, btnShuffle, btnRepeat;
    private ImageButton btnShare, btnFavorite, btnSleepTimer, btnAlarm;
    private FavoriteRepository favoriteRepository;
    private SleepTimerPresetRepository sleepTimerPresetRepository;
    private boolean isFavorite;
    private Station currentStation;
    private Player.Listener playerListener; // Para poder removerlo después

    @Override
    public void finish() {
        AdsManager.get().runWithMaybeAd(this, () -> {
            super.finish();
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        favoriteRepository = new FavoriteRepository(this);
        sleepTimerPresetRepository = new SleepTimerPresetRepository(this);

        // --- Toolbar ---
        MaterialToolbar toolbar = findViewById(R.id.playerTopBar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            // El logo actúa como botón de "atrás"
            toolbar.setNavigationIcon(R.drawable.ic_toolbar_logo_padded);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // --- Views ---
        playerView = findViewById(R.id.playerView);
        playerView.setControllerAutoShow(false);
        ivLogo = findViewById(R.id.ivLogo);
        ivBackground = findViewById(R.id.ivBackground); // Obtener referencia
        shimmerArt = findViewById(R.id.shimmerArt);
        bufferingProgress = findViewById(R.id.bufferingProgress);
        if (shimmerArt != null) { shimmerArt.startShimmer(); }
        tvTitle = findViewById(R.id.tvTitle);
        tvMeta = findViewById(R.id.tvMeta);

        // --- Initialize Control Buttons ---
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnShuffle = findViewById(R.id.btnShuffle);
        btnRepeat = findViewById(R.id.btnRepeat);
        androidx.mediarouter.app.MediaRouteButton btnDevices = findViewById(R.id.btnDevices);
        CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), btnDevices);
        btnShare = findViewById(R.id.btnShare);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnAlarm = findViewById(R.id.btnAlarm);
        btnSleepTimer = findViewById(R.id.btnSleepTimer);
    }

    private void setupClickListeners() {
        if (controller == null) return;

        btnPlayPause.setOnClickListener(v -> {
            if (controller.isPlaying()) {
                controller.pause();
            } else {
                // SOLUCIÓN ALTERNATIVA: Preparar antes de reproducir para reanudar streams.
                controller.prepare();
                controller.play();
            }
        });

        btnNext.setOnClickListener(v -> controller.seekToNextMediaItem());
        btnPrevious.setOnClickListener(v -> controller.seekToPreviousMediaItem());

        btnShuffle.setOnClickListener(v -> controller.setShuffleModeEnabled(!controller.getShuffleModeEnabled()));

        btnRepeat.setOnClickListener(v -> {
            int nextMode = (controller.getRepeatMode() + 1) % 3;
            controller.setRepeatMode(nextMode);
        });

        btnFavorite.setOnClickListener(v -> {
            toggleFavorite();
            updateButtonStates(); // Actualizar estados después del cambio
        });

        btnAlarm.setOnClickListener(v -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new AlarmFragmentSimplified())
                    .addToBackStack(null)
                    .commit();
        });

        btnSleepTimer.setOnClickListener(v -> {
            showTimerDialog();
            updateButtonStates(); // Actualizar estados después del cambio
        });

        btnShare.setOnClickListener(v -> {
            if (currentStation != null) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                // SOLUCIÓN: Usar concatenación de String simple, es más legible.
                String shareText = "Listening to " + currentStation.name + " on RoveCast! \n\n" +
                        "Download the app and tune in: https://play.google.com/store/apps/details?id=com.ivanmarty.rovecast";
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Listen to " + currentStation.name + " on RoveCast");
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                startActivity(Intent.createChooser(shareIntent, "Share via"));
            } else {
                Toast.makeText(this, "Station info not available to share.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_player, menu);
        MenuItem favoriteItem = menu.findItem(R.id.menu_favorite);
        if (isFavorite) {
            favoriteItem.setIcon(R.drawable.ic_heart);
        } else {
            favoriteItem.setIcon(R.drawable.ic_heart_outline);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_timer) {
            showTimerDialog();
            return true;
        } else if (itemId == R.id.menu_favorite) {
            toggleFavorite();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, PlaybackService.class));
        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                controller = controllerFuture.get();
                // SOLUCIÓN: Crear el objeto Station aquí, una sola vez.
                if (controller.getCurrentMediaItem() != null) {
                    currentStation = createStationFromMediaItem(controller.getCurrentMediaItem());
                }
                setupClickListeners(); // Setup listeners once controller is available
                initializeUIWithController(controller);
                updateButtonStates(); // Actualizar estados de botones al iniciar
            } catch (Exception e) {
                Log.e("PlayerActivity", "Error connecting to player", e);
            }
        }, MoreExecutors.directExecutor());
    }

    private void initializeUIWithController(MediaController mediaController) {
        this.controller = mediaController;
        playerView.setPlayer(mediaController);
        updateUI(); // Initial UI update

        playerListener = new Player.Listener() {
            @Override
            public void onEvents(@NonNull Player player, @NonNull Player.Events events) {
                // This single callback updates the UI on any relevant change.
                updateUI();
                updateButtonStates(); // Actualizar estados en cada cambio
            }
        };
        mediaController.addListener(playerListener);
    }

    private void updateUI() {
        if (controller == null || controller.getCurrentMediaItem() == null) return;

        MediaItem mediaItem = controller.getCurrentMediaItem();
        MediaMetadata metadata = mediaItem.mediaMetadata;

        tvTitle.setText(metadata.title);
        tvMeta.setText(metadata.artist);

        if (metadata.artworkUri != null) {
            shimmerArt.stopShimmer();
            shimmerArt.hideShimmer();

            // Cargar logo principal
            Glide.with(this)
                    .load(metadata.artworkUri)
                    .placeholder(R.drawable.ic_radio_placeholder)
                    .error(R.drawable.ic_radio_placeholder) // Fallback si la carga falla
                    .into(ivLogo);

            // Cargar fondo difuminado con fallback a gradiente
            try {
                Glide.with(this)
                        .load(metadata.artworkUri)
                        .transform(new BlurTransformation(25, 3))
                        .error(R.drawable.bg_player_gradient)
                        .into(ivBackground);
            } catch (Exception e) {
                Log.e("PlayerActivity", "Error applying blur transformation", e);
                ivBackground.setImageResource(R.drawable.bg_player_gradient);
            }
        } else {
            // Si no hay artwork, mantener el shimmer y el fondo por defecto
            shimmerArt.startShimmer();
            ivLogo.setImageResource(R.drawable.ic_radio_placeholder);
            ivBackground.setImageResource(R.drawable.bg_player_gradient);
        }

        updateFavoriteButton(mediaItem.mediaId);

        // Integración con CastManager: enviar información actual al dispositivo Cast si está conectado
        try {
            String url = getUrlFromMediaItem(mediaItem);
            String title = metadata.title != null ? metadata.title.toString() : "Radio Station";
            String image = metadata.artworkUri != null ? metadata.artworkUri.toString() : null;

            CastManager.get().setCurrentMedia(url, title, image);
        } catch (Exception e) {
            Log.e("PlayerActivity", "Error updating Cast information", e);
        }
        updatePlayerStateUI();
    }

    private void updateFavoriteButton(String stationUuid) {
        isFavorite = favoriteRepository.isFavoriteNow(stationUuid);
        btnFavorite.setImageResource(isFavorite ? R.drawable.ic_heart : R.drawable.ic_heart_outline);
        int favoriteColor = isFavorite ? getColor(R.color.accent) : getColor(R.color.textSecondary);
        btnFavorite.setColorFilter(favoriteColor);
    }

    private String getUrlFromMediaItem(MediaItem mediaItem) {
        if (mediaItem.localConfiguration != null) {
            return mediaItem.localConfiguration.uri.toString();
        }
        return null;
    }

    private void updatePlayerStateUI() {
        // Update play/pause button
        btnPlayPause.setImageResource(controller.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play_arrow);

        // Update shuffle button with tinting
        boolean isShuffleEnabled = controller.getShuffleModeEnabled();
        btnShuffle.setAlpha(isShuffleEnabled ? 1f : 0.7f);
        int shuffleColor = isShuffleEnabled ? getColor(R.color.accent) : getColor(R.color.textSecondary);
        btnShuffle.setColorFilter(shuffleColor);

        // Update repeat button with tinting
        int repeatMode = controller.getRepeatMode();
        float repeatAlpha = 0.7f;
        int repeatIcon = R.drawable.ic_repeat;
        int repeatColor = getColor(R.color.textSecondary);

        if (repeatMode == Player.REPEAT_MODE_ONE) {
            repeatAlpha = 1f;
            repeatIcon = R.drawable.ic_repeat_one;
            repeatColor = getColor(R.color.accent);
        } else if (repeatMode == Player.REPEAT_MODE_ALL) {
            repeatAlpha = 1f;
            repeatColor = getColor(R.color.accent);
        }
        btnRepeat.setAlpha(repeatAlpha);
        btnRepeat.setImageResource(repeatIcon);
        btnRepeat.setColorFilter(repeatColor);

        // Update buffering indicator
        if (bufferingProgress != null) {
            bufferingProgress.setVisibility(controller.getPlaybackState() == Player.STATE_BUFFERING ? View.VISIBLE : View.GONE);
        }
    }

    private Station createStationFromMediaItem(MediaItem mediaItem) {
        if (mediaItem == null) return null;
        Station station = new Station();
        station.stationuuid = mediaItem.mediaId;
        if (mediaItem.mediaMetadata.title != null) {
            station.name = mediaItem.mediaMetadata.title.toString();
        }
        if (mediaItem.requestMetadata.mediaUri != null) {
            station.url_resolved = mediaItem.requestMetadata.mediaUri.toString();
        }
        if (mediaItem.mediaMetadata.artworkUri != null) {
            station.favicon = mediaItem.mediaMetadata.artworkUri.toString();
        }
        return station;
    }

    private void toggleFavorite() {
        if (currentStation == null) {
            Toast.makeText(this, "Station data not available.", Toast.LENGTH_SHORT).show();
            return;
        }
        isFavorite = !isFavorite;
        favoriteRepository.toggleFavorite(currentStation, isFavorite);
        updateUI(); // Update UI to reflect the change
        Toast.makeText(this, isFavorite ? "Added to favorites" : "Removed from favorites", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // SOLUCIÓN: Liberar el controlador y remover el listener para evitar memory leaks.
        if (controller != null && playerListener != null) {
            controller.removeListener(playerListener);
        }
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
        }
        controller = null;
    }

    private void showTimerDialog() {
        if (PremiumManager.isPremium(this)) {
            showPremiumTimerDialog();
        } else {
            showFreeTimerDialog();
        }
    }

    private void showFreeTimerDialog() {
        final CharSequence[] items;
        final SleepTimerManager timerManager = SleepTimerManager.getInstance();

        if (timerManager.isTimerRunning()) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(timerManager.getMillisRemaining());
            items = new CharSequence[]{"Cancelar (" + minutes + " min)"};
        } else {
            items = new CharSequence[]{"15", "30", "60", "90"};
        }

        new AlertDialog.Builder(this)
                .setTitle("Sleep Timer (min)")
                .setItems(items, (dialog, item) -> {
                    if (timerManager.isTimerRunning()) {
                        timerManager.cancelTimer();
                        Toast.makeText(this, "Temporizador cancelado", Toast.LENGTH_SHORT).show();
                        updateButtonStates(); // Actualizar estado después de cancelar
                    } else {
                        long minutes = Long.parseLong(items[item].toString());
                        startSleepTimer(minutes);
                        Toast.makeText(this, "RoveCast se detendrá en " + minutes + " minutos", Toast.LENGTH_SHORT).show();
                        updateButtonStates(); // Actualizar estado después de iniciar
                    }
                })
                .show();
    }

    private void showPremiumTimerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_sleep_timer_presets, null);
        builder.setView(view);
        builder.setTitle("Sleep Timer Presets");

        RecyclerView recyclerView = view.findViewById(R.id.recycler_presets);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        SleepTimerPresetAdapter adapter = new SleepTimerPresetAdapter(new SleepTimerPresetAdapter.OnPresetClickListener() {
            @Override
            public void onPresetClick(SleepTimerPreset preset) {
                startSleepTimer(preset.durationMinutes);
                Toast.makeText(PlayerActivity.this, "RoveCast se detendrá en " + preset.durationMinutes + " minutos", Toast.LENGTH_SHORT).show();
                updateButtonStates(); // Actualizar estado después de iniciar timer
            }

            @Override
            public void onPresetLongClick(SleepTimerPreset preset) {
                showEditPresetDialog(preset);
            }
        });
        recyclerView.setAdapter(adapter);

        sleepTimerPresetRepository.getAll().observe(this, adapter::setPresets);

        view.findViewById(R.id.btnAddPreset).setOnClickListener(v -> showAddPresetDialog());

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void showAddPresetDialog() {
        showEditPresetDialog(null);
    }

    private void showEditPresetDialog(SleepTimerPreset preset) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(preset == null ? "Add Preset" : "Edit Preset");

        View view = getLayoutInflater().inflate(R.layout.dialog_edit_preset, null);
        builder.setView(view);

        EditText etName = view.findViewById(R.id.etPresetName);
        EditText etDuration = view.findViewById(R.id.etPresetDuration);

        if (preset != null) {
            etName.setText(preset.name);
            etDuration.setText(String.valueOf(preset.durationMinutes));
        }

        builder.setPositiveButton(preset == null ? "Add" : "Save", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String durationStr = etDuration.getText().toString().trim();

            if (name.isEmpty() || durationStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            long duration = Long.parseLong(durationStr);

            if (preset == null) {
                SleepTimerPreset newPreset = new SleepTimerPreset();
                newPreset.name = name;
                newPreset.durationMinutes = duration;
                sleepTimerPresetRepository.insert(newPreset);
            } else {
                preset.name = name;
                preset.durationMinutes = duration;
                sleepTimerPresetRepository.update(preset);
            }
        });

        if (preset != null) {
            builder.setNeutralButton("Delete", (dialog, which) -> sleepTimerPresetRepository.delete(preset));
        }

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void startSleepTimer(long minutes) {
        SleepTimerManager.getInstance().startTimer(minutes, controller != null ? controller::pause : null);
    }

    // ===== NUEVOS MÉTODOS PARA ESTADOS ACTIVOS DE ICONOS =====

    // Método para actualizar estados visuales de los iconos
    private void updateButtonStates() {
        updateFavoriteButtonState();
        updateAlarmButtonState();
        updateSleepTimerButtonState();
    }

    private void updateFavoriteButtonState() {
        if (currentStation != null) {
            boolean isFav = favoriteRepository.isFavoriteNow(currentStation.stationuuid);
            btnFavorite.setActivated(isFav);
            btnFavorite.setSelected(isFav);
            // Cambiar icono según estado
            if (isFav) {
                btnFavorite.setImageResource(R.drawable.ic_heart);
            } else {
                btnFavorite.setImageResource(R.drawable.ic_heart_outline);
            }
        }
    }

    private void updateAlarmButtonState() {
        // Verificar si hay alarmas activas
        boolean hasActiveAlarms = checkIfAlarmsActive();
        btnAlarm.setActivated(hasActiveAlarms);
    }

    private void updateSleepTimerButtonState() {
        // Verificar si sleep timer está activo
        boolean isTimerActive = SleepTimerManager.getInstance().isTimerRunning();
        btnSleepTimer.setActivated(isTimerActive);
    }

    private boolean checkIfAlarmsActive() {
        AlarmScheduler alarmScheduler = new AlarmScheduler(this);
        return alarmScheduler.load().isEnabled();
    }
}
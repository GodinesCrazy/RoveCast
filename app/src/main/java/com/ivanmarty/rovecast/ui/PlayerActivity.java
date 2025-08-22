package com.ivanmarty.rovecast.ui;

import android.content.ComponentName;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
import com.ivanmarty.rovecast.billing.PremiumManager;
import com.ivanmarty.rovecast.data.FavoriteRepository;
import com.ivanmarty.rovecast.data.SleepTimerPreset;
import com.ivanmarty.rovecast.data.SleepTimerPresetRepository;
import com.ivanmarty.rovecast.model.Station;
import com.ivanmarty.rovecast.player.PlaybackService;
import com.ivanmarty.rovecast.player.SleepTimerManager;
import com.ivanmarty.rovecast.ui.adapter.SleepTimerPresetAdapter;

import java.util.concurrent.TimeUnit;

@androidx.media3.common.util.UnstableApi
public class PlayerActivity extends AppCompatActivity {

    private ListenableFuture<MediaController> controllerFuture;
    private MediaController controller;
    private PlayerView playerView;
    private ImageView ivLogo;
    private TextView tvTitle, tvMeta;
    private FavoriteRepository favoriteRepository;
    private SleepTimerPresetRepository sleepTimerPresetRepository;
    private boolean isFavorite;
    private String stationId;
    private Station currentStation;


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
        tvTitle = findViewById(R.id.tvTitle);
        tvMeta = findViewById(R.id.tvMeta);
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
        if (item.getItemId() == R.id.menu_timer) {
            showTimerDialog();
            return true;
        } else if (item.getItemId() == R.id.menu_favorite) {
            isFavorite = !isFavorite;
            favoriteRepository.toggleFavorite(currentStation, isFavorite);
            if (isFavorite) {
                item.setIcon(R.drawable.ic_heart);
                Toast.makeText(this, "Añadido a favoritos", Toast.LENGTH_SHORT).show();
            } else {
                item.setIcon(R.drawable.ic_heart_outline);
                Toast.makeText(this, "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
            }
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
                initializeUIWithController(controller);
            } catch (Exception e) {
                Log.e("PlayerActivity", "Error connecting to player", e);
            }
        }, MoreExecutors.directExecutor());
    }

    private void initializeUIWithController(MediaController mediaController) {
        this.controller = mediaController;
        playerView.setPlayer(mediaController);
        updateUiForMediaItem(mediaController.getCurrentMediaItem());
        mediaController.addListener(new Player.Listener() {
            @Override
            public void onMediaMetadataChanged(@NonNull MediaMetadata mediaMetadata) {
                updateUiForMediaMetadata(mediaMetadata);
            }
        });
    }

    private void updateUiForMediaItem(androidx.media3.common.MediaItem mediaItem) {
        if (mediaItem != null) {
            updateUiForMediaMetadata(mediaItem.mediaMetadata);
            stationId = mediaItem.mediaId;
            isFavorite = favoriteRepository.isFavoriteNow(stationId);
            currentStation = new Station();
            currentStation.stationuuid = stationId;
            currentStation.name = mediaItem.mediaMetadata.title.toString();
            currentStation.url_resolved = mediaItem.requestMetadata.mediaUri.toString();
            currentStation.favicon = mediaItem.mediaMetadata.artworkUri.toString();
            invalidateOptionsMenu();
        } else {
            tvTitle.setText(R.string.app_name);
            tvMeta.setText("");
            ivLogo.setImageResource(R.drawable.ic_radio_placeholder);
        }
    }

    private void updateUiForMediaMetadata(MediaMetadata meta) {
        tvTitle.setText(meta.title);
        tvMeta.setText(meta.artist);
        if (meta.artworkUri != null) {
            Glide.with(this)
                    .load(meta.artworkUri)
                    .placeholder(R.drawable.ic_radio_placeholder)
                    .into(ivLogo);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (controller != null) {
            MediaController.releaseFuture(controllerFuture);
            controller = null;
        }
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
                    } else {
                        long minutes = Long.parseLong(items[item].toString());
                        startSleepTimer(minutes);
                        Toast.makeText(this, "RoveCast se detendrá en " + minutes + " minutos", Toast.LENGTH_SHORT).show();
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
            builder.setNeutralButton("Delete", (dialog, which) -> {
                sleepTimerPresetRepository.delete(preset);
            });
        }

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void startSleepTimer(long minutes) {
        SleepTimerManager.getInstance().startTimer(TimeUnit.MINUTES.toMillis(minutes), () -> {
            if (controller != null) {
                controller.stop();
            }
        });
    }
}

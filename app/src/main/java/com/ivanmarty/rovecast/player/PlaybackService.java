package com.ivanmarty.rovecast.player;

import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.DefaultMediaNotificationProvider;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

import com.ivanmarty.rovecast.R;

public class PlaybackService extends MediaSessionService {

    public static final String ACTION_PLAY = "com.ivanmarty.rovecast.player.ACTION_PLAY";
    public static final String EXTRA_URL = "EXTRA_URL";
    public static final String EXTRA_NAME = "EXTRA_NAME";
    public static final String EXTRA_LOGO = "EXTRA_LOGO";
    public static final String EXTRA_METADATA = "EXTRA_METADATA";
    public static final String EXTRA_STATION_UUID = "EXTRA_STATION_UUID";

    private MediaSession mediaSession;
    private ExoPlayer player;
    private ExecutorService backgroundExecutor;
    private Handler mainHandler;
    private MediaItem pendingMediaItem;

    @Override
    public void onCreate() {
        super.onCreate();
        
        // SOLUCIÓN: La inicialización del player y la sesión DEBEN estar en el hilo principal.
        backgroundExecutor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        try {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build();

            DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                    .setBufferDurationsMs(15000, 45000, 1000, 3000)
                    .build();

            player = new ExoPlayer.Builder(this)
                    .setAudioAttributes(audioAttributes, true)
                    .setHandleAudioBecomingNoisy(true)
                    .setLoadControl(loadControl)
                    .setWakeMode(C.WAKE_MODE_NETWORK)
                    .build();

            player.addListener(new Player.Listener() {
                @Override
                public void onPlayerError(@NonNull PlaybackException error) {
                    android.util.Log.e("PlaybackService", "Player error, stopping service", error);
                    stopSelf();
                }
            });

            mediaSession = new MediaSession.Builder(this, player).build();

            DefaultMediaNotificationProvider provider = new DefaultMediaNotificationProvider(this);
            setMediaNotificationProvider(provider);

        } catch (Exception e) {
            android.util.Log.e("PlaybackService", "Error initializing player", e);
            stopSelf(); // Si la inicialización falla, detener el servicio.
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_PLAY.equals(intent.getAction())) {
            String url = intent.getStringExtra(EXTRA_URL);
            String name = intent.getStringExtra(EXTRA_NAME);
            String logo = intent.getStringExtra(EXTRA_LOGO);
            String meta = intent.getStringExtra(EXTRA_METADATA);
            String stationUuid = intent.getStringExtra(EXTRA_STATION_UUID);

            if (url != null) {
                // Mover operaciones de construcción de MediaItem fuera del hilo principal
                backgroundExecutor.execute(() -> {
                    try {
                        MediaMetadata mediaMetadata = new MediaMetadata.Builder()
                                .setTitle(name)
                                .setArtist(meta)
                                .setArtworkUri(logo != null ? Uri.parse(logo) : null)
                                .build();

                        String mediaId = (stationUuid != null && !stationUuid.trim().isEmpty()) 
                            ? stationUuid 
                            : "station_" + System.currentTimeMillis();

                        MediaItem mediaItem = new MediaItem.Builder()
                                .setUri(url)
                                .setMediaId(mediaId)
                                .setMediaMetadata(mediaMetadata)
                                .build();

                        // Volver al hilo principal para interactuar con el player
                        mainHandler.post(() -> {
                            if (player != null) {
                                // Si el player está listo, reproducir inmediatamente
                                player.setMediaItem(mediaItem);
                                player.prepare();
                                player.play();
                            } else {
                                // Si el player no está listo, guardar el MediaItem para después
                                pendingMediaItem = mediaItem;
                            }
                        });
                        
                    } catch (Exception e) {
                        android.util.Log.e("PlaybackService", "Error processing media item", e);
                    }
                });
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public MediaSession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Override
    public void onDestroy() {
        // SOLUCIÓN: La liberación de recursos DEBE ocurrir en el hilo principal.
        if (mediaSession != null) {
            if (player != null) {
                player.release();
                player = null;
            }
            mediaSession.release();
            mediaSession = null;
        }
        if (backgroundExecutor != null) {
            backgroundExecutor.shutdown();
        }
        super.onDestroy();
    }
}

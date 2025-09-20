package com.ivanmarty.rovecast.player;

import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.DefaultMediaNotificationProvider;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

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

    @Override
    public void onCreate() {
        super.onCreate();

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build();

        // Buffer equilibrado: inicio rápido y estabilidad para streaming de radio
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        15000, // min 15s
                        45000, // max 45s
                        1000,  // empezar a reproducir con 1s
                        3000   // reanudar con 3s
                ).build();

        player = new ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .setLoadControl(loadControl)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .build();

        mediaSession = new MediaSession.Builder(this, player).build();

        // Notificación de reproducción (Media3)
        DefaultMediaNotificationProvider provider = new DefaultMediaNotificationProvider(this);
        // Algunas versiones de Media3 no exponen setSmallIconResourceId; usamos el valor por defecto.
        setMediaNotificationProvider(provider);
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
                MediaMetadata mediaMetadata = new MediaMetadata.Builder()
                        .setTitle(name)
                        .setArtist(meta)
                        .setArtworkUri(logo != null ? Uri.parse(logo) : null)
                        .build();

                MediaItem mediaItem = new MediaItem.Builder()
                        .setUri(url)
                        .setMediaId(stationUuid)
                        .setMediaMetadata(mediaMetadata)
                        .build();

                player.setMediaItem(mediaItem);
                player.prepare();
                player.play();
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
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}

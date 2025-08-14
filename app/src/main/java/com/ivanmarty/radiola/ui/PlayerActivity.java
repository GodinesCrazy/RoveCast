package com.ivanmarty.radiola.ui;

import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.mediarouter.app.MediaRouteButton;

import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.ivanmarty.radiola.R;
import com.ivanmarty.radiola.ads.AdsManager;
import com.ivanmarty.radiola.cast.CastManager;
import com.ivanmarty.radiola.player.SleepTimerManager;

import java.util.concurrent.TimeUnit;

@androidx.media3.common.util.UnstableApi
public class PlayerActivity extends AppCompatActivity {

    public static final String EXTRA_URL = "EXTRA_URL";
    public static final String EXTRA_NAME = "EXTRA_NAME";
    public static final String EXTRA_LOGO = "EXTRA_LOGO";
    public static final String EXTRA_COUNTRY = "EXTRA_COUNTRY";
    public static final String EXTRA_LANG = "EXTRA_LANG";
    public static final String EXTRA_BITRATE = "EXTRA_BITRATE";
    public static final String EXTRA_CODEC = "EXTRA_CODEC";
    public static final String EXTRA_TAGS = "EXTRA_TAGS";

    private ExoPlayer player;
    private ImageButton btnTimer;

    // Cast (auto-pausa local al iniciar/reanudar sesión)
    private CastContext castContext;
    private final SessionManagerListener<CastSession> castListener = new SessionManagerListener<CastSession>() {
        @Override public void onSessionStarting(CastSession session) {}
        @Override public void onSessionStarted(CastSession session, String s) { pauseLocalAndSendToCast(); }
        @Override public void onSessionStartFailed(CastSession session, int i) {}

        @Override public void onSessionEnding(CastSession session) { /* no-op */ }
        @Override public void onSessionEnded(CastSession session, int i) {}

        @Override public void onSessionResuming(CastSession session, String s) {}
        @Override public void onSessionResumed(CastSession session, boolean b) { pauseLocalAndSendToCast(); }
        @Override public void onSessionResumeFailed(CastSession session, int i) {}
        @Override public void onSessionSuspended(CastSession session, int i) {}
    };

    private String currentUrl, currentName, currentLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // Extras
        currentUrl  = getIntent().getStringExtra(EXTRA_URL);
        if (currentUrl == null || currentUrl.trim().isEmpty()) { finish(); return; }
        currentName = getIntent().getStringExtra(EXTRA_NAME);
        currentLogo = getIntent().getStringExtra(EXTRA_LOGO);
        String country = getIntent().getStringExtra(EXTRA_COUNTRY);
        String lang    = getIntent().getStringExtra(EXTRA_LANG);
        int bitrate    = getIntent().getIntExtra(EXTRA_BITRATE, 0);
        String codec   = getIntent().getStringExtra(EXTRA_CODEC);
        String tags    = getIntent().getStringExtra(EXTRA_TAGS);

        // Views
        PlayerView playerView = findViewById(R.id.playerView);
        ImageButton btnBack   = findViewById(R.id.btnBack);
        MediaRouteButton btnCast = findViewById(R.id.btnCast);
        btnTimer              = findViewById(R.id.btnTimer);
        ImageView ivLogo      = findViewById(R.id.ivLogo);
        TextView tvTitle      = findViewById(R.id.tvTitle);
        TextView tvMeta       = findViewById(R.id.tvMeta);

        // Logo y textos
        Glide.with(this).load(currentLogo).placeholder(R.drawable.ic_radio_placeholder).into(ivLogo);
        tvTitle.setText(currentName != null ? currentName : "Radio");
        tvMeta.setText(buildMeta(country, lang, bitrate, codec, tags));

        // ExoPlayer local con búfer optimizado para streaming
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        60000, // min 60s
                        120000, // max 120s
                        10000, // buffer para empezar a reproducir: 10s
                        15000 // buffer para reanudar tras interrupción: 15s
                ).build();

        player = new ExoPlayer.Builder(this).setLoadControl(loadControl).build();
        playerView.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(Uri.parse(currentUrl)));
        player.prepare();
        player.setPlayWhenReady(true);

        // Marca primer play para Ads
        player.addListener(new Player.Listener() {
            private boolean marked = false;
            @Override public void onPlaybackStateChanged(int state) {
                if (!marked && state == Player.STATE_READY && player.getPlayWhenReady()) {
                    AdsManager.get().markFirstPlayDone();
                    marked = true;
                }
            }
        });

        // Cast: botón y contexto
        CastManager.get(this).bindButton(btnCast);
        CastManager.get(this).setCurrentMedia(currentUrl, currentName, currentLogo);
        castContext = CastContext.getSharedInstance(this);

        btnBack.setOnClickListener(v -> finish());
        btnTimer.setOnClickListener(v -> showTimerDialog());
        updateTimerButtonState();
    }

    private void showTimerDialog() {
        final CharSequence[] items;
        final SleepTimerManager timerManager = SleepTimerManager.getInstance();

        if (timerManager.isTimerRunning()) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(timerManager.getMillisRemaining());
            items = new CharSequence[]{"Cancelar (" + minutes + " min)"};
        } else {
            items = new CharSequence[]{"15", "30", "60", "90"};
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sleep Timer (min)");
        builder.setItems(items, (dialog, item) -> {
            if (timerManager.isTimerRunning()) {
                timerManager.cancelTimer();
                Toast.makeText(this, "Temporizador cancelado", Toast.LENGTH_SHORT).show();
            } else {
                long minutes = Long.parseLong(items[item].toString());
                startSleepTimer(minutes);
                Toast.makeText(this, "La radio se detendrá en " + minutes + " minutos", Toast.LENGTH_SHORT).show();
            }
            updateTimerButtonState();
        });
        builder.show();
    }

    private void startSleepTimer(long minutes) {
        SleepTimerManager.getInstance().startTimer(TimeUnit.MINUTES.toMillis(minutes), () -> {
            if (player != null) {
                player.stop();
            }
        });
    }

    private void updateTimerButtonState() {
        if (btnTimer == null) return;
        int colorId = SleepTimerManager.getInstance().isTimerRunning() ? R.color.accent : R.color.on_dark;
        ImageViewCompat.setImageTintList(btnTimer, ContextCompat.getColorStateList(this, colorId));
    }


    private String buildMeta(String country, String lang, int bitrate, String codec, String tags) {
        StringBuilder sb = new StringBuilder();
        if (country != null && !country.isEmpty()) sb.append(country).append(" • ");
        if (lang != null && !lang.isEmpty()) sb.append(lang).append(" • ");
        if (bitrate > 0) sb.append(bitrate).append("kbps").append(" • ");
        if (codec != null && !codec.isEmpty()) sb.append(codec).append(" • ");
        if (tags != null && !tags.isEmpty()) sb.append(tags);
        String meta = sb.toString();
        if (meta.endsWith(" • ")) meta = meta.substring(0, meta.length() - 3);
        return meta;
    }

    private void pauseLocalAndSendToCast() {
        if (player != null) player.pause();
        // Reenvía la media por si la sesión comenzó tras abrir el player
        CastManager.get(this).setCurrentMedia(currentUrl, currentName, currentLogo);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (castContext != null) {
            castContext.getSessionManager().addSessionManagerListener(castListener, CastSession.class);
        }
        updateTimerButtonState();
    }

    @Override
    protected void onStop() {
        if (castContext != null) {
            castContext.getSessionManager().removeSessionManagerListener(castListener, CastSession.class);
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (player != null) { player.release(); player = null; }
        super.onDestroy();
    }
}

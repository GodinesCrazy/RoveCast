package com.ivanmarty.radiola.cast;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.mediarouter.app.MediaRouteButton;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaLoadRequestData;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;

public class CastManager {

    private static CastManager INSTANCE;

    public static CastManager get(@NonNull Context ctx) {
        if (INSTANCE == null) {
            INSTANCE = new CastManager(ctx.getApplicationContext());
        }
        return INSTANCE;
    }

    private final Context app;
    private final CastContext castContext;
    private String currentUrl, currentTitle, currentImage;

    // ✅ Implementa todos los métodos requeridos, incluido onSessionEnding
    private final SessionManagerListener<CastSession> sessionListener = new SessionManagerListener<CastSession>() {
        @Override public void onSessionStarting(CastSession session) {}
        @Override public void onSessionStarted(CastSession session, String s) { loadIfPossible(session); }
        @Override public void onSessionStartFailed(CastSession session, int i) {}

        @Override public void onSessionEnding(CastSession session) { /* no-op */ }
        @Override public void onSessionEnded(CastSession session, int i) {}

        @Override public void onSessionResuming(CastSession session, String s) {}
        @Override public void onSessionResumed(CastSession session, boolean b) { loadIfPossible(session); }
        @Override public void onSessionResumeFailed(CastSession session, int i) {}
        @Override public void onSessionSuspended(CastSession session, int i) {}
    };

    private CastManager(Context app) {
        this.app = app;
        this.castContext = CastContext.getSharedInstance(app);
        castContext.getSessionManager().addSessionManagerListener(sessionListener, CastSession.class);
    }

    public void bindButton(MediaRouteButton btn) {
        CastButtonFactory.setUpMediaRouteButton(app, btn);
    }

    /** Llamar desde PlayerActivity con la info actual para auto-enviar al conectar. */
    public void setCurrentMedia(String url, String title, String image) {
        this.currentUrl = url;
        this.currentTitle = title;
        this.currentImage = image;
        loadIfPossible(castContext.getSessionManager().getCurrentCastSession());
    }

    private void loadIfPossible(CastSession session) {
        if (session == null || !session.isConnected() || currentUrl == null) return;

        RemoteMediaClient client = session.getRemoteMediaClient();
        if (client == null) return;

        MediaMetadata md = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
        if (currentTitle != null) md.putString(MediaMetadata.KEY_TITLE, currentTitle);
        if (currentImage != null && !currentImage.isEmpty()) md.addImage(new WebImage(Uri.parse(currentImage)));

        MediaInfo mediaInfo = new MediaInfo.Builder(currentUrl)
                .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
                .setContentType("audio/mpeg")
                .setMetadata(md)
                .build();

        client.load(new MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setAutoplay(true)
                .build());
    }
}

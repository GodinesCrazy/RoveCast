package com.ivanmarty.rovecast.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
        miniPlayerContainer = findViewById(R.id.miniPlayer);
        miniPlayerLogo = miniPlayerContainer.findViewById(R.id.miniPlayerLogo);
        miniPlayerTitle = miniPlayerContainer.findViewById(R.id.miniPlayerTitle);
        miniPlayerStatus = miniPlayerContainer.findViewById(R.id.miniPlayerStatus);
        miniPlayerPlayPause = miniPlayerContainer.findViewById(R.id.miniPlayerPlayPause);

        miniPlayerPlayPause.setOnClickListener(v -> {
            try {
                MediaController mediaController = mediaControllerFuture.get();
                if (mediaController != null) {
                    if (mediaController.isPlaying()) {
                        mediaController.pause();
                    } else {
                        mediaController.play();
                    }
                }
            } catch (Exception e) { /* Ignored */ }
        });

        miniPlayerContainer.setOnClickListener(v -> {
            Intent intent = new Intent(this, PlayerActivity.class);
            startActivity(intent);
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
        } else {
            miniPlayerStatus.setText(R.string.status_paused);
            miniPlayerPlayPause.setImageResource(R.drawable.ic_play_arrow);
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

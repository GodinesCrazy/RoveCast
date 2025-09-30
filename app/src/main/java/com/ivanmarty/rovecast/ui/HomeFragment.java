package com.ivanmarty.rovecast.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.ivanmarty.rovecast.R;
import com.ivanmarty.rovecast.ads.AdsManager;
import com.ivanmarty.rovecast.billing.PremiumManager;
import com.ivanmarty.rovecast.data.FavoriteStation;
import com.ivanmarty.rovecast.model.Station;
import com.ivanmarty.rovecast.player.PlaybackService;
import com.ivanmarty.rovecast.ui.adapter.StationAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import android.os.Handler;
import android.os.Looper;

public class HomeFragment extends Fragment implements StationAdapter.StationListener {

    // Variables de clase correctamente declaradas
    private ShimmerFrameLayout shimmer;
    private HomeViewModel vm;
    private StationAdapter adapter;
    private boolean premium = false;
    private ProgressBar progressBar;
    private List<Station> currentStations = new ArrayList<>();
    private Set<String> favoriteStationIds = new HashSet<>();
    private long loadStartTime = 0;
    private static final long MIN_LOADING_TIME_MS = 3000; // 3 seconds minimum loading time

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup container, @Nullable Bundle saved) {
        View v = inf.inflate(R.layout.fragment_home, container, false);

        // Configurar RecyclerView
        RecyclerView rv = v.findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        adapter = new StationAdapter(this);
        rv.setAdapter(adapter);

        premium = PremiumManager.isPremium(requireContext());

        // Inicializar vistas
        progressBar = v.findViewById(R.id.progressBar);
        final View emptyStateContainer = v.findViewById(R.id.emptyStateContainer);
        shimmer = v.findViewById(R.id.shimmer);  // Variable de clase, no local

        // COMENTADO: AdView eliminado del layout durante correcciones de UI
        // adView = v.findViewById(R.id.adView);
        // if (premium) {
        //     adView.setVisibility(View.GONE);
        // } else {
        //     AdRequest adRequest = new AdRequest.Builder().build();
        //     adView.loadAd(adRequest);
        // }

        vm = new ViewModelProvider(this).get(HomeViewModel.class);

        // Banner primera carga
        final View firstLaunchBanner = v.findViewById(R.id.firstLaunchBanner);
        com.ivanmarty.rovecast.util.FirstLaunchManager flm = new com.ivanmarty.rovecast.util.FirstLaunchManager(requireContext());
        final boolean showFirstBanner = flm.isFirstLaunch();
        if (firstLaunchBanner != null) firstLaunchBanner.setVisibility(showFirstBanner ? View.VISIBLE : View.GONE);

        // Shimmer de carga inicial (oculta ProgressBar clásico)
        if (shimmer != null) {
            shimmer.setVisibility(View.VISIBLE);
            shimmer.startShimmer();
            loadStartTime = System.currentTimeMillis();
        }
        progressBar.setVisibility(View.GONE);
        vm.refreshStations();

        // Observer para estaciones
        vm.getStations().observe(getViewLifecycleOwner(), stations -> {
            long currentTime = System.currentTimeMillis();
            long timeElapsed = currentTime - loadStartTime;
            long remainingTime = MIN_LOADING_TIME_MS - timeElapsed;
            
            Runnable hideShimmer = () -> {
                if (shimmer != null && shimmer.getVisibility() == View.VISIBLE) {
                    shimmer.stopShimmer();
                    shimmer.setVisibility(View.GONE);
                }
                // Update the UI with the loaded stations
                currentStations = stations != null ? stations : new ArrayList<>();
                updateAdapterList();
                if (emptyStateContainer != null) {
                    emptyStateContainer.setVisibility(currentStations.isEmpty() ? View.VISIBLE : View.GONE);
                }
                if (firstLaunchBanner != null && !currentStations.isEmpty() && showFirstBanner) {
                    firstLaunchBanner.setVisibility(View.GONE);
                    flm.setFirstLaunch(false);
                }
            };
            
            if (remainingTime > 0) {
                // Aún no ha pasado el tiempo mínimo de carga
                new Handler(Looper.getMainLooper()).postDelayed(hideShimmer, remainingTime);
            } else {
                // Ya pasó el tiempo mínimo, ocultar inmediatamente
                hideShimmer.run();
            }
            progressBar.setVisibility(View.GONE);
        });

        // Observer para favoritos
        vm.getFavRepo().getFavorites().observe(getViewLifecycleOwner(), favs -> {
            favoriteStationIds.clear();
            if (favs != null) {
                for (FavoriteStation f : favs) {
                    if (f.stationuuid != null) favoriteStationIds.add(f.stationuuid);
                }
            }
            updateAdapterList();
        });

        // Manejo del botón back
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requireActivity().finish();
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // Ajustar padding dinámicamente según visibilidad del mini-player
        // Esto garantiza que se muestren 6+ radios consistentemente
        adjustRecyclerViewPadding();
    }

    @Override
    public void onStationSelected(Station s) {
        Intent serviceIntent = new Intent(requireContext(), PlaybackService.class);
        serviceIntent.setAction(PlaybackService.ACTION_PLAY);
        serviceIntent.putExtra(PlaybackService.EXTRA_URL, s.getUrl());
        serviceIntent.putExtra(PlaybackService.EXTRA_NAME, s.name);
        serviceIntent.putExtra(PlaybackService.EXTRA_LOGO, s.favicon);
        serviceIntent.putExtra(PlaybackService.EXTRA_STATION_UUID, s.stationuuid);
        serviceIntent.putExtra(PlaybackService.EXTRA_METADATA, buildMeta(s));
        ContextCompat.startForegroundService(requireContext(), serviceIntent);

        Intent playerIntent = new Intent(requireContext(), PlayerActivity.class);
        startActivity(playerIntent);
    }

    @Override
    public void onFavoriteToggle(Station s, boolean makeFav) {
        vm.getFavRepo().toggleFavorite(s, makeFav);
    }

    private String buildMeta(Station s) {
        StringBuilder sb = new StringBuilder();
        if (s.country != null && !s.country.isEmpty()) sb.append(s.country).append(" · ");
        if (s.language != null && !s.language.isEmpty()) sb.append(s.language).append(" · ");
        if (s.bitrate > 0) sb.append(s.bitrate).append("kbps").append(" · ");
        if (s.codec != null && !s.codec.isEmpty()) sb.append(s.codec).append(" · ");
        if (s.tags != null && !s.tags.isEmpty()) sb.append(s.tags);
        String meta = sb.toString();
        if (meta.endsWith(" · ")) meta = meta.substring(0, meta.length() - 3);
        return meta;
    }

    private void updateAdapterList() {
        if (currentStations == null) {
            adapter.submitList(java.util.Collections.emptyList());
            return;
        }
        List<Station> processedList = new java.util.ArrayList<>();
        for (Station originalStation : currentStations) {
            Station station = new Station();
            station.stationuuid = originalStation.stationuuid;
            station.name = originalStation.name;
            station.url_resolved = originalStation.url_resolved;
            station.favicon = originalStation.favicon;
            station.country = originalStation.country;
            station.language = originalStation.language;
            station.bitrate = originalStation.bitrate;
            station.codec = originalStation.codec;
            station.tags = originalStation.tags;
            station.isFavorite = favoriteStationIds.contains(originalStation.stationuuid);
            processedList.add(station);
        }
        adapter.submitList(processedList);
    }

    /**
     * Ajusta dinámicamente el padding del RecyclerView según la visibilidad del mini-player
     * para garantizar que se muestren consistentemente 6+ radios en ambos estados
     */
    public void adjustRecyclerViewPadding() {
        if (getActivity() == null) return;
        
        View recyclerView = getActivity().findViewById(R.id.recycler);
        View miniPlayer = getActivity().findViewById(R.id.miniPlayerContainer);
        
        if (recyclerView != null && miniPlayer != null) {
            // Convertir dp a pixels
            float density = getResources().getDisplayMetrics().density;
            
            // Padding cuando mini-player está visible (64dp altura + margen)
            int paddingWithMiniPlayer = (int) (80 * density);
            // Padding cuando mini-player NO está visible (solo margen inferior)
            int paddingWithoutMiniPlayer = (int) (16 * density);
            
            int newPadding = (miniPlayer.getVisibility() == View.VISIBLE) 
                ? paddingWithMiniPlayer 
                : paddingWithoutMiniPlayer;
            
            recyclerView.setPadding(
                recyclerView.getPaddingLeft(),
                recyclerView.getPaddingTop(),
                recyclerView.getPaddingRight(),
                newPadding
            );
        }
    }
}

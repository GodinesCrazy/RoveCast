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
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
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

public class HomeFragment extends Fragment implements StationAdapter.StationListener {

    private HomeViewModel vm;
    private StationAdapter adapter;
    private boolean premium = false;
    private AdView adView;
    private ProgressBar progressBar;
    private List<Station> currentStations = new ArrayList<>();
    private Set<String> favoriteStationIds = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup container, @Nullable Bundle saved) {
        View v = inf.inflate(R.layout.fragment_home, container, false);
        RecyclerView rv = v.findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        adapter = new StationAdapter(this);
        rv.setAdapter(adapter);

        premium = PremiumManager.isPremium(requireContext());

        progressBar = v.findViewById(R.id.progressBar);
        final View tvEmpty = v.findViewById(R.id.tvEmpty);
        final ShimmerFrameLayout shimmer = v.findViewById(R.id.shimmer);
        adView = v.findViewById(R.id.adView);
        if (premium) {
            adView.setVisibility(View.GONE);
        } else {
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }

        vm = new ViewModelProvider(this).get(HomeViewModel.class);

        // Banner primera carga
        final View firstLaunchBanner = v.findViewById(R.id.firstLaunchBanner);
        com.ivanmarty.rovecast.util.FirstLaunchManager flm = new com.ivanmarty.rovecast.util.FirstLaunchManager(requireContext());
        final boolean showFirstBanner = flm.isFirstLaunch();
        if (firstLaunchBanner != null) firstLaunchBanner.setVisibility(showFirstBanner ? View.VISIBLE : View.GONE);

        // Shimmer de carga inicial (oculta ProgressBar clásico)
        if (shimmer != null) { shimmer.setVisibility(View.VISIBLE); shimmer.startShimmer(); }
        progressBar.setVisibility(View.GONE);
        vm.refreshStations();

        vm.getStations().observe(getViewLifecycleOwner(), stations -> {
            if (shimmer != null) { shimmer.stopShimmer(); shimmer.setVisibility(View.GONE); }
            progressBar.setVisibility(View.GONE);
            currentStations = stations != null ? stations : new ArrayList<>();
            updateAdapterList();
            if (tvEmpty != null) {
                tvEmpty.setVisibility(currentStations.isEmpty() ? View.VISIBLE : View.GONE);
            }
            if (firstLaunchBanner != null && !currentStations.isEmpty() && showFirstBanner) {
                firstLaunchBanner.setVisibility(View.GONE);
                flm.setFirstLaunch(false);
            }
        });

        vm.getFavRepo().getFavorites().observe(getViewLifecycleOwner(), favs -> {
            favoriteStationIds.clear();
            if (favs != null) {
                for (FavoriteStation f : favs) {
                    if (f.stationuuid != null) favoriteStationIds.add(f.stationuuid);
                }
            }
            updateAdapterList();
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { requireActivity().finish(); }
        });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        AdsManager.get().runWithMaybeAd(requireActivity(), null);
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

    @Override public void onFavoriteToggle(Station s, boolean makeFav) { vm.getFavRepo().toggleFavorite(s, makeFav); }

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
}

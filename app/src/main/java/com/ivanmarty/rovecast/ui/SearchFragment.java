package com.ivanmarty.rovecast.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.*;
import android.util.Log;
import android.view.*;
import android.widget.EditText;
import androidx.annotation.*;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ivanmarty.rovecast.R;
import com.ivanmarty.rovecast.data.FavoriteStation;
import com.ivanmarty.rovecast.model.Station;
import com.ivanmarty.rovecast.player.PlaybackService;
import com.ivanmarty.rovecast.ui.adapter.StationAdapter;
import java.util.*;

public class SearchFragment extends Fragment implements StationAdapter.StationListener {

    private SearchViewModel vm;
    private StationAdapter adapter;
    private List<Station> currentStations = new ArrayList<>();
    private Set<String> favoriteStationIds = new HashSet<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup container, @Nullable Bundle saved) {
        View v = inf.inflate(R.layout.fragment_search, container, false);

        RecyclerView rv = v.findViewById(R.id.recyclerSearch);
        rv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        adapter = new StationAdapter(this);
        rv.setAdapter(adapter);

        EditText et = v.findViewById(R.id.etSearch);
        final com.facebook.shimmer.ShimmerFrameLayout shimmer = v.findViewById(R.id.shimmerSearch);
        vm = new ViewModelProvider(this).get(SearchViewModel.class);

        vm.getRepo().getFavorites().observe(getViewLifecycleOwner(), favs -> {
            favoriteStationIds.clear();
            if (favs != null) {
                for (FavoriteStation f : favs) {
                    if (f.stationuuid != null) favoriteStationIds.add(f.stationuuid);
                }
            }
            updateAdapterList();
        });

        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                String q = s.toString().trim();
                if (q.length() < 2) {
                    currentStations.clear();
                    updateAdapterList();
                    return;
                }
                if (shimmer != null) { shimmer.setVisibility(View.VISIBLE); shimmer.startShimmer(); }
                vm.search(q).observe(getViewLifecycleOwner(), list -> {
                    currentStations = list != null ? list : new ArrayList<>();
                    updateAdapterList();
                    if (shimmer != null) { shimmer.stopShimmer(); shimmer.setVisibility(View.GONE); }
                });
            }
        });

        return v;
    }

    @Override public void onStationSelected(Station s) {
        Intent serviceIntent = new Intent(requireContext(), PlaybackService.class);
        serviceIntent.setAction(PlaybackService.ACTION_PLAY);
        serviceIntent.putExtra(PlaybackService.EXTRA_URL, s.getUrl());
        serviceIntent.putExtra(PlaybackService.EXTRA_NAME, s.name);
        serviceIntent.putExtra(PlaybackService.EXTRA_LOGO, s.favicon);
        serviceIntent.putExtra(PlaybackService.EXTRA_METADATA, buildMeta(s));
        ContextCompat.startForegroundService(requireContext(), serviceIntent);

        Intent playerIntent = new Intent(requireContext(), PlayerActivity.class);
        startActivity(playerIntent);
    }

    private String buildMeta(Station s) {
        StringBuilder sb = new StringBuilder();
        if (s.country != null && !s.country.isEmpty()) sb.append(s.country).append(" • ");
        if (s.language != null && !s.language.isEmpty()) sb.append(s.language).append(" • ");
        if (s.bitrate > 0) sb.append(s.bitrate).append("kbps").append(" • ");
        if (s.codec != null && !s.codec.isEmpty()) sb.append(s.codec).append(" • ");
        if (s.tags != null && !s.tags.isEmpty()) sb.append(s.tags);
        String meta = sb.toString();
        if (meta.endsWith(" • ")) meta = meta.substring(0, meta.length() - 3);
        return meta;
    }

    @Override public void onFavoriteToggle(Station s, boolean makeFav) { vm.getRepo().toggleFavorite(s, makeFav); }

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

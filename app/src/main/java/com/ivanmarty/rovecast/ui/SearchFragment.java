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
import com.ivanmarty.rovecast.ads.AdsManager;
import com.ivanmarty.rovecast.data.FavoriteStation;
import com.ivanmarty.rovecast.model.Station;
import com.ivanmarty.rovecast.player.PlaybackService;
import com.ivanmarty.rovecast.ui.adapter.StationAdapter;
import java.util.*;

public class SearchFragment extends Fragment implements StationAdapter.StationListener {

    private SearchViewModel vm;
    private StationAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup container, @Nullable Bundle saved) {
        View v = inf.inflate(R.layout.fragment_search, container, false);

        RecyclerView rv = v.findViewById(R.id.recyclerSearch);
        rv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        adapter = new StationAdapter(this);
        rv.setAdapter(adapter);

        EditText et = v.findViewById(R.id.etSearch);
        vm = new ViewModelProvider(this).get(SearchViewModel.class);

        // Observa favoritos para marcar corazones
        vm.getRepo().getFavorites().observe(getViewLifecycleOwner(), favs -> {
            Set<String> ids = new HashSet<>();
            if (favs != null) for (FavoriteStation f : favs) if (f.stationuuid != null) ids.add(f.stationuuid);
            adapter.updateFavorites(ids);
        });

        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                String q = s.toString().trim();
                Log.d("SearchFragment", "Query text changed to: " + q);
                if (q.length() < 2) { adapter.submitList(new ArrayList<>()); return; }
                vm.search(q).observe(getViewLifecycleOwner(), list -> {
                    if (list != null) {
                        Log.d("SearchFragment", "Received " + list.size() + " stations for query: " + q);
                        adapter.submitList(list);
                    }
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
}
